package com.jarvis.assistant.titan

import com.jarvis.assistant.core.*
import com.jarvis.assistant.neural.*
import com.jarvis.assistant.omnisystem.FeatureVault
import org.json.*
import java.util.UUID

data class Routine(val id:String,val name:String,val trigger:String,val steps:List<String>,val enabled:Boolean)
object RoutinePolicy {
    fun step(text:String):ToolRequest {
        val t=AppMatching.normalize(text)
        return when(t){
            "time","what time is it","hora"->ToolRequest("time",emptyMap())
            "battery","battery percentage","bateria"->ToolRequest("battery",emptyMap())
            "my calendar today","my schedule today","mi calendario hoy"->ToolRequest("calendar",emptyMap())
            "my tasks","show tasks","mis tareas"->ToolRequest("tasks",emptyMap())
            else->{
                if(ForecastPolicy.request(text))ToolRequest("weather",mapOf("city" to (ForecastPolicy.city(text) ?: ""),"day" to ForecastPolicy.day(text).toString()))
                else AppMatching.commandTarget(text)?.let{ToolRequest("open_app",mapOf("name" to it))} ?: throw IllegalArgumentException("Use time, battery, weather, my calendar today, my tasks, or open an app.")
            }
        }
    }
    fun validate(r:Routine){
        require(r.id.length in 1..100 && r.name.length in 1..80 && r.trigger.length in 2..100 && r.trigger.none(Char::isISOControl))
        val trigger=AppMatching.normalize(r.trigger)
        require(trigger !in setOf("yes","no","stop","cancel","hey jarvis","oye jarvis","shut down all systems","unlock my phone","lock my phone"))
        require(r.steps.size in 1..8 && r.steps.all{it.length in 1..160 && it.none(Char::isISOControl)});r.steps.forEach(::step)
    }
}
/** User-defined text commands feed the same checked ToolRegistry/NeuralOrchestrator, never a second automation service. */
class Routines(private val vault:FeatureVault){
    @Synchronized fun all():List<Routine>{
        val raw=vault.read("titan_routines") ?: run{check(!vault.contains("titan_routines"));return emptyList()}
        val a=JSONArray(raw);require(a.length()<=12)
        return (0 until a.length()).map{i->val j=a.getJSONObject(i);val steps=j.getJSONArray("steps");Routine(j.getString("id"),j.getString("name"),j.getString("trigger"),(0 until steps.length()).map{steps.getString(it)},j.getBoolean("enabled")).also(RoutinePolicy::validate)}
    }
    @Synchronized fun save(id:String?,name:String,trigger:String,steps:List<String>,enabled:Boolean){
        val r=Routine(id ?: UUID.randomUUID().toString(),name.trim(),trigger.trim(),steps.map(String::trim),enabled);RoutinePolicy.validate(r)
        val old=all();require(old.size<12 || old.any{it.id==id});require(old.none{it.id!=id && AppMatching.normalize(it.trigger)==AppMatching.normalize(r.trigger)})
        persist(old.filterNot{it.id==id}+r)
    }
    @Synchronized fun remove(id:String)=persist(all().filterNot{it.id==id})
    private fun persist(routines:List<Routine>){vault.write("titan_routines",JSONArray().apply{routines.forEach{r->put(JSONObject().put("id",r.id).put("name",r.name).put("trigger",r.trigger).put("steps",JSONArray(r.steps)).put("enabled",r.enabled))}}.toString())}
    suspend fun route(input:String,language:AppLanguage,orchestrator:NeuralOrchestrator):AssistantReply? {
        val r=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){all().singleOrNull{it.enabled && AppMatching.normalize(it.trigger)==AppMatching.normalize(input)}} ?: return null
        val steps=r.steps.map(RoutinePolicy::step)
        val canonical=steps.mapIndexed{i,step->when(step.id){
            "time"->"what time is it";"battery"->"battery";"calendar"->"my calendar today";"tasks"->"show tasks"
            "open_app"->"open "+step.arguments.getValue("name");else->r.steps[i]
        }}.joinToString(" then ")
        return kotlinx.coroutines.withTimeout(180_000){orchestrator.executePlan(canonical,steps,language)}
    }
}
