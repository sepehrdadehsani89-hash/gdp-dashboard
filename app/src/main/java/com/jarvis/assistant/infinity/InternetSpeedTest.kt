package com.jarvis.assistant.infinity

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

/** User-initiated fixed-size HTTP transfer, not a claim of ISP capacity, ICMP latency or packet loss. */
class InternetSpeedTest(private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).followRedirects(false).build(),private val baseUrl:String="https://speed.cloudflare.com") {
    data class Result(val downMbps:Double,val upMbps:Double,val httpLatencyMs:Double,val httpJitterMs:Double,val downloaded:Long,val uploaded:Long)
    suspend fun run(progress:(String)->Unit):Result=withContext(Dispatchers.IO){
        val latency=mutableListOf<Double>()
        repeat(3){progress("Measuring HTTP response ${it+1}/3");val start=System.nanoTime();transfer(Request.Builder().url("$baseUrl/__down?bytes=0&jarvis=${java.util.UUID.randomUUID()}").header("Cache-Control","no-store").build(),0);latency+=(System.nanoTime()-start)/1_000_000.0}
        progress("Downloading 4 MiB test payload")
        val downBytes=4L*1024*1024;var start=System.nanoTime()
        transfer(Request.Builder().url("$baseUrl/__down?bytes=$downBytes&jarvis=${java.util.UUID.randomUUID()}").header("Accept-Encoding","identity").header("Cache-Control","no-store").build(),downBytes)
        val down=DevicePolicy.throughputMbps(downBytes,System.nanoTime()-start)
        progress("Uploading 1 MiB generated test payload")
        val bytes=ByteArray(1024*1024).also{java.security.SecureRandom().nextBytes(it)}
        start=System.nanoTime();transfer(Request.Builder().url("$baseUrl/__up").post(bytes.toRequestBody("application/octet-stream".toMediaType())).build(),null)
        val up=DevicePolicy.throughputMbps(bytes.size.toLong(),System.nanoTime()-start)
        Result(down,up,latency.average(),latency.zipWithNext{a,b->kotlin.math.abs(a-b)}.average(),downBytes,bytes.size.toLong())
    }
    private suspend fun transfer(request:Request,expected:Long?)=coroutineScope {
        val call=client.newCall(request)
        val watcher=launch(Dispatchers.Default,start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{call.cancel()}}
        try{call.execute().use{response->
            if(!response.isSuccessful)throw IOException("Speed service returned HTTP ${response.code}. No complete speed result is available.")
            val body=response.body ?: throw IOException("Empty speed-test response")
            var count=0L
            body.byteStream().use{input->val buffer=ByteArray(65536);while(true){currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break;count+=n;if(count>(expected ?: 65536L))throw IOException("Unexpected speed-test payload size");}}
            if(expected!=null && count!=expected)throw IOException("Incomplete speed-test download")
        }}finally{watcher.cancelAndJoin()}
    }
}
