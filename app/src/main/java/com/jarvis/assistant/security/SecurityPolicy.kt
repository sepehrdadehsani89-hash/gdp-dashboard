package com.jarvis.assistant.security

import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.core.choose
import java.util.UUID

enum class VerificationMode { THREE_ANDROID_CHALLENGES, ANDROID_AND_TWO_CONFIRMATIONS, ANDROID_SINGLE_CHALLENGE }
enum class SecurityStage { NOT_CONFIGURED, VERIFYING, VERIFIED, AUTHENTICATION_REQUIRED, CANCELLED, UNAVAILABLE }
data class CredentialChallenge(val id:String,val stage:Int,val issued:Long)
/** Only a matching, live system callback can satisfy a credential stage. No credential value enters this model. */
class SecurityVerification(private val now:()->Long){
    var mode=VerificationMode.THREE_ANDROID_CHALLENGES;private set
    var completed=0;private set
    var state=SecurityStage.NOT_CONFIGURED;private set
    var pending:CredentialChallenge?=null;private set
    private var started=0L
    fun begin(secure:Boolean,mode:VerificationMode){
        completed=0;pending=null;this.mode=mode;started=now()
        state=if(secure)SecurityStage.VERIFYING else SecurityStage.UNAVAILABLE
    }
    fun needsAndroid()=state==SecurityStage.VERIFYING && (mode in setOf(VerificationMode.THREE_ANDROID_CHALLENGES,VerificationMode.ANDROID_SINGLE_CHALLENGE) || completed==0)
    fun challenge(secure:Boolean):CredentialChallenge? {
        if(!valid(secure) || !needsAndroid() || pending!=null)return null
        return CredentialChallenge(UUID.randomUUID().toString(),completed+1,now()).also{pending=it}
    }
    fun systemResult(id:String,success:Boolean,secure:Boolean):Boolean{
        val p=pending ?: return false
        if(id!=p.id)return false
        pending=null
        if(!valid(secure) || !success || now()-p.issued !in 0..120_000){abort();return false}
        advance();return true
    }
    fun confirmSetup(secure:Boolean):Boolean{
        if(!valid(secure) || needsAndroid() || pending!=null || completed !in 1..2)return false
        advance();return true
    }
    fun abort(){pending=null;completed=0;state=SecurityStage.CANCELLED}
    fun invalidate(){pending=null;completed=0;state=SecurityStage.UNAVAILABLE}
    private fun advance(){completed++;if(completed==(if(mode==VerificationMode.ANDROID_SINGLE_CHALLENGE)1 else 3))state=SecurityStage.VERIFIED}
    private fun valid(secure:Boolean):Boolean{
        if(state!=SecurityStage.VERIFYING)return false
        if(!secure || now()-started !in 0..600_000){invalidate();return false}
        return true
    }
}
enum class ScreenCommand { UNLOCK, LOCK }
object ScreenCommandPolicy {
    fun parse(input:String):ScreenCommand?{
        val t=AppMatching.normalize(input).removePrefix("hey jarvis ").removePrefix("jarvis ").removePrefix("هی جارویس ").removePrefix("جارویس ").removePrefix("please ")
        return when(t){
            "unlock my phone","unlock the screen","unlock my device","unlock my screen","open my locked screen","desbloquea mi telefono","desbloquea la pantalla","قفل گوشی را باز کن","قفل صفحه را باز کن"->ScreenCommand.UNLOCK
            "lock my phone","lock my screen","lock the screen","turn off my screen","bloquea mi telefono","bloquea la pantalla","گوشی را قفل کن","صفحه را قفل کن"->ScreenCommand.LOCK
            else->null
        }
    }
}

enum class UnlockPresentation { DISABLED, ALREADY_UNLOCKED, FOREGROUND_REQUEST, NOTIFICATION, OPEN_MANUALLY }
object UnlockPresentationPolicy {
    fun choose(enabled:Boolean,keyguardLocked:Boolean,deviceLocked:Boolean,foreground:Boolean,notifications:Boolean)=when{
        !enabled->UnlockPresentation.DISABLED
        !keyguardLocked && !deviceLocked->UnlockPresentation.ALREADY_UNLOCKED
        foreground->UnlockPresentation.FOREGROUND_REQUEST
        notifications->UnlockPresentation.NOTIFICATION
        else->UnlockPresentation.OPEN_MANUALLY
    }
}

fun SecurityStage.label(l:com.jarvis.assistant.core.AppLanguage)=l.choose(when(this){
    SecurityStage.NOT_CONFIGURED->"NOT CONFIGURED";SecurityStage.VERIFYING->"VERIFICATION IN PROGRESS";SecurityStage.VERIFIED->"VERIFIED";SecurityStage.AUTHENTICATION_REQUIRED->"AUTHENTICATION REQUIRED";SecurityStage.CANCELLED->"VERIFICATION NOT COMPLETED";SecurityStage.UNAVAILABLE->"UNAVAILABLE"
},when(this){
    SecurityStage.NOT_CONFIGURED->"NO CONFIGURADO";SecurityStage.VERIFYING->"VERIFICACIÓN EN CURSO";SecurityStage.VERIFIED->"VERIFICADO";SecurityStage.AUTHENTICATION_REQUIRED->"AUTENTICACIÓN REQUERIDA";SecurityStage.CANCELLED->"VERIFICACIÓN NO COMPLETADA";SecurityStage.UNAVAILABLE->"NO DISPONIBLE"
},when(this){
    SecurityStage.NOT_CONFIGURED->"تنظیم نشده";SecurityStage.VERIFYING->"تأیید در حال انجام";SecurityStage.VERIFIED->"تأیید شده";SecurityStage.AUTHENTICATION_REQUIRED->"احراز هویت لازم است";SecurityStage.CANCELLED->"تأیید کامل نشد";SecurityStage.UNAVAILABLE->"در دسترس نیست"
})
