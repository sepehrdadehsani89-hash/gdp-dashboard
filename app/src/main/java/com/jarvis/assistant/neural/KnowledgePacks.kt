package com.jarvis.assistant.neural

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.jarvis.assistant.core.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class KnowledgePack(val id:String,val title:String,val version:String,val publication:String,val bytes:Long,val sha256:String,val file:String)
object KnowledgePacks {
    private var catalog:List<KnowledgePack> = emptyList()
    @Synchronized fun catalog(context:Context):List<KnowledgePack>{if(catalog.isEmpty()){val a=JSONArray(context.assets.open("neural-pack-catalog.json").bufferedReader().use{it.readText()});catalog=(0 until a.length()).map{val j=a.getJSONObject(it);KnowledgePack(j.getString("id"),j.getString("title"),j.getString("version"),j.getString("publication"),j.getLong("bytes"),j.getString("sha256"),j.getString("file"))}};return catalog}
    fun parse(pack:KnowledgePack,bytes:ByteArray):JSONObject{
        require(bytes.size.toLong()==pack.bytes && bytes.size<=100_000){"Pack size mismatch"}
        val actual=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        require(actual==pack.sha256){"Knowledge pack integrity check failed"}
        val j=JSONObject(bytes.toString(Charsets.UTF_8))
        require(j.getString("license")=="CC0-1.0" && j.getString("id")==pack.id && j.getString("version")==pack.version && j.getString("publication")==pack.publication)
        val entries=j.getJSONArray("entries");require(entries.length() in 1..32)
        for(i in 0 until entries.length()){val e=entries.getJSONObject(i);require(e.getString("title").length in 1..160 && e.getString("text").length in 1..20_000)}
        return j
    }
    suspend fun downloadAndInstall(pack:KnowledgePack,index:KnowledgeIndex):Int{
        val url="https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/jarvis-next/downloads/neural-packs/${pack.file}"
        val http=OkHttpClient.Builder().followRedirects(false).connectTimeout(12,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).build()
        val bytes=suspendCancellableCoroutine<ByteArray>{continuation->
            val call=http.newCall(Request.Builder().url(url).build());continuation.invokeOnCancellation{call.cancel()}
            call.enqueue(object:Callback{
                override fun onFailure(call:Call,e:java.io.IOException){if(continuation.isActive)continuation.resumeWithException(e)}
                override fun onResponse(call:Call,response:Response){val result=runCatching{response.use{check(it.isSuccessful){"Pack unavailable (${it.code})"};val body=it.body ?: error("Empty pack");check(body.contentLength()<0 || body.contentLength()==pack.bytes);body.source().use{source->source.request(pack.bytes+1);check(source.buffer.size<=pack.bytes);source.buffer.readByteArray()}}};if(continuation.isActive)result.fold({continuation.resume(it)},{continuation.resumeWithException(it)})}
            })
        }
        val j=parse(pack,bytes);val entries=j.getJSONArray("entries")
        withContext(Dispatchers.IO){index.installPack(pack,(0 until entries.length()).map{entries.getJSONObject(it).let{e->e.getString("title") to e.getString("text")}})}
        return entries.length()
    }
}
@Composable fun KnowledgePackSection(busy:Boolean,onInstall:(KnowledgePack)->Unit){
    val context=LocalContext.current
    val settings by (context.applicationContext as com.jarvis.assistant.JarvisApplication).runtime.store.settings.collectAsState()
    val language=settings.language
    Column{
        Text(NeuralStrings.text(language,"Optional licensed knowledge packs"),style=MaterialTheme.typography.titleLarge)
        Text(NeuralStrings.text(language,"Small original CC0 starter notes, not an encyclopedia. Each download is pinned by SHA-256 in this signed app. Installation is optional; no model is loaded."))
        KnowledgePacks.catalog(context).forEach{pack->OutlinedButton(enabled=!busy,onClick={onInstall(pack)}){Text("Download ${pack.title} · ${pack.bytes} bytes · v${pack.version}")}}
    }
}
