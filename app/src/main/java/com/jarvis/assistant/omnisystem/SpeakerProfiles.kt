package com.jarvis.assistant.omnisystem

import android.app.KeyguardManager
import android.content.Context
import android.os.StatFs
import com.jarvis.assistant.local.ModelDownloadPolicy
import com.jarvis.assistant.local.ModelFileTransfer
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

data class SpeakerProfile(val id:String,val name:String,val owner:Boolean,val vector:List<Float>,val samples:Int,val consentAt:Long,val modelSha:String)
data class SpeakerSuggestion(val profile:SpeakerProfile?,val similarity:Float?,val ambiguous:Boolean=false)
object SpeakerProfilePolicy {
    fun normalized(vector:FloatArray):FloatArray {
        require(vector.size in 16..1024 && vector.all(Float::isFinite))
        val norm=sqrt(vector.sumOf{it.toDouble()*it})
        require(norm>1e-10)
        return FloatArray(vector.size){(vector[it]/norm).toFloat()}
    }
    fun cosine(a:List<Float>,b:List<Float>):Float {
        require(a.size==b.size && a.size in 16..1024 && a.all(Float::isFinite) && b.all(Float::isFinite))
        val n=sqrt(a.sumOf{it.toDouble()*it}*b.sumOf{it.toDouble()*it});require(n>1e-10)
        return (a.indices.sumOf{a[it].toDouble()*b[it]}/n).toFloat().coerceIn(-1f,1f)
    }
    /** Conservative development thresholds, not calibrated probabilities or authentication. */
    fun suggest(query:FloatArray,profiles:List<SpeakerProfile>,threshold:Float=.72f,margin:Float=.08f):SpeakerSuggestion {
        require(profiles.size<=6 && threshold in .5f..1f && margin in 0f..1f)
        val unit=normalized(query).toList()
        val scores=profiles.filter{it.modelSha==SpeakerProfiles.SHA && it.vector.size==unit.size}.map{it to cosine(unit,it.vector)}.sortedByDescending{it.second}
        val first=scores.firstOrNull() ?: return SpeakerSuggestion(null,null)
        if(first.second<threshold)return SpeakerSuggestion(null,first.second)
        if(scores.size>1 && first.second-scores[1].second<margin)return SpeakerSuggestion(null,first.second,true)
        return SpeakerSuggestion(first.first,first.second)
    }
    fun parse(raw:String):List<SpeakerProfile>{
        val array=JSONArray(raw);require(array.length()<=6)
        val result=(0 until array.length()).map{i->val p=array.getJSONObject(i);val v=p.getJSONArray("vector")
            val profile=SpeakerProfile(p.getString("id"),p.getString("name"),p.getBoolean("owner"),(0 until v.length()).map{v.getDouble(it).toFloat()},p.getInt("samples"),p.getLong("consent_at"),p.getString("model_sha256"))
            require(profile.id.length in 1..100 && profile.name.length in 1..80 && profile.name.none(Char::isISOControl) && com.jarvis.assistant.neural.AppMatching.normalize(profile.name).isNotBlank() && profile.samples in 1..10 && profile.consentAt>0 && profile.modelSha.matches(Regex("[a-f0-9]{64}")))
            normalized(profile.vector.toFloatArray());profile
        }
        require(result.count{it.owner}<=1 && result.count{!it.owner}<=5 && result.map{it.id}.distinct().size==result.size)
        require(result.map{com.jarvis.assistant.neural.AppMatching.normalize(it.name)}.distinct().size==result.size)
        return result
    }
    fun encode(profiles:List<SpeakerProfile>)=JSONArray().apply{profiles.forEach{p->put(JSONObject().put("id",p.id).put("name",p.name).put("owner",p.owner).put("vector",JSONArray(p.vector)).put("samples",p.samples).put("consent_at",p.consentAt).put("model_sha256",p.modelSha))}}.toString()
}
class SpeakerProfiles(private val context:Context,private val vault:FeatureVault){
    companion object {
        const val FILE="wespeaker_en_voxceleb_resnet34_LM.onnx"
        const val URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recongition-models/$FILE"
        const val SHA="e9848563da86f263117134dfd7ad63c92355b37de492b55e325400c9d9c39012"
        const val SIZE=26_530_550L
        const val LICENSE="CC BY 4.0 · WeSpeaker VoxCeleb pretrained model; ONNX distribution by Sherpa-ONNX"
    }
    private val directory=File(context.filesDir,"speaker-profiles")
    private val model=File(directory,FILE)
    private val mutex=Mutex()
    @Volatile private var extractor:SpeakerEmbeddingExtractor?=null
    val isWarm get()=extractor!=null
    val focusEnabled:Boolean get()=options.getBoolean("segment_focus",false) && autoEnabled
    fun setFocus(enabled:Boolean){check(unlocked());if(enabled)check(autoEnabled);check(options.edit().putBoolean("segment_focus",enabled).commit())}
    private val cleanupScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var releaseJob:Job?=null
    private val options=context.getSharedPreferences("jarvis_speaker_options_v1",Context.MODE_PRIVATE)
    private var ownerVector:FloatArray?=null
    private var guestVectors=emptyList<FloatArray>()
    private val ownerVersion=java.util.concurrent.atomic.AtomicLong()
    val ownerRevision get()=ownerVersion.get()
    val ownerOnlyRequested get()=options.getBoolean("owner_only",false)
    val ownerThreshold get()=options.getFloat("owner_threshold",.90f).takeIf{it.isFinite()}?.coerceIn(.72f,.95f) ?: .90f
    fun setOwnerThreshold(value:Float){check(unlocked());require(value.isFinite() && value in 0.72f..0.95f);check(options.edit().putFloat("owner_threshold",value).commit());ownerVersion.incrementAndGet()}
    @Synchronized fun ownerSessionReady()=ownerOnlyRequested && ownerVector!=null && present()
    @Synchronized fun armOwnerSession(){
        check(unlocked())
        if(!ownerOnlyRequested)return
        val all=profiles();val owner=all.singleOrNull{it.owner} ?: error("Enroll the owner voice first.")
        check(present()){"Download the speaker recognition model first."}
        releaseJob?.cancel();clearOwnerVectors();ownerVector=owner.vector.toFloatArray();guestVectors=all.filterNot{it.owner}.map{it.vector.toFloatArray()};ownerVersion.incrementAndGet()
    }
    @Synchronized private fun clearOwnerVectors(){ownerVector?.fill(0f);ownerVector=null;guestVectors.forEach{it.fill(0f)};guestVectors=emptyList()}
    @Synchronized fun endOwnerSession(){clearOwnerVectors();ownerVersion.incrementAndGet()}
    @Synchronized fun setOwnerOnly(enabled:Boolean){
        check(unlocked());if(enabled)check(profiles().any{it.owner} && present()){"Enroll an owner and install the recognition model first."}
        check(options.edit().putBoolean("owner_only",enabled).commit())
        if(enabled)armOwnerSession()else endOwnerSession()
    }
    @Volatile private var knownCount=0
    @Volatile private var loadedProfiles=false
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
    fun present()=model.isFile && model.length()==SIZE
    val autoEnabled:Boolean get(){
        if(!options.getBoolean("auto",false) || !unlocked())return false
        if(!loadedProfiles)runCatching{profiles()}.onFailure{knownCount=0;loadedProfiles=true}
        return knownCount>0 && present()
    }
    @Synchronized fun setAuto(enabled:Boolean){
        check(unlocked());if(enabled)require(profiles().isNotEmpty() && present())
        check(options.edit().putBoolean("auto",enabled).commit())
    }
    fun resetOptions(){endOwnerSession();check(options.edit().clear().commit());knownCount=0;loadedProfiles=false}
    @Synchronized fun profiles():List<SpeakerProfile>{
        check(unlocked())
        val raw=vault.read("speaker_profiles") ?: run{check(!vault.contains("speaker_profiles")){"Encrypted profiles unavailable."};knownCount=0;loadedProfiles=true;return emptyList()}
        return SpeakerProfilePolicy.parse(raw).also{knownCount=it.size;loadedProfiles=true}
    }
    @Synchronized fun enroll(name:String,owner:Boolean,embedding:FloatArray,consent:Boolean,existingId:String?=null):SpeakerProfile {
        check(unlocked());require(consent){"Every enrolled person must consent."}
        val clean=name.trim();require(clean.length in 1..80 && clean.none(Char::isISOControl))
        val existing=profiles();val old=existing.firstOrNull{it.id==existingId}
        require(existingId==null || old!=null)
        require(existing.none{it.id!=existingId && com.jarvis.assistant.neural.AppMatching.normalize(it.name)==com.jarvis.assistant.neural.AppMatching.normalize(clean)})
        if(old==null)require(existing.size<6 && if(owner)existing.none{it.owner} else existing.count{!it.owner}<5){"Profile limit reached."}
        if(old!=null)require(old.modelSha==SHA && old.samples<10 && old.owner==owner)
        val unit=SpeakerProfilePolicy.normalized(embedding)
        val count=old?.samples ?: 0
        if(old!=null)require(old.vector.size==unit.size)
        val mean=unit.indices.map{i->((old?.vector?.get(i) ?: 0f)*count+unit[i])/(count+1)}
        val p=SpeakerProfile(old?.id ?: UUID.randomUUID().toString(),clean,owner,mean,count+1,old?.consentAt ?: System.currentTimeMillis(),SHA)
        val all=existing.filterNot{it.id==p.id}+p
        val raw=SpeakerProfilePolicy.encode(all);SpeakerProfilePolicy.parse(raw);vault.write("speaker_profiles",raw)
        knownCount=all.size;loadedProfiles=true
        if(ownerOnlyRequested)armOwnerSession()
        return p
    }
    @Synchronized fun delete(id:String){val remaining=profiles().filterNot{it.id==id};vault.write("speaker_profiles",SpeakerProfilePolicy.encode(remaining));knownCount=remaining.size;endOwnerSession();if(remaining.isEmpty())setAuto(false);if(ownerOnlyRequested && remaining.any{it.owner})armOwnerSession()}
    @Synchronized fun clearProfiles(){check(unlocked());endOwnerSession();vault.remove("speaker_profiles");knownCount=0;loadedProfiles=true;setAuto(false)}
    suspend fun download(onProgress:(Float)->Unit)=mutex.withLock{withContext(Dispatchers.IO){
        check(directory.exists() || directory.mkdirs());check(StatFs(directory.path).availableBytes>SIZE+20_000_000){"Free storage for the model first."}
        releaseJob?.cancel();extractor?.release();extractor=null
        val temporary=File(directory,"$FILE.part")
        val client=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).callTimeout(180,TimeUnit.SECONDS).followSslRedirects(false).build()
        val call=client.newCall(Request.Builder().url(URL).build())
        val watcher=CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default,start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{call.cancel()}}
        try {call.execute().use{response->
            require(response.isSuccessful && response.request.url.isHttps && response.request.url.host in setOf("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","github-releases.githubusercontent.com"))
            val body=response.body ?: error("Model response unavailable.")
            val descriptor=ModelDownloadPolicy.Descriptor("speaker-recongition-models",SHA,SIZE)
            body.byteStream().use{ModelFileTransfer.copyVerified(it,temporary,descriptor,0,onProgress)}
            currentCoroutineContext().ensureActive()
            check(temporary.renameTo(model)){"Could not install verified model."}
            onProgress(1f)
        }} finally{watcher.cancel();temporary.delete()}
    }}
    private fun verify(){
        check(present()){"Download the verified speaker model first."}
        val digest=MessageDigest.getInstance("SHA-256")
        model.inputStream().use{input->val b=ByteArray(131072);while(true){val n=input.read(b);if(n<0)break;digest.update(b,0,n)}}
        check(digest.digest().joinToString(""){"%02x".format(it)}==SHA){"Speaker model integrity check failed."}
    }
    private fun engine():SpeakerEmbeddingExtractor {
        if(extractor==null){verify();extractor=SpeakerEmbeddingExtractor(config=SpeakerEmbeddingExtractorConfig(model=model.absolutePath,numThreads=1,debug=false,provider="cpu"))}
        return extractor ?: error("Speaker engine unavailable.")
    }
    private fun releaseLater(){releaseJob?.cancel();if(ownerSessionReady())return;releaseJob=cleanupScope.launch{delay(60_000);release()}}
    suspend fun prepare()=mutex.withLock{withContext(Dispatchers.IO){check(unlocked());engine();releaseLater();Unit}}
    suspend fun embedding(audio:CapturedVoice):FloatArray=computeEmbedding(audio,false)
    private suspend fun computeEmbedding(audio:CapturedVoice,forOwner:Boolean):FloatArray=mutex.withLock{withContext(Dispatchers.IO){
        check(if(forOwner)ownerSessionReady()else unlocked());require(audio.sampleRate==16000 && audio.pcm.size in 32_000..800_000)
        releaseJob?.cancel()
        val engine=engine()
        val stream=engine.createStream();val samples=audio.floats()
        try {
            stream.acceptWaveform(samples,16000);stream.inputFinished();check(engine.isReady(stream)){"More clear speech is needed."}
            val vector=engine.compute(stream);require(vector.size==engine.dim());check(if(forOwner)ownerSessionReady()else unlocked())
            SpeakerProfilePolicy.normalized(vector)
        } finally {
            samples.fill(0f);stream.release()
            releaseLater()
        }
    }}
    suspend fun verifyOwner(audio:CapturedVoice):com.jarvis.assistant.voice.OwnerVoiceDecision {
        val revision=ownerRevision
        val snapshot=synchronized(this){ownerVector?.copyOf()?.let{it to guestVectors.map{v->v.copyOf()}}} ?: return com.jarvis.assistant.voice.OwnerVoiceDecision(com.jarvis.assistant.voice.OwnerVoiceResult.UNAVAILABLE)
        var query:FloatArray?=null
        return try{
            query=computeEmbedding(audio,true)
            if(revision!=ownerRevision || !ownerSessionReady())com.jarvis.assistant.voice.OwnerVoiceDecision(com.jarvis.assistant.voice.OwnerVoiceResult.UNAVAILABLE)
            else com.jarvis.assistant.voice.OwnerVoicePolicy.decide(query,snapshot.first,snapshot.second,ownerThreshold)
        }finally{query?.fill(0f);snapshot.first.fill(0f);snapshot.second.forEach{it.fill(0f)}}
    }
    suspend fun release()=mutex.withLock{withContext(Dispatchers.IO){extractor?.release();extractor=null}}
    fun releaseAsync(){if(ownerSessionReady() && com.jarvis.assistant.voice.WakeListenerService.state.value.running)return;cleanupScope.launch{release()}}
    suspend fun removeModel()=mutex.withLock{withContext(Dispatchers.IO){endOwnerSession();check(options.edit().putBoolean("auto",false).commit());releaseJob?.cancel();extractor?.release();extractor=null;check(!directory.exists() || directory.deleteRecursively());Unit}}
}
