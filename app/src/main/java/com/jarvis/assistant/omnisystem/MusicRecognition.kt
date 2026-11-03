package com.jarvis.assistant.omnisystem

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI

data class RecognizedSong(val title:String,val artist:String,val album:String?,val spotifyId:String?,val artwork:String?)
object MusicResultPolicy {
    fun trackId(value:String?):String?=value?.takeIf{it.matches(Regex("[A-Za-z0-9]{22}"))}
    fun artwork(value:String?):String?=value?.takeIf{url->runCatching{
        val uri=URI(url)
        url.length<=2048 && uri.scheme=="https" && uri.userInfo==null && uri.port in listOf(-1,443) &&
            uri.host in setOf("i.scdn.co","image-cdn-ak.spotifycdn.com")
    }.getOrDefault(false)}
    fun parse(json:JSONObject):RecognizedSong? {
        require(json.optString("status")=="success"){"Music provider rejected the request."}
        if(json.isNull("result"))return null
        val result=json.getJSONObject("result")
        fun text(obj:JSONObject,key:String,max:Int):String?=(obj.opt(key) as? String)?.filterNot{it.isISOControl()}?.trim()?.take(max)?.takeIf{it.isNotBlank()}
        val title=text(result,"title",240) ?: error("Music provider returned no title.")
        val artist=text(result,"artist",240) ?: error("Music provider returned no artist.")
        val spotify=result.optJSONObject("spotify")
        val images=spotify?.optJSONObject("album")?.optJSONArray("images")
        val image=if(images!=null && images.length()>0)artwork(images.optJSONObject(0)?.optString("url")) else null
        return RecognizedSong(title,artist,text(result,"album",240),trackId(spotify?.optString("id")),image)
    }
}
class MusicRecognition(private val http:ModuleHttp=ModuleHttp()) {
    suspend fun identify(wav:ByteArray,apiKey:String):RecognizedSong? {
        require(apiKey.length in 1..1024 && apiKey.none{it.isWhitespace() || it.isISOControl()}){"Configure a valid AudD token privately."}
        require(wav.size in 44..1_000_000 && wav.copyOfRange(0,4).contentEquals("RIFF".toByteArray()))
        val body=MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("api_token",apiKey).addFormDataPart("return","spotify")
            .addFormDataPart("file","jarvis-sample.wav",wav.toRequestBody("audio/wav".toMediaType())).build()
        val response=http.json(Request.Builder().url("https://api.audd.io/").post(body).build())
        return MusicResultPolicy.parse(response)
    }
}
