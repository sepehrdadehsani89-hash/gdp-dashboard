package com.jarvis.assistant.screen

/** Process-only projection ownership; Android grants are never persisted or replayed. */
object ProjectionLease {
    private var owner:String?=null
    @Synchronized fun claim(id:String):Boolean {if(owner!=null)return owner==id;owner=id;return true}
    @Synchronized fun owned(id:String)=owner==id
    @Synchronized fun release(id:String){if(owner==id)owner=null}
    @Synchronized fun available()=owner==null
}
enum class RecordingStage { IDLE, CONSENT_REQUIRED, STARTING, RECORDING, FINALIZING, SAVED, CANCELED, FAILED }
data class RecordingStatus(val stage:RecordingStage=RecordingStage.IDLE,val message:String="No owned video recording is active.",val uri:String?=null,val bytes:Long=0,val durationMs:Long=0){val active get()=stage in setOf(RecordingStage.STARTING,RecordingStage.RECORDING,RecordingStage.FINALIZING)}
object RecordingPolicy {
    fun dimensions(width:Int,height:Int):Pair<Int,Int>{require(width>0 && height>0);val scale=minOf(1.0,1280.0/maxOf(width,height));return ((width*scale).toInt().coerceAtLeast(2)/2*2) to ((height*scale).toInt().coerceAtLeast(2)/2*2)}
    fun saved(stopSucceeded:Boolean,bytes:Long,durationMs:Long)=stopSucceeded && bytes>1024 && durationMs>0
    fun validConsent(issued:Long,now:Long)=now-issued in 0..120_000
}
