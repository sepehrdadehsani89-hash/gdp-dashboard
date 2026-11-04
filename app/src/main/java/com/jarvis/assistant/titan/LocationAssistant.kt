package com.jarvis.assistant.titan

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.*
import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.omnisystem.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

enum class LocationGoal { CURRENT,ADDRESS,NAV_HOME,SAVE_HOME,SHOW_HOME,EDIT_HOME,DELETE_HOME,OPEN_MAPS,WALK,DRIVE,STOP_NAVIGATION }
object NavigationPolicy {
    val modes=listOf("driving","walking","bicycling","transit")
    fun home(destinations:List<SavedDestination>)=destinations.filter{AppMatching.normalize(it.name) in setOf("home","casa","hogar","mi casa")}.singleOrNull()
    fun parse(text:String):LocationGoal?=when(AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ").removePrefix("please ")){
        "where am i","what s my current location","whats my current location","my location","donde estoy","cual es mi ubicacion"->LocationGoal.CURRENT
        "tell me my address","what is my address","dime mi direccion"->LocationGoal.ADDRESS
        "take me home","navigate home","how do i get home","set my location to home","go home","llevame a casa","navega a casa","como llego a casa"->LocationGoal.NAV_HOME
        "save this place as home","save this location as home","set my home location","remember this place as home","guarda este lugar como casa"->LocationGoal.SAVE_HOME
        "change my home location","change home","cambia mi casa"->LocationGoal.EDIT_HOME
        "show my saved home","show home","muestra mi casa"->LocationGoal.SHOW_HOME
        "delete my home location","delete home","borra mi casa"->LocationGoal.DELETE_HOME
        "open google maps","open maps","abre google maps","abre mapas","start navigation","show me the route"->LocationGoal.OPEN_MAPS
        "make it walking","use walking","walking mode","a pie","usa a pie"->LocationGoal.WALK
        "use driving","make it driving","driving mode","en coche","usa coche"->LocationGoal.DRIVE
        "stop navigation","deten la navegacion"->LocationGoal.STOP_NAVIGATION
        else->null
    }
    fun url(destination:SavedDestination,mode:String):String {
        require(LocationMapPolicy.coordinates(destination.latitude,destination.longitude) && mode in modes)
        val point=String.format(Locale.ROOT,"%.6f,%.6f",destination.latitude,destination.longitude)
        return "https://www.google.com/maps/dir/?api=1&destination="+URLEncoder.encode(point,StandardCharsets.UTF_8.name())+"&travelmode=$mode&dir_action=navigate"
    }
}
object LocationRequests {
    private var request:Triple<String,LocationGoal,Long>?=null
    @Synchronized fun issue(goal:LocationGoal)=UUID.randomUUID().toString().also{request=Triple(it,goal,android.os.SystemClock.elapsedRealtime())}
    @Synchronized fun consume(id:String?):LocationGoal? {
        val r=request ?: return null
        if(r.first!=id)return null
        request=null
        return r.second.takeIf{android.os.SystemClock.elapsedRealtime()-r.third in 0..120_000}
    }
    @Synchronized fun clear(){request=null}
}
class NavigationPreferences(context:Context){
    private val p=context.getSharedPreferences("jarvis_titan_navigation_v1",Context.MODE_PRIVATE)
    var mode:String get()=p.getString("mode","driving").orEmpty().takeIf{it in NavigationPolicy.modes} ?: "driving";set(v){require(v in NavigationPolicy.modes);p.edit().putString("mode",v).apply()}
    fun clear(){p.edit().clear().apply()}
}
/** A two-minute, current-user-only settings conversation. No persistent background task or location history. */
class LocationAssistant(private val context:Context,private val locations:LocationIntelligence){
    private var pending:Pair<LocationGoal,Long>?=null
    private var destinationContext:Pair<String,Long>?=null
    private val settings=NavigationPreferences(context)
    fun cancel(){pending=null;destinationContext=null;LocationRequests.clear()}
    private fun permitted()=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    private fun enabled()=context.getSystemService(LocationManager::class.java).isLocationEnabled
    private fun open(goal:LocationGoal):String {
        val token=LocationRequests.issue(goal)
        return TitanLaunch.open(context,Intent(context,LocationHudActivity::class.java).putExtra("titan_request",token),"Location")
    }
    suspend fun route(input:String,lang:AppLanguage):AssistantReply? {
        if(context.getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked){cancel();return null}
        var goal=NavigationPolicy.parse(input)
        val active=pending?.takeIf{android.os.SystemClock.elapsedRealtime()-it.second in 0..120_000}
        val confirmation=LanguageRules.confirmation(input)
        if(active!=null && confirmation!=null){
            pending=null
            if(!confirmation)return AssistantReply(if(lang==AppLanguage.ES)"Entendido. Solicitud de ubicación cancelada." else "Understood. Location request cancelled.")
            return AssistantReply(open(active.first))
        }
        if(goal==null){if(active!=null)pending=null;return null}
        if(goal==LocationGoal.STOP_NAVIGATION)return AssistantReply(if(lang==AppLanguage.ES)"Detén la navegación en Google Maps. Jarvis no controla esa sesión." else "Stop navigation in Google Maps. Jarvis does not control that session.")
        if(goal==LocationGoal.WALK || goal==LocationGoal.DRIVE){
            val prior=destinationContext?.takeIf{android.os.SystemClock.elapsedRealtime()-it.second in 0..600_000}
                ?: return AssistantReply(if(lang==AppLanguage.ES)"Pide primero una ruta a Casa." else "Request a route to Home first.")
            val home=NavigationPolicy.home(locations.destinations())
            if(home?.id!=prior.first)return AssistantReply("Saved destination changed. Request Home again.")
            settings.mode=if(goal==LocationGoal.WALK)"walking" else "driving";goal=LocationGoal.NAV_HOME
        }
        if(goal in setOf(LocationGoal.SAVE_HOME,LocationGoal.EDIT_HOME,LocationGoal.DELETE_HOME,LocationGoal.SHOW_HOME))return AssistantReply(open(goal))
        if(goal==LocationGoal.OPEN_MAPS)return AssistantReply(TitanLaunch.open(context,Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps")),"Maps"))
        if(!enabled() || !permitted()){
            pending=goal to android.os.SystemClock.elapsedRealtime()
            return AssistantReply(if(lang==AppLanguage.ES){if(!enabled())"Los servicios de ubicación están apagados. ¿Quieres abrir los controles Android y continuar esta solicitud después de activarlos?" else "Jarvis necesita permiso de ubicación. ¿Quieres abrir su configuración para continuar?"}
                else if(!enabled())"Location services are turned off. Would you like to open Android location controls and continue this request after enabling them?" else "Jarvis needs location permission. Would you like to open its location setup and continue?")
        }
        if(goal==LocationGoal.NAV_HOME){
            val home=NavigationPolicy.home(locations.destinations()) ?: return AssistantReply(if(lang==AppLanguage.ES)"Casa no está guardada de forma única. Abre Ubicación y elige o guarda Casa; no inventé una dirección." else "Home is missing or ambiguous. Open Location to select or save Home; no address was guessed.")
            destinationContext=home.id to android.os.SystemClock.elapsedRealtime()
            return AssistantReply(TitanLaunch.open(context,Intent(Intent.ACTION_VIEW,Uri.parse(NavigationPolicy.url(home,settings.mode))),"Maps")+if(lang==AppLanguage.ES)" Solicitud de ruta enviada. Maps puede requerir pulsar Iniciar." else " Directions requested. Maps may require you to press Start.")
        }
        val fix=locations.current()
        val text=String.format(Locale.ROOT,"%.6f, %.6f · ±%.0f m",fix.latitude,fix.longitude,fix.accuracy)
        return AssistantReply(if(goal==LocationGoal.ADDRESS)locations.address(Locale.forLanguageTag(lang.tag))+"\n"+text else (if(lang==AppLanguage.ES)"Ubicación Android: " else "Android location: ")+text)
    }
}
