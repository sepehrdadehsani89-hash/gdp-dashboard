package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.omnisystem.FeatureVault
import com.jarvis.assistant.omnisystem.ModuleHttp
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Output voice selection only; never an authentication or speaker-recognition identity. */
object CustomVoicePolicy {
    const val REQUESTED_ID="05b36da8574341d0803391491850db20"
    const val SPANISH_ID="ae74d1059517440aa6e5d2d50598d179"
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
class CustomVoiceConfig(val id:String=CustomVoicePolicy.REQUESTED_ID,val model:String="s2.1-pro",val key:String="",val enabled:Boolean=false,val privateAllowed:Boolean=false)
class CustomVoice(private val vault:FeatureVault,private val http:ModuleHttp=ModuleHttp(okhttp3.OkHttpClient.Builder().connectTimeout(8,java.util.concurrent.TimeUnit.SECONDS).readTimeout(20,java.util.concurrent.TimeUnit.SECONDS).callTimeout(25,java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()),private val greetingCache:PublicGreetingCache?=null) {
    private val greetingMutex=kotlinx.coroutines.sync.Mutex()
    private fun namespace(language:AppLanguage)=if(language==AppLanguage.ES)"spanish_output_voice" else "custom_output_voice"
    fun config(language:AppLanguage=AppLanguage.EN):CustomVoiceConfig {
        val default=if(language==AppLanguage.ES)CustomVoiceConfig(id=CustomVoicePolicy.SPANISH_ID) else CustomVoiceConfig()
        val raw=vault.read(namespace(language)) ?: return default
        return runCatching{val j=JSONObject(raw);CustomVoiceConfig(j.getString("id"),j.getString("model"),j.getString("key"),j.optBoolean("enabled"),j.optBoolean("private_allowed",false))}.getOrElse{default}
    }
    fun save(c:CustomVoiceConfig,language:AppLanguage=AppLanguage.EN){
        require(language!=AppLanguage.ES || c.id==CustomVoicePolicy.SPANISH_ID)
        require(CustomVoicePolicy.validId(c.id) && c.model.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        require(c.key.length in 1..1000 && c.key.none{it=='\r'||it=='\n'})
        vault.write(namespace(language),JSONObject().put("id",c.id).put("model",c.model).put("key",c.key).put("enabled",c.enabled).put("private_allowed",c.privateAllowed).toString())
    }
    fun disable(language:AppLanguage=AppLanguage.EN){val c=config(language);if(c.key.isNotBlank())save(CustomVoiceConfig(c.id,c.model,c.key,false,c.privateAllowed),language)}
    fun revokePrivate(language:AppLanguage){val c=config(language);if(c.key.isNotBlank())save(CustomVoiceConfig(c.id,c.model,c.key,c.enabled,false),language)}
    fun clear(language:AppLanguage=AppLanguage.EN)=vault.remove(namespace(language))
    suspend fun preview(c:CustomVoiceConfig,language:AppLanguage=AppLanguage.EN):ByteArray {
        require(language!=AppLanguage.ES || c.id==CustomVoicePolicy.SPANISH_ID)
        require(CustomVoicePolicy.validId(c.id) && c.key.isNotBlank() && c.model.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        val metadata=http.json(Request.Builder().url("https://api.fish.audio/model/${c.id}").header("Authorization","Bearer ${c.key}").build(),100_000)
        check(metadata.optString("_id")==c.id){"Voice service returned a different identity."}
        return synthesize(c,if(language==AppLanguage.ES)"Hola. Esta es una prueba de la voz española exclusiva de Jarvis." else "Hello. This is a preview of your selected Jarvis voice.")
    }
    suspend fun synthesize(c:CustomVoiceConfig,text:String):ByteArray = if(PublicGreetingCache.eligible(text))greetingMutex.withLock{cachedOrFetch(c,text)}else cachedOrFetch(c,text)
    private suspend fun cachedOrFetch(c:CustomVoiceConfig,text:String):ByteArray {
        require(c.key.isNotBlank() && c.model.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){greetingCache?.read(c,text)}?.let{return it}
        return http.bytes(Request.Builder().url("https://api.fish.audio/v1/tts")
            .header("Authorization","Bearer ${c.key}").header("model",c.model)
            .post(CustomVoicePolicy.payload(text,c.id).toRequestBody("application/msgpack".toMediaType())).build(),4_000_000).also{audio->check(audio.isNotEmpty());kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){greetingCache?.write(c,text,audio)}}
    }
    fun clearGreetingCache(){greetingCache?.clear()}
}
