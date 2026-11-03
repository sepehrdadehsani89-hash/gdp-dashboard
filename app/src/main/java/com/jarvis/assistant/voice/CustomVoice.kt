package com.jarvis.assistant.voice

import com.jarvis.assistant.omnisystem.FeatureVault
import com.jarvis.assistant.omnisystem.ModuleHttp
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Output voice selection only; never an authentication or speaker-recognition identity. */
object CustomVoicePolicy {
    const val REQUESTED_ID="05b36da8574341d0803391491850db20"
    fun validId(id:String)=id.matches(Regex("[A-Za-z0-9_-]{1,100}"))
    fun eligible(enabled:Boolean,allowCloudSpeech:Boolean,privateTurn:Boolean)=enabled && allowCloudSpeech && !privateTurn
    // The official Fish SDK uses MessagePack. Encode only this fixed, bounded string map.
    fun payload(text:String,id:String):ByteArray {
        require(validId(id) && text.isNotBlank() && text.length<=3500)
        val out=ByteArrayOutputStream()
        fun string(value:String){val b=value.toByteArray(Charsets.UTF_8);when{
            b.size<32->out.write(0xa0 or b.size)
            b.size<=255->{out.write(0xd9);out.write(b.size)}
            else->{out.write(0xda);out.write(b.size ushr 8);out.write(b.size and 255)}
        };out.write(b)}
        out.write(0x83)
        string("text");string(text);string("reference_id");string(id);string("format");string("mp3")
        return out.toByteArray()
    }
}
class CustomVoiceConfig(val id:String=CustomVoicePolicy.REQUESTED_ID,val model:String="s2.1-pro",val key:String="",val enabled:Boolean=false)
class CustomVoice(private val vault:FeatureVault,private val http:ModuleHttp=ModuleHttp()) {
    fun config():CustomVoiceConfig {
        val raw=vault.read("custom_output_voice") ?: return CustomVoiceConfig()
        return runCatching{val j=JSONObject(raw);CustomVoiceConfig(j.getString("id"),j.getString("model"),j.getString("key"),j.optBoolean("enabled"))}.getOrElse{CustomVoiceConfig()}
    }
    fun save(c:CustomVoiceConfig){
        require(CustomVoicePolicy.validId(c.id) && c.model.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        require(c.key.length in 1..1000 && c.key.none{it=='\r'||it=='\n'})
        vault.write("custom_output_voice",JSONObject().put("id",c.id).put("model",c.model).put("key",c.key).put("enabled",c.enabled).toString())
    }
    fun disable(){val c=config();if(c.key.isNotBlank())save(CustomVoiceConfig(c.id,c.model,c.key,false))}
    fun clear()=vault.remove("custom_output_voice")
    suspend fun preview(c:CustomVoiceConfig):ByteArray {
        require(CustomVoicePolicy.validId(c.id) && c.key.isNotBlank() && c.model.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        val metadata=http.json(Request.Builder().url("https://api.fish.audio/model/${c.id}").header("Authorization","Bearer ${c.key}").build(),100_000)
        check(metadata.optString("_id")==c.id){"Voice service returned a different identity."}
        return synthesize(c,"Hello. This is a preview of your selected Jarvis voice.")
    }
    suspend fun synthesize(c:CustomVoiceConfig,text:String):ByteArray {
        require(c.key.isNotBlank() && c.model.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        return http.bytes(Request.Builder().url("https://api.fish.audio/v1/tts")
            .header("Authorization","Bearer ${c.key}").header("model",c.model)
            .post(CustomVoicePolicy.payload(text,c.id).toRequestBody("application/msgpack".toMediaType())).build(),4_000_000).also{check(it.isNotEmpty())}
    }
}
