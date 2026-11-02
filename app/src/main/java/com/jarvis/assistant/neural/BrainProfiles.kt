package com.jarvis.assistant.neural

/** Estimates are admission budgets, never measured usage or download byte counts. */
data class BrainProfile(val id:String,val title:String,val repository:String,val file:String,val context:Int,val workingMiB:Int,val minimumTotalMiB:Int,val maximumBytes:Long,val available:Boolean=true)
object BrainProfiles {
    val lite=BrainProfile("lite","Jarvis Lite","Qwen/Qwen2.5-0.5B-Instruct-GGUF","qwen2.5-0.5b-instruct-q4_k_m.gguf",1536,850,1800,600_000_000)
    val balanced=BrainProfile("balanced","Jarvis Balanced · experimental","Qwen/Qwen2.5-1.5B-Instruct-GGUF","qwen2.5-1.5b-instruct-q4_k_m.gguf",2048,1900,5500,1_600_000_000)
    val advanced=BrainProfile("advanced","Jarvis Advanced · hardware validation required","","",0,0,0,0,false)
    val all=listOf(lite,balanced,advanced)
    fun get(id:String)=all.firstOrNull{it.id==id} ?: lite
    fun admission(profile:BrainProfile,totalMiB:Long,availableMiB:Long,lowMemory:Boolean,thermal:Int,arm64:Boolean):String?=when {
        !profile.available->"Advanced is unavailable until a compatible larger model and hardware are verified."
        !arm64->"Phone profiles require an ARM64 Android device."
        lowMemory || totalMiB<profile.minimumTotalMiB || availableMiB<profile.workingMiB->"Not enough available RAM for this profile. Close other apps or choose Lite. Estimates include a safety margin."
        thermal>=3->"The phone is too hot for local inference. Let it cool and retry."
        else->null
    }
}
