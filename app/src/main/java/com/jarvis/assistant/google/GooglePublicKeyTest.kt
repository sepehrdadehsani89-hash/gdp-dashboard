package com.jarvis.assistant.google

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleKeyFailure(val category:String):java.io.IOException(category)
data class GooglePublicKeyResult(val languages:Int,val elapsedMillis:Long)
/** A public YouTube metadata read tests only API-key access, never private Google identity. */
class GooglePublicKeyTest internal constructor(private val client:OkHttpClient,private val endpoint:HttpUrl){
    constructor():this(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).connectTimeout(10,TimeUnit.SECONDS).readTimeout(12,TimeUnit.SECONDS).callTimeout(15,TimeUnit.SECONDS).build(),"https://www.googleapis.com/youtube/v3/i18nLanguages".toHttpUrl())
    init{require(!client.followRedirects && !client.followSslRedirects && !client.retryOnConnectionFailure);require(endpoint.toString()=="https://www.googleapis.com/youtube/v3/i18nLanguages" || endpoint.scheme=="http" && endpoint.host in setOf("localhost","127.0.0.1","::1"))}
    fun read(key:String,androidPackage:String?=null,certificateSha1:String?=null):GooglePublicKeyResult {
        if(!key.matches(Regex("AIza[A-Za-z0-9_-]{35}")))throw GoogleKeyFailure("KEY_FORMAT")
        val builder=Request.Builder().url(endpoint.newBuilder().addQueryParameter("part","snippet").addQueryParameter("key",key).build())
        if(androidPackage!=null || certificateSha1!=null){if(androidPackage==null || certificateSha1==null || !androidPackage.matches(Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")) || !certificateSha1.matches(Regex("[A-Fa-f0-9]{40}")))throw GoogleKeyFailure("ANDROID_IDENTITY_FORMAT");builder.header("X-Android-Package",androidPackage).header("X-Android-Cert",certificateSha1)}
        val request=builder.build()
        val started=System.nanoTime()
        try{client.newCall(request).execute().use{response->
            if(!response.isSuccessful)throw GoogleKeyFailure(when(response.code){401->"AUTHENTICATION";403->"API_KEY_OR_API_CONFIGURATION";429->"QUOTA";in 300..399->"REDIRECT_REJECTED";else->"HTTP_${response.code}"})
            if(response.body?.contentType()?.let{it.type=="application" && it.subtype=="json"}!=true)throw GoogleKeyFailure("INVALID_RESPONSE")
            val input=response.body?.byteStream() ?: throw GoogleKeyFailure("EMPTY_RESPONSE")
            val raw=input.use{stream->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);while(true){val n=stream.read(buffer);if(n<0)break;if(out.size()+n>128*1024)throw GoogleKeyFailure("RESPONSE_TOO_LARGE");out.write(buffer,0,n)};out.toString("UTF-8")}
            val j=try{JSONObject(raw)}catch(_:Exception){throw GoogleKeyFailure("INVALID_RESPONSE")}
            if(j.optString("kind")!="youtube#i18nLanguageListResponse")throw GoogleKeyFailure("INVALID_RESPONSE")
            val rows=j.optJSONArray("items") ?: throw GoogleKeyFailure("INVALID_RESPONSE")
            if(rows.length() !in 1..500)throw GoogleKeyFailure("INVALID_RESPONSE")
            for(i in 0 until rows.length()){val row=rows.optJSONObject(i) ?: throw GoogleKeyFailure("INVALID_RESPONSE");val id=row.opt("id");val name=row.optJSONObject("snippet")?.opt("name");if(id !is String || id.length !in 1..30 || name !is String || name.length !in 1..200)throw GoogleKeyFailure("INVALID_RESPONSE")}
            return GooglePublicKeyResult(rows.length(),(System.nanoTime()-started)/1_000_000)
        }}catch(e:GoogleKeyFailure){throw e}catch(_:java.net.SocketTimeoutException){throw GoogleKeyFailure("TIMEOUT")}catch(_:java.io.IOException){throw GoogleKeyFailure("NETWORK")}
    }
}
