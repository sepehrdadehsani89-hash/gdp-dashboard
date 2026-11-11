package com.jarvis.assistant.finishpolish.predictive

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.finishpolish.learning.LearningStore
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import java.time.ZoneId
import java.time.Instant
import java.util.UUID

class PredictiveActivity:PrivateModuleActivity(){
    private val engine get()=runtime.predictive
    private val calendarPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->status=if(granted)"Calendar permission granted. Only upcoming opted-in events may be read for local suggestions." else "Calendar access denied. No calendar data will be read; manual routines remain available."}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("YOUR NEXT ACTION · PREDICTIVE INTELLIGENCE"){
            val profile by engine.store.state.collectAsStateWithLifecycle()
            val suggestion by engine.state.collectAsStateWithLifecycle()
            val engineStatus by engine.status.collectAsStateWithLifecycle()
            val c=profile.config
            Text("Optional, local suggestions based on your approved routines, real battery observations and separately opted-in calendar. This does not learn a personal schedule from conversations or execute anything automatically.")
            if(!engine.store.available)Text("Private predictive storage is unavailable. Existing data was preserved; suggestions stay off.")
            Text("Predictive suggestions")
            Switch(c.enabled,{enabled->authorize{if(enabled)engine.store.configure(engine.store.state.value.config.copy(enabled=true))else engine.disable()}})
            Text("Real low-battery suggestion at 10% or below")
            Switch(c.battery,{enabled->authorize{engine.store.configure(engine.store.state.value.config.copy(battery=enabled))}})
            Text("Read authorized Android calendar events beginning in the next 20 minutes. Titles stay local and are not spoken in a voice prompt. Consent and READ_CALENDAR are both required.")
            Switch(c.calendar,{enabled->authorize{
                engine.store.configure(engine.store.state.value.config.copy(calendar=enabled))
                if(enabled && ContextCompat.checkSelfPermission(this@PredictiveActivity,Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            }})
            Text("Delivery · silent strip / text / generic spoken invitation")
            SuggestionDelivery.entries.forEach{delivery->FilterChip(c.delivery==delivery,{authorize{engine.store.configure(engine.store.state.value.config.copy(delivery=delivery))}},label={Text(delivery.name.lowercase())})}
            Text("Quiet hours ${c.quietStart}:00–${c.quietEnd}:00 · at most ${c.maximumPerDay}/day · minimum ${c.gapMinutes} minutes apart")
            Switch(c.quiet,{enabled->authorize{engine.store.configure(engine.store.state.value.config.copy(quiet=enabled))}})
            var start by remember(c.quietStart){mutableStateOf(c.quietStart.toString())};var end by remember(c.quietEnd){mutableStateOf(c.quietEnd.toString())}
            var cap by remember(c.maximumPerDay){mutableStateOf(c.maximumPerDay.toString())};var gap by remember(c.gapMinutes){mutableStateOf(c.gapMinutes.toString())}
            OutlinedTextField(start,{start=it.take(2)},label={Text("Quiet start hour 0–23")});OutlinedTextField(end,{end=it.take(2)},label={Text("Quiet end hour 0–23")})
            OutlinedTextField(cap,{cap=it.take(1)},label={Text("Daily maximum 1–6")});OutlinedTextField(gap,{gap=it.take(3)},label={Text("Minimum gap 15–240 minutes")})
            OutlinedButton(onClick={authorize{val next=engine.store.state.value.config.copy(quietStart=start.toInt(),quietEnd=end.toInt(),maximumPerDay=cap.toInt(),gapMinutes=gap.toInt());PredictivePolicy.validate(next);engine.store.configure(next);status="Suggestion limits saved and read back from encrypted storage."}}){Text("Save quiet hours and limits")}
            Text(engineStatus)
            suggestion?.let{s->
                Text(s.title);Text(s.explanation);Text("Source checked ${Instant.ofEpochMilli(s.observedAt).atZone(ZoneId.systemDefault())}; expires ${Instant.ofEpochMilli(s.expiresAt).atZone(ZoneId.systemDefault())}.")
                OutlinedButton(onClick={engine.dismiss(s.id)}){Text("Not now")};OutlinedButton(onClick={engine.snooze(s.id)}){Text("Snooze 30 minutes")};OutlinedButton(onClick={authorize{engine.never(s.id)}}){Text("Never suggest this source")}
                Text("Return to Jarvis to review an available action in the suggestion strip. No action was run by this screen.")
            }
            HorizontalDivider();Text("Your explicit routines")
            var editing by remember{mutableStateOf<String?>(null)};var title by remember{mutableStateOf("")};var dateTime by remember{mutableStateOf("")}
            var zone by remember{mutableStateOf(ZoneId.systemDefault().id)};var daily by remember{mutableStateOf(false)};var lead by remember{mutableStateOf("20")}
            var trigger by remember{mutableStateOf("")};var weekdays by remember{mutableStateOf((1..7).toSet())}
            OutlinedTextField(title,{title=it.take(100)},label={Text("Routine title · kept locally")})
            FilterChip(daily,{daily=!daily},label={Text(if(daily)"Repeating on selected weekdays" else "One dated occurrence")})
            OutlinedTextField(dateTime,{dateTime=it.take(30)},label={Text(if(daily)"Local time HH:mm" else "Local date/time YYYY-MM-DDTHH:mm")})
            OutlinedTextField(zone,{zone=it.take(80)},label={Text("Time zone, e.g. America/Guayaquil")})
            if(daily)(1..7).forEach{day->FilterChip(day in weekdays,{weekdays=if(day in weekdays)weekdays-day else weekdays+day},label={Text(java.time.DayOfWeek.of(day).name.lowercase())})}
            OutlinedTextField(lead,{lead=it.take(3)},label={Text("Offer preparation 0–120 minutes before")})
            OutlinedTextField(trigger,{trigger=it.take(160)},label={Text("Optional approved learned recipe phrase · blank is a reminder only")})
            Text("A linked phrase must currently resolve to an enabled approved recipe. Every execution still opens the shared action review. A deleted/reused phrase cannot silently target a different recipe.")
            Button(enabled=engine.store.available && title.isNotBlank() && dateTime.isNotBlank(),onClick={authorize{
                val selected=trigger.trim().takeIf{it.isNotEmpty()}?.let{LearningStore(vault).match(it) ?: error("Enable learning and approve this recipe first.")}
                val at=if(daily)null else PredictivePolicy.exactTime(dateTime.trim(),zone.trim()).also{require(it>System.currentTimeMillis()){"Choose a future time."}}
                val r=PredictiveRoutine(editing ?: UUID.randomUUID().toString(),title.trim(),zone.trim(),at,if(daily)dateTime.trim()else null,weekdays,lead.toInt(),trigger.trim(),selected?.id.orEmpty())
                engine.store.saveRoutine(r);editing=null;title="";dateTime="";trigger="";status="Routine saved and read back from encrypted local storage. No action was executed."
            }}){Text(if(editing==null)"Review and save routine" else "Save edited routine")}
            profile.routines.forEach{r->
                Text("${r.title} · ${r.dailyTime ?: r.onceAt?.let{Instant.ofEpochMilli(it).atZone(ZoneId.of(r.zone))}} · ${r.zone}")
                OutlinedButton(onClick={editing=r.id;title=r.title;zone=r.zone;daily=r.dailyTime!=null;dateTime=r.dailyTime ?: Instant.ofEpochMilli(requireNotNull(r.onceAt)).atZone(ZoneId.of(r.zone)).toLocalDateTime().toString();lead=r.leadMinutes.toString();trigger=r.recipeTrigger;weekdays=r.weekdays}){Text("Edit date/time and preparation")}
                OutlinedButton(onClick={authorize{engine.store.deleteRoutine(r.id)}}){Text("Delete routine")}
            }
            OutlinedButton(onClick={authorize{engine.store.clearSuppression();status="Never/snooze suppression cleared. Daily cap and already-shown occurrences remain."}}){Text("Restore suppressed sources")}
            OutlinedButton(onClick={authorize{engine.reset();status="Only new predictive settings, routines and aggregate history were deleted. Suggestions are off."}}){Text("Delete predictive data and opt out")}
            Text("Aggregate counts: ${profile.history.accepted} accepted for review, ${profile.history.dismissed} dismissed. These are not execution-success or learning-accuracy scores.")
            Text("Suggestions refresh only while this app process and an eligible visible/idle session are available. No exact-time background notification or inferred habit is promised. Clock-change gaps/overlaps are rejected for dated entries and skipped for repeating entries; no duplicate guess is made. Silent/DND, music and battery saver suppress delivery.")
        }
    }
}
