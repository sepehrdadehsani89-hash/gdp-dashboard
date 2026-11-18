package com.jarvis.assistant.companion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Discovery is an untrusted hint. Only a saved certificate and signed session can establish trust. */
data class NearbyCompanion(val deviceId:String,val address:String,val known:Boolean)
object NearbyCompanionPolicy {
    const val TYPE="_jarvis-pc._tcp."
    fun candidate(host:String,port:Int,attributes:Map<String,ByteArray>,saved:List<DesktopPair>):NearbyCompanion?=runCatching{
        require(attributes["protocol"]?.toString(Charsets.US_ASCII)=="1")
        val device=attributes["device"]?.toString(Charsets.US_ASCII).orEmpty();require(CompanionPolicy.uuid(device))
        val address=CompanionPolicy.localUrl("https://$host:$port")
        NearbyCompanion(device,address,saved.any{it.deviceId==device})
    }.getOrNull()
    fun canUseSavedEndpoint(candidate:NearbyCompanion,pair:DesktopPair)=candidate.known && candidate.deviceId==pair.deviceId && candidate.address==pair.url
}

data class NearbyScan(val running:Boolean=false,val results:List<NearbyCompanion> = emptyList(),val message:String="Nearby discovery has not run.")
class NearbyCompanions(context:Context,private val saved:()->List<DesktopPair>){
    private val manager=context.getSystemService(NsdManager::class.java)
    private val main=Handler(Looper.getMainLooper())
    private val mutable=MutableStateFlow(NearbyScan());val state:StateFlow<NearbyScan> = mutable
    private var listener:NsdManager.DiscoveryListener?=null
    private var generation=0;private var resolves=0;private val queue=java.util.ArrayDeque<NsdServiceInfo>()
    fun start(){
        stop();val token=++generation;resolves=0;queue.clear()
        if(manager==null){mutable.value=NearbyScan(message="NSD_UNAVAILABLE · Use the private QR invitation.");return}
        mutable.value=NearbyScan(true,message="Discovering nearby companions for 15 seconds. Advertisements do not grant trust.")
        val discovery=object:NsdManager.DiscoveryListener{
            override fun onDiscoveryStarted(type:String){}
            override fun onDiscoveryStopped(type:String){}
            override fun onStartDiscoveryFailed(type:String,error:Int){main.post{if(token==generation){mutable.value=mutable.value.copy(running=false,message="NSD_START_FAILED_$error · QR/manual pairing remains available.");stop()}}}
            override fun onStopDiscoveryFailed(type:String,error:Int){}
            override fun onServiceLost(info:NsdServiceInfo){} // A lost multicast hint is not proof that a trusted session is offline.
            override fun onServiceFound(info:NsdServiceInfo){main.post{if(token==generation && info.serviceType.trimEnd('.')==NearbyCompanionPolicy.TYPE.trimEnd('.') && resolves+queue.size<20){queue.add(info);resolveNext(token)}}}
        }
        listener=discovery
        try{manager.discoverServices(NearbyCompanionPolicy.TYPE,NsdManager.PROTOCOL_DNS_SD,discovery);main.postDelayed({if(token==generation){stop();mutable.value=mutable.value.copy(message="Discovery window complete. Only authenticated connection checks establish connectivity; first pairing still requires a QR and both approvals.")}},15_000)}
        catch(_:Exception){stop();mutable.value=mutable.value.copy(message="NSD_UNAVAILABLE · Check local-network access or use QR/manual pairing.")}
    }
    private var resolving=false
    private fun resolveNext(token:Int){
        if(resolving || queue.isEmpty() || token!=generation)return
        resolving=true;val info=queue.removeFirst();resolves++
        try{manager?.resolveService(info,object:NsdManager.ResolveListener{
            override fun onResolveFailed(service:NsdServiceInfo,error:Int){main.post{if(token==generation){resolving=false;resolveNext(token)}}}
            override fun onServiceResolved(service:NsdServiceInfo){main.post{if(token==generation){
                val candidate=NearbyCompanionPolicy.candidate(service.host?.hostAddress.orEmpty(),service.port,service.attributes,runCatching(saved).getOrDefault(emptyList()))
                if(candidate!=null && mutable.value.results.none{it.deviceId==candidate.deviceId && it.address==candidate.address})mutable.value=mutable.value.copy(results=(mutable.value.results+candidate).take(20))
                resolving=false;resolveNext(token)
            }}}
        })}catch(_:Exception){resolving=false;resolveNext(token)}
    }
    fun stop(){generation++;queue.clear();resolving=false;listener?.let{runCatching{manager?.stopServiceDiscovery(it)}};listener=null;mutable.value=mutable.value.copy(running=false)}
}
