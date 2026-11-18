package com.jarvis.assistant.reminders

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import java.time.LocalDateTime
import java.time.ZoneId

class RemindersActivity:PrivateModuleActivity(){
    private var revision by mutableIntStateOf(0)
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){revision++;status=if(it)"Android notification permission granted. No reminder was created." else "Notification permission denied. No reminder was scheduled."}
    override fun onResume(){super.onResume();revision++}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("JARVIS · REMINDERS"){
            var title by remember{mutableStateOf("")};var time by remember{mutableStateOf("")};var editing by remember{mutableStateOf<String?>(null)}
            val rows=remember(revision){runCatching{runtime.reminders.store.list()}}
            Text("Persistent local reminders use Android WorkManager. Delivery can be delayed by Doze or Xiaomi battery management. For a precise ringing alarm, use Android's Clock app; this page does not claim exact delivery. Saved times are fixed instants, displayed in their creation time zone.")
            Text("Reminder notifications enabled: ${runtime.reminders.allowed()}")
            Button(onClick={if(Build.VERSION.SDK_INT>=33)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)else startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))}){Text("Allow / review Android reminder notifications")}
            OutlinedTextField(title,{title=it.take(300)},label={Text("Reminder text")})
            OutlinedTextField(time,{time=it.take(32)},label={Text("Local date and24-hour time · YYYY-MM-DDTHH:MM")})
            Button(enabled=!working,onClick={task{
                val zone=ZoneId.systemDefault();val local=LocalDateTime.parse(time.trim());val offsets=zone.rules.getValidOffsets(local)
                require(offsets.size==1){"This local time is skipped or repeated by daylight saving. Choose an unambiguous time."}
                val row=runtime.reminders.create(title,local.toInstant(offsets.single()).toEpochMilli(),zone.id,editing)
                editing=null;title="";time="";revision++;status="SCHEDULED · ${ReminderManager.format(row)}. Android accepted the approximate work request; delivery has not happened yet."
            }}){Text(if(editing==null)"Save and schedule reminder" else "Save changed reminder")}
            rows.exceptionOrNull()?.let{Text("Saved reminders could not be read. Existing data was preserved.")}
            rows.getOrDefault(emptyList()).sortedBy{it.at}.forEach{row->HorizontalDivider();Text(row.title);Text("${ReminderManager.format(row)} · ${row.status}\nID ${row.id}")
                OutlinedButton(enabled=!working,onClick={editing=row.id;title=row.title;time=java.time.Instant.ofEpochMilli(row.at).atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0).toString();status="Editing this reminder. Choose a future time and save to replace its schedule."}){Text("Edit / reschedule")}
                TextButton(enabled=!working,onClick={task{runtime.reminders.remove(row.id);revision++;status="Reminder removed locally; Android cancellation requested."}}){Text("Remove this reminder")}
            }
            Text("NOTIFIED means a notification was posted to Android, not that you read or heard it. DELIVERY_UNCONFIRMED means a previous delivery was interrupted; review or reschedule it rather than assuming it succeeded.")
            Text("Notifications contain a generic prompt to open this private page. Reminder text is never placed in the notification, sent to an AI provider, or spoken automatically by this feature.")
        }
    }
}
