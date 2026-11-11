package com.jarvis.assistant.finishpolish.predictive

import com.jarvis.assistant.omnisystem.FeatureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class PredictiveProfile(val config:PredictiveConfig=PredictiveConfig(),val routines:List<PredictiveRoutine> = emptyList(),val history:SuggestionHistory=SuggestionHistory())
object PredictiveCodec {
    fun encode(p:PredictiveProfile):String {
        PredictivePolicy.validate(p.config);require(p.routines.size<=30);p.routines.forEach(PredictivePolicy::validate)
        val c=p.config;val h=p.history
        return JSONObject().put("version",1).put("config",JSONObject().put("enabled",c.enabled).put("battery",c.battery).put("calendar",c.calendar).put("quiet",c.quiet).put("start",c.quietStart).put("end",c.quietEnd).put("daily_cap",c.maximumPerDay).put("gap",c.gapMinutes).put("delivery",c.delivery.name))
            .put("routines",JSONArray().apply{p.routines.forEach{r->put(JSONObject().put("id",r.id).put("title",r.title).put("zone",r.zone).put("once",r.onceAt ?: JSONObject.NULL).put("daily",r.dailyTime ?: JSONObject.NULL).put("days",JSONArray(r.weekdays.sorted())).put("lead",r.leadMinutes).put("trigger",r.recipeTrigger).put("recipe_id",r.recipeId).put("enabled",r.enabled))}})
            .put("history",JSONObject().put("seen",JSONObject(h.seen)).put("snoozed",JSONObject(h.snoozed)).put("never",JSONArray(h.never.sorted())).put("last",h.lastShown).put("day",h.shownDay).put("today",h.shownToday).put("accepted",h.accepted).put("dismissed",h.dismissed)).toString()
    }
    fun decode(raw:String):PredictiveProfile {
        require(raw.toByteArray().size<=200_000);val j=JSONObject(raw);require(j.getInt("version")==1)
        val c=j.getJSONObject("config");val config=PredictiveConfig(c.getBoolean("enabled"),c.getBoolean("battery"),c.getBoolean("calendar"),c.getBoolean("quiet"),c.getInt("start"),c.getInt("end"),c.getInt("daily_cap"),c.getInt("gap"),SuggestionDelivery.valueOf(c.getString("delivery")));PredictivePolicy.validate(config)
        val a=j.getJSONArray("routines");require(a.length()<=30)
        val routines=(0 until a.length()).map{i->val r=a.getJSONObject(i);val days=r.getJSONArray("days");PredictiveRoutine(r.getString("id"),r.getString("title"),r.getString("zone"),if(r.isNull("once"))null else r.getLong("once"),if(r.isNull("daily"))null else r.getString("daily"),(0 until days.length()).map{days.getInt(it)}.toSet(),r.getInt("lead"),r.getString("trigger"),r.optString("recipe_id"),r.getBoolean("enabled")).also(PredictivePolicy::validate)}
        require(routines.map{it.id}.distinct().size==routines.size)
        val h=j.getJSONObject("history")
        fun map(key:String):Map<String,Long>{val v=h.getJSONObject(key);require(v.length()<=100);return v.keys().asSequence().associateWith{require(it.length<=160);v.getLong(it).also{at->require(at>=0)}}}
        val never=h.getJSONArray("never");require(never.length()<=100)
        val history=SuggestionHistory(map("seen"),map("snoozed"),(0 until never.length()).map{never.getString(it).also{s->require(s.length<=160)}}.toSet(),h.getLong("last"),h.getString("day"),h.getInt("today"),h.getInt("accepted"),h.getInt("dismissed"))
        require(history.lastShown>=0 && history.shownToday>=0 && history.accepted>=0 && history.dismissed>=0)
        return PredictiveProfile(config,routines,history)
    }
}
class PredictiveStore(private val vault:FeatureVault){
    companion object{const val KEY="finish_predictive_v_one"}
    private var writable=true
    private val mutable=MutableStateFlow(runCatching{val raw=vault.read(KEY);if(raw==null){check(!vault.contains(KEY));PredictiveProfile()}else PredictiveCodec.decode(raw)}.getOrElse{writable=false;PredictiveProfile()})
    val state=mutable.asStateFlow()
    val available get()=writable
    @Synchronized private fun save(p:PredictiveProfile){check(writable){"Predictive storage is unavailable; existing data was preserved."};val raw=PredictiveCodec.encode(p);vault.write(KEY,raw);check(vault.read(KEY)==raw){"Private suggestion settings could not be verified."};mutable.value=p}
    @Synchronized fun configure(c:PredictiveConfig){save(mutable.value.copy(config=c))}
    @Synchronized fun saveRoutine(r:PredictiveRoutine){PredictivePolicy.validate(r);val p=mutable.value;require(p.routines.size<30 || p.routines.any{it.id==r.id});save(p.copy(routines=p.routines.filterNot{it.id==r.id}+r))}
    @Synchronized fun deleteRoutine(id:String){val p=mutable.value;save(p.copy(routines=p.routines.filterNot{it.id==id}))}
    @Synchronized fun history(h:SuggestionHistory){save(mutable.value.copy(history=h))}
    @Synchronized fun resetAfterUserErase(){check(!vault.contains(KEY));writable=true;mutable.value=PredictiveProfile()}
    @Synchronized fun clearSuppression(){val p=mutable.value;save(p.copy(history=p.history.copy(never=emptySet(),snoozed=emptyMap())))}
    @Synchronized fun clear(){check(writable);vault.remove(KEY);mutable.value=PredictiveProfile()}
}
