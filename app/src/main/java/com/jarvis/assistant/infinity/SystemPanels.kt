package com.jarvis.assistant.infinity

import android.content.Intent
import android.provider.Settings

/** A system panel requests user interaction; dispatching it never means the setting changed. */
object SystemPanels {
    val actions=mapOf("internet" to Settings.Panel.ACTION_INTERNET_CONNECTIVITY,"wifi" to Settings.Panel.ACTION_WIFI,"bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,"volume" to Settings.Panel.ACTION_VOLUME,"display" to Settings.ACTION_DISPLAY_SETTINGS,"location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,"battery_saver" to Settings.ACTION_BATTERY_SAVER_SETTINGS,"airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS,"dnd" to Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    fun intent(id:String)=Intent(requireNotNull(actions[id]){"Unsupported settings panel"}).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
