package com.jarvis.assistant.companion

data class DesktopConnectionProof(val device:String,val session:String,val at:Long){override fun toString()="DesktopConnectionProof(identity=redacted, at=$at)"}
object DesktopConnectionProofPolicy {
    fun current(proof:DesktopConnectionProof?,pair:DesktopPair?,now:Long)=proof!=null && pair!=null && proof.at>0 && now>0 && proof.device==pair.deviceId && proof.session==pair.sessionId && now-proof.at in 0..60_000
    fun text(proof:DesktopConnectionProof?,pair:DesktopPair?,now:Long)=when{
        pair==null->"Computer · unpaired"
        current(proof,pair,now)->"Computer · paired / authenticated\nRecent signed capability response verified\nCommand completion is separate"
        else->"Computer · paired locally\nCurrent authentication / connectivity unverified"
    }
}
