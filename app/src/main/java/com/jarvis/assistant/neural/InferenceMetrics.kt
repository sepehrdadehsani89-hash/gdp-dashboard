package com.jarvis.assistant.neural
import org.json.JSONObject
/** Observations from actual inference, never preset phone performance values. */
data class InferenceMetrics(val profile:String,val modelBytes:Long,val promptTokens:Long,val generatedTokens:Long,val loadMs:Long,val firstTokenMs:Long?,val totalMs:Long,val peakSampledPssKiB:Long,val thermalStatus:Int,val generationMs:Long,val modelHeader:Map<String,Long> = emptyMap()){
    fun json()=JSONObject().put("model_header",JSONObject(modelHeader)).put("profile",profile).put("model_bytes",modelBytes).put("prompt_tokens",promptTokens).put("generated_tokens",generatedTokens).put("load_ms",loadMs).put("first_token_ms",firstTokenMs ?: JSONObject.NULL).put("total_ms",totalMs).put("peak_sampled_app_pss_kib",peakSampledPssKiB).put("thermal_status",thermalStatus).put("tokens_per_generation_second",if(generationMs>0)generatedTokens*1000.0/generationMs else JSONObject.NULL).put("battery_energy","not measured").put("device",android.os.Build.MODEL).toString(2)
}
