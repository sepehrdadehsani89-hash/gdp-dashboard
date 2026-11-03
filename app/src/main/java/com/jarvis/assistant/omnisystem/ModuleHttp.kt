package com.jarvis.assistant.omnisystem

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ModuleServiceFailure(val code:Int):IOException("Service request failed (HTTP $code).")
/** No logging, cookies, redirects or unbounded response allocation. Cancellation cancels the call. */
class ModuleHttp(private val client:OkHttpClient=sharedClient) {
    companion object {
        private val sharedClient=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS)
            .readTimeout(30,TimeUnit.SECONDS).callTimeout(50,TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
    }
    suspend fun json(request:Request,limit:Int=1_000_000):JSONObject=JSONObject(bytes(request,limit).toString(Charsets.UTF_8))
    suspend fun bytes(request:Request,limit:Int):ByteArray=suspendCancellableCoroutine{continuation->
        require(limit in 1..8_000_000)
        val call=client.newCall(request)
        continuation.invokeOnCancellation{call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException){if(continuation.isActive)continuation.resumeWithException(IOException("Service unavailable."))}
            override fun onResponse(call:Call,response:Response){
                try {response.use{
                    if(!it.isSuccessful)throw ModuleServiceFailure(it.code)
                    val body=it.body ?: throw IOException("Empty response.")
                    require(body.contentLength()<=limit){"Response exceeds limit."}
                    val result=ByteArrayOutputStream();val buffer=ByteArray(8192)
                    body.byteStream().use{input->while(true){
                        val n=input.read(buffer);if(n<0)break
                        require(result.size().toLong()+n<=limit){"Response exceeds limit."};result.write(buffer,0,n)
                    }}
                    if(continuation.isActive)continuation.resume(result.toByteArray())
                }} catch(e:Exception){if(continuation.isActive)continuation.resumeWithException(e)}
            }
        })
    }
}
