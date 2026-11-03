package com.jarvis.assistant.security

import android.content.Context

/** Non-secret fail-closed recorder pause, retained across process recreation of Android credential UI. */
object SystemCredentialPrivacy {
    private fun preferences(context:Context)=context.getSharedPreferences("jarvis_system_credential_privacy_v1",Context.MODE_PRIVATE)
    fun pending(context:Context)=runCatching{preferences(context).getBoolean("pending",false)}.getOrDefault(true)
    fun begin(context:Context){check(preferences(context).edit().putBoolean("pending",true).commit())}
    fun end(context:Context){check(preferences(context).edit().putBoolean("pending",false).commit())}
}
