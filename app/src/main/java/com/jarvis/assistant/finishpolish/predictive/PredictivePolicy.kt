package com.jarvis.assistant.finishpolish.predictive

import java.time.*
import java.util.UUID

enum class SuggestionSource { BATTERY, ROUTINE, CALENDAR }
enum class SuggestionDelivery { SILENT, TEXT, VOICE }
enum class SuggestionActionKind { BATTERY_SETTINGS, REVIEW_RECIPE, REVIEW_CALENDAR }
data class SuggestionAction(val kind:SuggestionActionKind,val target:String="",val referenceId:String="")
data class FutureSuggestion(
    val id:String,val source:SuggestionSource,val sourceKey:String,val title:String,val explanation:String,
    val observedAt:Long,val dueAt:Long?,val expiresAt:Long,val action:SuggestionAction?,
    val confidence:String="Observed device state or explicitly configured schedule; no inferred probability",
    val announcement:String="A suggestion is available, sir. Would you like to review it?",
)
data class PredictiveRoutine(
    val id:String=UUID.randomUUID().toString(),val title:String,val zone:String,
    val onceAt:Long?=null,val dailyTime:String?=null,val weekdays:Set<Int> = (1..7).toSet(),
    val leadMinutes:Int=20,val recipeTrigger:String="",val recipeId:String="",val enabled:Boolean=true,
)
data class PredictiveConfig(
    val enabled:Boolean=false,val battery:Boolean=false,val calendar:Boolean=false,
    val quiet:Boolean=true,val quietStart:Int=22,val quietEnd:Int=7,
    val maximumPerDay:Int=3,val gapMinutes:Int=30,val delivery:SuggestionDelivery=SuggestionDelivery.SILENT,
)
data class SuggestionEnvironment(
    val now:Long,val zone:String,val unlocked:Boolean,val idle:Boolean,val dnd:Boolean=false,
    val silent:Boolean=false,val music:Boolean=false,val powerSave:Boolean=false,
)
data class SuggestionHistory(
    val seen:Map<String,Long> = emptyMap(),val snoozed:Map<String,Long> = emptyMap(),
    val never:Set<String> = emptySet(),val lastShown:Long=0,val shownDay:String="",val shownToday:Int=0,
    val accepted:Int=0,val dismissed:Int=0,
)
object PredictivePolicy {
    const val FRESH_BATTERY_MS=150_000L
    fun validate(c:PredictiveConfig){require(c.quietStart in 0..23 && c.quietEnd in 0..23 && c.maximumPerDay in 1..6 && c.gapMinutes in 15..240)}
    fun validate(r:PredictiveRoutine){
        require(r.id.length in 1..100 && r.title.isNotBlank() && r.title.length<=100 && r.title.none(Char::isISOControl))
        ZoneId.of(r.zone);require((r.onceAt!=null) xor (r.dailyTime!=null));r.onceAt?.let{require(it>0)}
        r.dailyTime?.let{LocalTime.parse(it)};require(r.weekdays.isNotEmpty() && r.weekdays.all{it in 1..7} && r.leadMinutes in 0..120)
        require(r.recipeTrigger.length<=160 && r.recipeTrigger.none(Char::isISOControl) && r.recipeId.length<=100)
    }
    fun exactTime(local:String,zone:String):Long{
        val dt=LocalDateTime.parse(local);val z=ZoneId.of(zone);val offsets=z.rules.getValidOffsets(dt)
        require(offsets.size==1){"Choose an unambiguous valid local time; this time crosses a clock change."}
        return dt.toInstant(offsets.single()).toEpochMilli()
    }
    fun occurrence(r:PredictiveRoutine,now:Long):Long? {
        validate(r);if(!r.enabled)return null
        r.onceAt?.let{return it.takeIf{at->now<=at+300_000}}
        val zone=ZoneId.of(r.zone);val today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();val time=LocalTime.parse(r.dailyTime)
        for(delta in 0L..7L){
            val date=today.plusDays(delta);if(date.dayOfWeek.value !in r.weekdays)continue
            val local=LocalDateTime.of(date,time);val offsets=zone.rules.getValidOffsets(local)
            if(offsets.size!=1)continue // Skip nonexistent/ambiguous clock-change occurrences; never guess twice.
            val at=local.toInstant(offsets.single()).toEpochMilli();if(now<=at+300_000)return at
        }
        return null
    }
    fun quiet(c:PredictiveConfig,e:SuggestionEnvironment):Boolean {
        if(!c.quiet || c.quietStart==c.quietEnd)return false
        val hour=Instant.ofEpochMilli(e.now).atZone(ZoneId.of(e.zone)).hour
        return if(c.quietStart<c.quietEnd)hour in c.quietStart until c.quietEnd else hour>=c.quietStart || hour<c.quietEnd
    }
    fun suppression(c:PredictiveConfig,e:SuggestionEnvironment,h:SuggestionHistory):String? {
        validate(c)
        val day=Instant.ofEpochMilli(e.now).atZone(ZoneId.of(e.zone)).toLocalDate().toString()
        return when {
            !c.enabled->"Predictive suggestions are off"
            !e.unlocked->"Private suggestions hidden while locked"
            !e.idle->"Waiting for the current interaction to finish"
            e.dnd || e.silent || e.music->"Quiet audio policy is active"
            e.powerSave->"Suppressed in battery saver"
            quiet(c,e)->"Quiet hours"
            h.lastShown>e.now->"Clock changed; waiting for the prior suggestion window"
            h.shownDay==day && h.shownToday>=c.maximumPerDay->"Daily suggestion limit reached"
            h.lastShown>0 && e.now-h.lastShown<c.gapMinutes*60_000L->"Suggestion frequency limit"
            else->null
        }
    }
    fun battery(percent:Int?,charging:Boolean?,observedAt:Long,now:Long):FutureSuggestion? {
        if(percent !in 0..10 || charging!=false || now-observedAt !in 0..FRESH_BATTERY_MS)return null
        return FutureSuggestion("battery-low",SuggestionSource.BATTERY,"battery-low","Battery is at $percent%","Source: current Android battery observation; not a predicted reading.",observedAt,null,now+600_000,SuggestionAction(SuggestionActionKind.BATTERY_SETTINGS),announcement="Sir, your battery is low. Would you like to review battery saver settings?")
    }
    fun routine(r:PredictiveRoutine,now:Long):FutureSuggestion? {
        val at=occurrence(r,now) ?: return null
        if(now<at-r.leadMinutes*60_000L || now>at+300_000)return null
        return FutureSuggestion("routine:${r.id}:$at",SuggestionSource.ROUTINE,"routine:${r.id}",r.title,"Source: your approved routine, ${r.zone}; scheduled ${Instant.ofEpochMilli(at).atZone(ZoneId.of(r.zone))}.",now,at,at+300_000,
            r.recipeTrigger.takeIf{it.isNotBlank()}?.let{SuggestionAction(SuggestionActionKind.REVIEW_RECIPE,it,r.recipeId)},announcement="Sir, your configured routine is coming up. Would you like to review it?")
    }
    fun eligible(s:FutureSuggestion,c:PredictiveConfig,e:SuggestionEnvironment,h:SuggestionHistory):Boolean {
        if(!c.enabled || s.expiresAt<=e.now || s.observedAt>e.now || s.sourceKey in h.never || (h.snoozed[s.sourceKey] ?: 0)>e.now)return false
        if(s.source==SuggestionSource.BATTERY && !c.battery || s.source==SuggestionSource.CALENDAR && !c.calendar)return false
        return s.id !in h.seen
    }
    fun choose(candidates:List<FutureSuggestion>,c:PredictiveConfig,e:SuggestionEnvironment,h:SuggestionHistory):FutureSuggestion? {
        if(suppression(c,e,h)!=null)return null
        return candidates.filter{eligible(it,c,e,h)}.sortedWith(compareBy<FutureSuggestion>{it.dueAt ?: Long.MAX_VALUE}.thenBy{it.id}).firstOrNull()
    }
    fun shown(s:FutureSuggestion,c:PredictiveConfig,e:SuggestionEnvironment,h:SuggestionHistory):SuggestionHistory {
        val day=Instant.ofEpochMilli(e.now).atZone(ZoneId.of(e.zone)).toLocalDate().toString()
        return h.copy(seen=(h.seen+(s.id to e.now)).entries.sortedByDescending{it.value}.take(100).associate{it.toPair()},lastShown=e.now,shownDay=day,shownToday=if(h.shownDay==day)h.shownToday+1 else 1)
    }
}
