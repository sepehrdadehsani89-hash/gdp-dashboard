package com.jarvis.assistant.power

data class PowerAlertConfig(
    val enabled:Boolean=true,val critical:Boolean=true,val chargingStart:Boolean=true,
    val full:Boolean=true,val repeatMinutes:Int=15,val disconnect:Boolean=false,val progressMinutes:Int=0,
)
enum class PowerAlertKind { CRITICAL, FULL, REMINDER, START, ESTIMATE, PROGRESS, DISCONNECTED }
data class PowerAttempt(val count:Int=0,val retryAt:Long=0)
data class PowerAlert(val kind:PowerAlertKind,val key:String,val level:Int?=null,val hadEstimate:Boolean=false)
data class PowerAlertState(
    val percent:Int?=null,val connected:Boolean?=null,val episode:Long=0,val mask:Int=0,
    val session:Long=0,val startDone:Boolean=false,val fullDone:Boolean=false,val estimateDone:Boolean=false,
    val dismissed:Boolean=false,val snoozeUntil:Long=0,val reminderAt:Long=0,
    val criticalPending:Int?=null,val disconnectPending:Boolean=false,
    val attempts:Map<String,PowerAttempt> = emptyMap(),val progressAt:Long=0,val reliable:Boolean=false,
)

/** Pure transition/queue policy. One latest observed critical level; no missed-level replay.
 * A durable attempt is not an acknowledgement of audibility. Only completed playback is acknowledged. */
