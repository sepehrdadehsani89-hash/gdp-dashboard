package com.jarvis.assistant.checkin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import java.time.*
import java.time.format.DateTimeFormatter

class CallMeActivity:PrivateModuleActivity(){
    private var date by mutableStateOf(LocalDate.now())
    private var time by mutableStateOf(LocalTime.now().plusMinutes(10).withSecond(0).withNano(0))
    private var title by mutableStateOf("Jarvis check-in")
    private var exact by mutableStateOf(false)
    private var spoken by mutableStateOf(true)
    private var editing by mutableStateOf<String?>(null)
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        exact=runtime.callMe.exactAllowed()
        val request=intent.getStringExtra("request").orEmpty()
        val suggested=CallMePolicy.suggested(request,ZonedDateTime.now())
        suggested?.let{date=it.toLocalDate();time=it.toLocalTime().withSecond(0).withNano(0)}
        if(request.isNotBlank() && suggested==null)status="Please choose and review the date, time and AM/PM. Nothing has been scheduled yet."
        runCatching{runtime.callMe.reload()}
        screen("JARVIS · CALL ME"){
            val entries by runtime.callMe.state.collectAsStateWithLifecycle()
            Text("A Jarvis voice check-in, not a telephone call. Choose an editable date/time. The spoken invitation is public courtesy; private reminder titles are not read aloud. Answer to ask questions or give tasks.")
            OutlinedTextField(title,{title=it.take(100)},label={Text("Private label")})
            Text("$date · ${time.format(DateTimeFormatter.ofPattern("HH:mm"))} · ${ZoneId.systemDefault()}")
            OutlinedButton(onClick={DatePickerDialog(this@CallMeActivity,{_,y,m,d->date=LocalDate.of(y,m+1,d)},date.year,date.monthValue-1,date.dayOfMonth).show()}){Text("Change date")}
            OutlinedButton(onClick={TimePickerDialog(this@CallMeActivity,{_,h,m->time=LocalTime.of(h,m)},time.hour,time.minute,android.text.format.DateFormat.is24HourFormat(this@CallMeActivity)).show()}){Text("Change time")}
            Switch(exact,{exact=it});Text(if(exact)"Exact alarm requested · needs Android's exact-alarm access" else "Approximate · Android may delay delivery")
            Switch(spoken,{spoken=it});Text("Ring for an AI call at this time. Jarvis speaks after you answer; silent mode and Do Not Disturb are respected.")
            if(Build.VERSION.SDK_INT>=31)OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))}){Text("Android exact-alarm access")}
            OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}){Text("Android notification and check-in sound settings")}
            Button(enabled=!working && title.isNotBlank(),onClick={authorize{task{
                val at=CallMePolicy.strict(date,time,ZoneId.systemDefault());val c=runtime.callMe.save(at.toInstant().toEpochMilli(),at.zone.id,title,exact,spoken,editing)
                editing=null;status="Saved for ${at.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z"))}. "+if(c.exact)"Android received an exact-alarm request." else "Approximate delivery selected; Android may delay it."
            }}}){Text(if(editing==null)"Schedule Jarvis to call me" else "Save changed date/time")}
            entries.sortedBy{it.at}.forEach{c->
                Text("${c.title}\n${Instant.ofEpochMilli(c.at).atZone(ZoneId.of(c.zone)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z"))} · ${if(c.exact)"exact requested" else "approximate"} · ${c.status}")
                OutlinedButton(enabled=!working,onClick={val at=Instant.ofEpochMilli(c.at).atZone(ZoneId.systemDefault());date=at.toLocalDate();time=at.toLocalTime().withSecond(0).withNano(0);title=c.title;exact=c.exact;spoken=c.spoken;editing=c.id}){Text("Edit date / time")}
                TextButton(enabled=!working,onClick={authorize{runtime.cancelCallMe(c.id,c.revision);runtime.callMe.cancel(c.id);if(editing==c.id)editing=null}}){Text("Cancel and delete")}
                if(c.status in setOf("NOTIFIED","TEXT_ONLY","SPOKEN"))Button(enabled=!working,onClick={status=if(runtime.requestAiCall(com.jarvis.assistant.call.AiCallSource.CHECK_IN,c.id,c.revision,showUi=true,answerOnOpen=true))"Answering your Jarvis check-in…" else "Finish the current conversation, then tap Answer. If this reminder was edited or deleted, refresh it first."}){Text("Answer · speak and listen")}
            }
            Text("Start listening beforehand for a hands-free follow-up. Without an already-enabled background microphone, Android may require you to unlock and tap Answer. Inexact alarms, Doze, phone restarts, permissions, Fish connectivity and OEM battery policies can affect timing/audio; the recorded status distinguishes notification, speech completion and unavailable playback. No hidden microphone or new phone-call permission is enabled.")
        }
    }
}
