package com.jarvis.assistant.neural

/** Memory estimates are information only. They never authorize or refuse execution. */
data class BrainProfile(val id:String,val title:String,val repository:String,val file:String,val context:Int,val workingMiB:Int,val maximumBytes:Long,val available:Boolean=true,val minimumBytes:Long=1_000_000)
object BrainProfiles {
    val lite=BrainProfile("lite","Jarvis Lite","Qwen/Qwen2.5-0.5B-Instruct-GGUF","qwen2.5-0.5b-instruct-q4_k_m.gguf",1536,850,600_000_000)
    val balanced=BrainProfile("balanced","Jarvis Balanced · near 1 GB · experimental","Qwen/Qwen2.5-1.5B-Instruct-GGUF","qwen2.5-1.5b-instruct-q4_k_m.gguf",2048,1900,1_300_000_000,minimumBytes=900_000_000)
    val advanced=BrainProfile("advanced","Jarvis Advanced · hardware validation required","","",0,0,0,false)
    val all=listOf(lite,balanced,advanced)
    fun get(id:String)=all.firstOrNull{it.id==id} ?: lite
    fun compatibilityError(profile:BrainProfile,thermal:Int,arm64:Boolean):String?=when {
        !profile.available->"Advanced is unavailable until a compatible larger model and hardware are verified."
        !arm64->"Phone profiles require an ARM64 Android device."
        thermal>=3->"The phone is too hot for local inference. Let it cool and retry."
        else->null
    }
}
