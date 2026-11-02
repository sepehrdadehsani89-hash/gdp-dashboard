package com.jarvis.assistant.neural

import com.jarvis.assistant.core.*
import com.jarvis.assistant.tools.ToolPolicy
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ResearchPolicy {
    fun publicAddress(address:InetAddress):Boolean {
        val b=address.address
        val reservedV4=b.size==4 && ((b[0].toInt() and 255)==0 || (b[0].toInt() and 255)>=224 || (b[0].toInt() and 255)==100 && (b[1].toInt() and 255) in 64..127 || (b[0].toInt() and 255)==198 && (b[1].toInt() and 255) in 18..19)
        return !reservedV4 && !address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress && !address.isSiteLocalAddress && !address.isMulticastAddress && !(b.size==16 && b[0].toInt() and 0xfe==0xfc)
    }
    fun readable(html:String):String {
        val source=html.take(500_000);val output=StringBuilder();var i=0;var hidden:String?=null
        while(i<source.length){
            if(source[i]=='<'){
                val end=source.indexOf('>',i+1);if(end<0)break
                val tag=source.substring(i+1,minOf(end,i+2001)).trim().lowercase(java.util.Locale.ROOT)
                val closing=tag.startsWith("/");val name=tag.removePrefix("/").takeWhile{it.isLetterOrDigit()}
                if(hidden!=null){if(closing && name==hidden)hidden=null}
                else if(!closing && name in setOf("script","style","noscript","svg"))hidden=name
                if(hidden==null)output.append(' ')
                i=end+1
            }else{if(hidden==null)output.append(source[i]);i++}
        }
        return output.toString().replace("&nbsp;"," ").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace(Regex("\\s+")," ").trim().take(20_000)
    }

}
/** User-requested public HTTPS reading, bounded and no redirects to internal/private services. */
class WebResearch {
    private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(12,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS)
        .dns(object:Dns{override fun lookup(hostname:String):List<InetAddress> = Dns.SYSTEM.lookup(hostname).also{addresses->if(addresses.isEmpty() || !addresses.all(ResearchPolicy::publicAddress))throw java.net.UnknownHostException("Private network destinations are not supported.")}}).build()
    private val cache=mutableMapOf<String,Pair<Long,String>>()
    suspend fun read(url:String,language:AppLanguage):AssistantReply {
        require(ToolPolicy.allowedWebUrl(url)){"Use a public HTTPS source URL without credentials or IP addresses."}
        val now=System.currentTimeMillis();val prior=cache[url]
        val text=if(prior!=null && now-prior.first in 0..600_000)prior.second else suspendCancellableCoroutine<String>{continuation->
            val call=client.newCall(Request.Builder().url(url).header("User-Agent","JarvisAndroid/4.0 source reader").build());continuation.invokeOnCancellation{call.cancel()}
            call.enqueue(object:Callback{
                override fun onFailure(call:Call,e:java.io.IOException){if(continuation.isActive)continuation.resumeWithException(e)}
                override fun onResponse(call:Call,response:Response){val result=runCatching{response.use{check(it.isSuccessful){"Source unavailable (HTTP ${it.code}); no current facts were verified."};val body=it.body ?: error("Empty source");val type=it.header("Content-Type").orEmpty();require(type.startsWith("text/html") || type.startsWith("text/plain")){"This source is not a supported text page."};check(body.contentLength()<=500_000);val bytes=body.source().use{s->s.request(500001);check(s.buffer.size<=500000){"Source exceeds size limit"};s.buffer.readByteArray()};ResearchPolicy.readable(bytes.toString(body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)).also{check(it.isNotBlank()){"No readable source text"}}}};if(continuation.isActive)result.fold({continuation.resume(it)},{continuation.resumeWithException(it)})}
            })
        }.also{if(cache.size>=5)cache.clear();cache[url]=now to it}
        return AssistantReply(language.choose("Fetched public-source excerpt (untrusted reference text, not independently verified; publication date unknown):\n","Extracto de una fuente pública (no verificado independientemente; fecha de publicación desconocida):\n","بخشی از منبع عمومی؛ متن مرجع نامطمئن، بدون راستی‌آزمایی مستقل و با تاریخ انتشار نامشخص:\n")+text.take(3000)+"\nFetched: ${java.time.Instant.ofEpochMilli(prior?.first?.takeIf{now-it in 0..600_000} ?: now)}",listOf(SourceLink("Requested public source",url)),allowCloudSpeech=false)
    }
}
