package com.jarvis.assistant.security

import android.app.KeyguardManager
import android.content.Context

/** Non-secret setup metadata only; never stores a system credential or a reusable authentication result. */
class ScreenSecurityPreferences(private val context:Context){
    private val prefs=context.getSharedPreferences("jarvis_screen_security_v1",Context.MODE_PRIVATE)
    fun secure()=runCatching{context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure==true}.getOrDefault(false)
    val verifiedAt get()=runCatching{prefs.getLong("verified_at",0L)}.getOrDefault(0L)
    val enabled get()=secure() && verifiedAt>0 && runCatching{prefs.getBoolean("voice_enabled",false)}.getOrDefault(false)
    val publicReactor get()=runCatching{prefs.getBoolean("public_reactor",false)}.getOrDefault(false)
    val requireSensitive get()=runCatching{prefs.getBoolean("require_sensitive",true)}.getOrDefault(true)
    val mode get()=runCatching{VerificationMode.valueOf(prefs.getString("mode","").orEmpty())}.getOrDefault(VerificationMode.THREE_ANDROID_CHALLENGES)
    fun reconcile(){if(!secure() && (verifiedAt>0 || runCatching{prefs.getBoolean("voice_enabled",false)}.getOrDefault(false)))resetVerification()}
    fun resetVerification(){check(prefs.edit().remove("verified_at").remove("mode").putBoolean("voice_enabled",false).commit())}
    fun complete(mode:VerificationMode){
        check(secure())
        if(!prefs.edit().putLong("verified_at",System.currentTimeMillis()).putString("mode",mode.name).putBoolean("voice_enabled",true).commit()){
            prefs.edit().putBoolean("voice_enabled",false).remove("verified_at").commit()
            error("Setup could not be stored durably.")
        }
    }
    fun enable(value:Boolean){require(!value || secure() && verifiedAt>0);check(prefs.edit().putBoolean("voice_enabled",value).commit())}
    fun reactor(value:Boolean){check(prefs.edit().putBoolean("public_reactor",value).commit())}
    fun sensitive(value:Boolean){check(prefs.edit().putBoolean("require_sensitive",value).commit())}
    fun clear(){check(prefs.edit().clear().commit())}
}