object PowerAlertPolicy {
    fun observe(old:PowerAlertState,r:PowerReading):PowerAlertState {
        val percent=r.battery.percent
        val newCharge=r.connected==true && old.connected!=true
        val newDischarge=old.connected==true && r.connected==false || percent!=null && percent>=15 && old.percent?.let{it<15}==true
        val episode=old.episode+if(newDischarge)1 else 0
        val session=old.session+if(newCharge)1 else 0
        val mask=if(newDischarge || (percent ?: -1)>=15)0 else old.mask
        val critical=if(r.condition==PowerCondition.UNKNOWN)old.criticalPending else if(r.condition in setOf(PowerCondition.DISCHARGING,PowerCondition.PLUGGED_NOT_CHARGING) && percent in 1..10 &&
            (!old.reliable || old.percent==null || newDischarge || percent!!<old.percent || old.criticalPending==percent) && mask and (1 shl percent!!)==0)percent else null
        val reset=newCharge || r.connected==false
        return old.copy(percent=percent ?: old.percent,connected=r.connected ?: old.connected,episode=episode,session=session,mask=mask,
            criticalPending=critical,startDone=if(reset)false else old.startDone,
            fullDone=if(reset)false else old.fullDone,estimateDone=if(reset)false else old.estimateDone,
            dismissed=if(reset)false else old.dismissed,snoozeUntil=if(reset)0 else old.snoozeUntil,
            reminderAt=if(reset)0 else old.reminderAt,
            progressAt=if(reset)0 else old.progressAt,
            reliable=r.condition!=PowerCondition.UNKNOWN && percent!=null,
            disconnectPending=old.connected==true && r.connected==false || !newCharge && old.disconnectPending,
            attempts=old.attempts.filterKeys{it.startsWith("critical:$episode:") || it.startsWith("charge:$session:")}.entries.toList().takeLast(16).associate{it.toPair()})
    }
    fun next(s:PowerAlertState,r:PowerReading,c:PowerAlertConfig,now:Long):PowerAlert? {
        if(!c.enabled)return null
        val candidates=buildList {
            s.criticalPending?.takeIf{it==r.battery.percent && !r.charging && r.condition in setOf(PowerCondition.DISCHARGING,PowerCondition.PLUGGED_NOT_CHARGING) && (it>5 || c.critical)}?.let{
                add(PowerAlert(PowerAlertKind.CRITICAL,"critical:${s.episode}:$it",it))
            }
            if(r.connected==true && !s.dismissed && now>=s.snoozeUntil){
                if(r.verifiedFull && c.full){
                    if(!s.fullDone)add(PowerAlert(PowerAlertKind.FULL,"charge:${s.session}:full",100))
                    else if(c.repeatMinutes>0 && s.reminderAt>0 && now>=s.reminderAt)add(PowerAlert(PowerAlertKind.REMINDER,"charge:${s.session}:reminder:${s.reminderAt}",100))
                }else if(r.charging && !r.verifiedFull && r.battery.percent!=null && c.chargingStart && !s.startDone){
                    add(PowerAlert(PowerAlertKind.START,"charge:${s.session}:start",r.battery.percent,r.estimate!=null))
                }else if(r.charging && !r.verifiedFull && c.chargingStart && s.startDone && !s.estimateDone && r.estimate!=null){
                    add(PowerAlert(PowerAlertKind.ESTIMATE,"charge:${s.session}:estimate",r.battery.percent,true))
                }
                if(r.charging && !r.verifiedFull && c.progressMinutes>0 && s.progressAt>0 && now>=s.progressAt)add(PowerAlert(PowerAlertKind.PROGRESS,"charge:${s.session}:progress:${s.progressAt}",r.battery.percent,r.estimate!=null))
            }
            if(s.disconnectPending && r.connected==false && c.disconnect)add(PowerAlert(PowerAlertKind.DISCONNECTED,"charge:${s.session}:disconnect"))
        }
        // A critical request waiting for its retry is retained; lower-priority chatter cannot replace it.
        val candidate=candidates.firstOrNull() ?: return null
        val attempt=s.attempts[candidate.key] ?: PowerAttempt()
        return candidate.takeIf{attempt.count<3 && now>=attempt.retryAt}
    }
    fun requested(s:PowerAlertState,a:PowerAlert,now:Long):PowerAlertState {
        val previous=s.attempts[a.key] ?: PowerAttempt()
        return s.copy(attempts=(s.attempts+(a.key to PowerAttempt(previous.count+1,now+120_000))).entries.toList().takeLast(16).associate{it.toPair()})
    }
    fun failed(s:PowerAlertState,a:PowerAlert,now:Long)=s.copy(attempts=s.attempts+(a.key to (s.attempts[a.key] ?: PowerAttempt(1)).copy(retryAt=now+if((s.attempts[a.key]?.count ?: 1)>1)120_000 else 30_000)))
    fun completed(s:PowerAlertState,a:PowerAlert,c:PowerAlertConfig,now:Long):PowerAlertState {
        val base=s.copy(attempts=s.attempts-a.key)
        if(a.kind!=PowerAlertKind.CRITICAL && !a.key.startsWith("charge:${s.session}:"))return base
        return when(a.kind){
            PowerAlertKind.CRITICAL->if(a.key.startsWith("critical:${s.episode}:"))base.copy(mask=base.mask or (1 shl requireNotNull(a.level)),criticalPending=base.criticalPending.takeUnless{it==a.level})else base
            PowerAlertKind.START->base.copy(startDone=true,estimateDone=a.hadEstimate,progressAt=if(c.progressMinutes>0)now+c.progressMinutes*60_000L else 0)
            PowerAlertKind.PROGRESS->base.copy(progressAt=if(c.progressMinutes>0)now+c.progressMinutes*60_000L else 0)
            PowerAlertKind.ESTIMATE->base.copy(estimateDone=true)
            PowerAlertKind.FULL,PowerAlertKind.REMINDER->base.copy(fullDone=true,reminderAt=if(c.repeatMinutes>0)now+c.repeatMinutes*60_000L else 0)
            PowerAlertKind.DISCONNECTED->base.copy(disconnectPending=false)
        }
    }
    fun dismiss(s:PowerAlertState)=s.copy(dismissed=true,snoozeUntil=0,reminderAt=0)
    fun snooze(s:PowerAlertState,now:Long,minutes:Int=15)=s.copy(snoozeUntil=now+minutes.coerceIn(5,60)*60_000L,reminderAt=now+minutes.coerceIn(5,60)*60_000L)
    fun fullCheckAt(s:PowerAlertState,r:PowerReading,c:PowerAlertConfig,now:Long):Long? {
        if(!c.enabled || s.dismissed)return null
        if(r.charging && !r.verifiedFull && c.progressMinutes>0 && s.progressAt>0){
            val a=s.attempts["charge:${s.session}:progress:${s.progressAt}"]
            return if((a?.count ?: 0)>=3)null else maxOf(s.progressAt,s.snoozeUntil,a?.retryAt ?: 0,now)
        }
        if(!c.full || !r.verifiedFull)return null
        if(s.snoozeUntil>now)return s.snoozeUntil
        if(!s.fullDone){val a=s.attempts["charge:${s.session}:full"];return if(a?.count==3)null else maxOf(now,a?.retryAt ?: now)}
        if(c.repeatMinutes<=0 || s.reminderAt<=0)return null
        val a=s.attempts["charge:${s.session}:reminder:${s.reminderAt}"]
        return if((a?.count ?: 0)>=3)null else maxOf(s.reminderAt,a?.retryAt ?: 0,now)
    }
}
