package com.jarvis.assistant.companion

import com.jarvis.assistant.omnisystem.FeatureVault
import java.security.cert.X509Certificate
import java.net.Proxy
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class CompanionProgress { SENT, RUNNING }
data class CompanionExecution(val state:String,val detail:String,val verified:Boolean,val errorCode:String?=null)

class CompanionClient(private val vault:FeatureVault){
    fun paired():DesktopPair? {
        val raw=vault.read("desktop_companion")
        if(raw==null){if(vault.contains("desktop_companion"))throw CompanionFailure("PRIVATE_PAIRING_UNAVAILABLE");return null}
        return DesktopPair.parse(raw)
    }
    fun forget(){vault.remove("desktop_companion")}
    private fun transport(url:String,pin:String,onSent:()->Unit={}):OkHttpClient {
        CompanionPolicy.localUrl(url);require(pin.matches(Regex("[a-f0-9]{64}")))
        val trust=object:X509TrustManager {
            override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain:Array<X509Certificate>,authType:String){throw java.security.cert.CertificateException("Client certificates unsupported")}
            override fun checkServerTrusted(chain:Array<X509Certificate>,authType:String){
                if(chain.size!=1 || CompanionPolicy.sha(chain[0].encoded)!=pin)throw java.security.cert.CertificateException("Companion certificate mismatch")
                chain[0].checkValidity()
            }
        }
        val tls=SSLContext.getInstance("TLS").apply{init(null,arrayOf(trust),null)}
        // This client is confined to the user's explicitly paired private IPv4 endpoint.
        // Keep OkHttp hostname/SAN verification; no global TLS or trust changes.
        return OkHttpClient.Builder().eventListener(object:EventListener(){override fun requestBodyEnd(call:Call,byteCount:Long){onSent()}}).sslSocketFactory(tls.socketFactory,trust).proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).connectTimeout(5,java.util.concurrent.TimeUnit.SECONDS).readTimeout(20,java.util.concurrent.TimeUnit.SECONDS).callTimeout(24,java.util.concurrent.TimeUnit.SECONDS).build()
    }
    private suspend fun request(url:String,pin:String,path:String,body:JSONObject?,token:String?=null,onSent:()->Unit={}):JSONObject=suspendCancellableCoroutine{c->
        val builder=Request.Builder().url(url+path)
        if(token!=null)builder.header("Authorization","Bearer $token")
        if(body!=null)builder.post(body.toString().toRequestBody("application/json".toMediaType()))
        val call=transport(url,pin){if(c.isActive)onSent()}.newCall(builder.build());c.invokeOnCancellation{call.cancel()}
        call.enqueue(object:Callback{
            override fun onFailure(call:Call,e:java.io.IOException){if(c.isActive)c.resumeWithException(CompanionFailure(when(e){is javax.net.ssl.SSLException->"TLS_PIN_OR_CERTIFICATE_ERROR";is java.net.SocketTimeoutException->"DESKTOP_TIMEOUT_STATUS_UNKNOWN";else->"DESKTOP_CONNECTION_UNAVAILABLE"}))}
            override fun onResponse(call:Call,response:Response){
                try{response.use{
                    val source=it.body?.byteStream() ?: throw CompanionFailure("EMPTY_RECEIPT")
                    val bytes=source.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);while(true){val n=input.read(buffer);if(n<0)break;if(out.size()+n>32768)throw CompanionFailure("RECEIPT_TOO_LARGE");out.write(buffer,0,n)};out.toByteArray()}
                    val j=JSONObject(bytes.toString(Charsets.UTF_8));bytes.fill(0)
                    if(!it.isSuccessful)throw CompanionFailure(j.optString("error").takeIf{s->s.matches(Regex("[A-Z_]{3,60}"))} ?: "DESKTOP_HTTP_${it.code}")
                    if(c.isActive)c.resume(j)
                }}catch(e:Exception){if(c.isActive)c.resumeWithException(if(e is CompanionFailure)e else CompanionFailure("INVALID_DESKTOP_RESPONSE"))}
            }
        })
    }
    suspend fun requestPair(i:DesktopInvitation,clientId:String,nonce:String):JSONObject {
        require(System.currentTimeMillis()<i.expires)
        val proof=CompanionPolicy.mac(i.code.toByteArray(),"${i.pairingId}\n$clientId\n$nonce".toByteArray())
        return request(i.url,i.pin,"/v1/pair/request",JSONObject().put("pairing_id",i.pairingId).put("client_id",clientId).put("nonce",nonce).put("proof",proof))
    }
    suspend fun finishPair(i:DesktopInvitation,ticket:String,clientId:String,nonce:String):DesktopPair {
        require(System.currentTimeMillis()<i.expires && CompanionPolicy.uuid(ticket))
        val proof=CompanionPolicy.mac(i.code.toByteArray(),"${i.pairingId}\n$clientId\n$nonce".toByteArray())
        val j=request(i.url,i.pin,"/v1/pair/complete",JSONObject().put("ticket",ticket).put("client_id",clientId).put("nonce",nonce).put("proof",proof))
        require(j.getInt("schema_version")==1 && j.getString("device_id")==i.deviceId && j.getString("client_id")==clientId)
        val p=DesktopPair.parse(DesktopPair(i.url,i.pin,i.deviceId,clientId,j.getString("session_id"),j.getString("token")).json())
        withContext(Dispatchers.IO){vault.write("desktop_companion",p.json())};return p
    }
    suspend fun health():JSONObject {val p=paired() ?: throw CompanionFailure("NO_PAIRED_COMPUTER");return request(p.url,p.pin,"/v1/health",null).also{require(it.getInt("schema_version")==1 && it.getString("device_id")==p.deviceId && it.getString("service")=="jarvis-companion")}}
    suspend fun revoke(){val p=paired() ?: return;request(p.url,p.pin,"/v1/revoke",JSONObject(),p.token);withContext(Dispatchers.IO){forget()}}
    suspend fun capabilities():com.jarvis.assistant.finishpolish.mission.DesktopReadiness {
        val p=withContext(Dispatchers.IO){paired()} ?: throw CompanionFailure("NO_PAIRED_COMPUTER")
        val nonce=UUID.randomUUID().toString()
        val response=request(p.url,p.pin,"/v1/capabilities",JSONObject().put("nonce",nonce),p.token)
        return try{com.jarvis.assistant.finishpolish.mission.MissionPolicy.parseManifest(response,p,nonce,System.currentTimeMillis())}catch(_:Exception){throw CompanionFailure("CAPABILITY_VERIFICATION_FAILED")}
    }
    suspend fun execute(r:DesktopRequest):String=executeObserved(r).detail
    /** SENT follows actual HTTP request-body transmission; RUNNING follows an authenticated
     * receipt endpoint's STILL_PROCESSING response. Neither implies verified completion. */
    suspend fun executeObserved(r:DesktopRequest,expectedDeviceId:String?=null,expectedSessionId:String?=null,onProgress:(CompanionProgress)->Unit={}):CompanionExecution=coroutineScope {
        val p=withContext(Dispatchers.IO){paired()} ?: throw CompanionFailure("NO_PAIRED_COMPUTER")
        if(expectedDeviceId!=null && (p.deviceId!=expectedDeviceId || expectedSessionId!=null && p.sessionId!=expectedSessionId))throw CompanionFailure("PAIRED_TARGET_CHANGED_REVIEW_AGAIN")
        val id=UUID.randomUUID().toString();val command=CompanionPolicy.command(p,r,id,System.currentTimeMillis());val hash=CompanionPolicy.sha(command.toString().toByteArray())
        try {
            val operation=async{request(p.url,p.pin,"/v1/commands",command,p.token){onProgress(CompanionProgress.SENT)}}
            val observer=launch{
                delay(400)
                var runningReported=false
                while(isActive && !operation.isCompleted){
                    try{request(p.url,p.pin,"/v1/receipts/$id",null,p.token)}
                    catch(e:CompanionFailure){if(e.code=="STILL_PROCESSING" && !runningReported){runningReported=true;onProgress(CompanionProgress.RUNNING)}}
                    delay(750)
                }
            }
            val envelope=try{operation.await()}finally{observer.cancelAndJoin()}
            val receipt=try{CompanionPolicy.verify(envelope,p,command,hash,System.currentTimeMillis())}catch(_:Exception){throw CompanionFailure("RECEIPT_VERIFICATION_FAILED")}
            when(receipt.getString("state")){
                "COMPLETED"->CompanionExecution("COMPLETED",if(r.query==null)"Chrome is open in your paired computer's Jarvis browser window. The loaded page was verified." else "The requested search is open in Chrome on your paired computer. The loaded page was verified.",true)
                "PROCESSING"->CompanionExecution("PROCESSING","Your paired computer is still processing. No completed result is claimed. Check the desktop before trying again.",false,"STILL_PROCESSING")
                else->{val error=receipt.optString("error_category").takeIf{it.matches(Regex("[A-Z_]{3,60}"))} ?: "DESKTOP_EXECUTION_FAILED";CompanionExecution(receipt.getString("state"),"Your paired computer reported $error. No verified completion is claimed.",false,error)}
            }
        }catch(e:CancellationException){withContext(NonCancellable){withTimeoutOrNull(2000){runCatching{request(p.url,p.pin,"/v1/cancel/$id",JSONObject(),p.token)}}};throw e}
    }
}
