package com.jarvis.assistant.finishpolish.shared

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RegisteredAction(val id:String,val risk:Int,val target:String,val verification:String)
data class ActionStep(val id:String,val argument:String="",val targetDeviceId:String?=null,val targetSessionId:String?=null)
data class ActionPlan(val id:String=UUID.randomUUID().toString(),val steps:List<ActionStep>,val createdAt:Long,val recipeId:String?=null,val recipeRevision:Int?=null,val profileRevision:Long?=null){
    fun json()=JSONObject().put("id",id).put("created",createdAt).put("recipe",recipeId).put("recipe_revision",recipeRevision).put("profile_revision",profileRevision).put("steps",JSONArray().apply{steps.forEach{put(JSONObject().put("id",it.id).put("argument",it.argument).put("target",it.targetDeviceId).put("session",it.targetSessionId))}}).toString()
    companion object{fun parse(raw:String):ActionPlan{require(raw.length<=10000);val j=JSONObject(raw);val a=j.getJSONArray("steps");require(a.length() in 1..6);return ActionPlan(j.getString("id"),(0 until a.length()).map{i->a.getJSONObject(i).let{ActionStep(it.getString("id"),it.getString("argument"),it.optString("target").takeIf{t->t.isNotBlank()},it.optString("session").takeIf{t->t.isNotBlank()})}},j.getLong("created"),j.optString("recipe").takeIf{it.isNotBlank()},if(j.has("recipe_revision"))j.getInt("recipe_revision")else null,if(j.has("profile_revision"))j.getLong("profile_revision")else null)}}
}
object PolishActionRegistry {
    val entries=listOf(
        RegisteredAction("local.open_app",1,"Android","Observed foreground package after approved launch, otherwise DISPATCHED_UNVERIFIED"),
        RegisteredAction("desktop.open_chrome",1,"Paired computer","Authenticated loaded-document receipt"),
        RegisteredAction("desktop.search",1,"Paired computer","Authenticated navigation/status receipt"),
        RegisteredAction("local.battery_settings",1,"Android","Settings intent dispatched; user changes remain manual"),
        RegisteredAction("local.calendar_review",1,"Android","Calendar event intent dispatched; no edits"),
        RegisteredAction("doctor.balance",2,"App settings","Approved settings readback, not acoustic verification")
    )
    fun validate(plan:ActionPlan,now:Long,pairedId:String?,sessionId:String?=null){
        require(runCatching{UUID.fromString(plan.id).toString()==plan.id}.getOrDefault(false))
        require(now-plan.createdAt in 0..120_000 && plan.steps.size in 1..6){"PLAN_EXPIRED_OR_INVALID"}
        plan.steps.forEach{s->
            require(s.id in entries.map{it.id} && s.id!="doctor.balance"){"ACTION_NOT_RECIPE_ELIGIBLE"}
            require(s.argument.length<=300 && s.argument.none(Char::isISOControl))
            when(s.id){
                "local.open_app"->require(s.argument.matches(Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+")) && s.targetDeviceId==null)
                "desktop.open_chrome","desktop.search"->{require(s.targetDeviceId!=null && s.targetDeviceId==pairedId && s.targetSessionId!=null && s.targetSessionId==sessionId){"PAIRED_TARGET_CHANGED"};require(s.id!="desktop.search" || s.argument.isNotBlank())}
                "local.calendar_review"->require(s.argument.toLongOrNull()?.let{it>0}==true && s.targetDeviceId==null)
                "local.battery_settings"->require(s.argument.isEmpty() && s.targetDeviceId==null)
            }
        }
    }
}
data class ActionProgress(val action:String="none",val stage:String="IDLE",val reason:String="",val at:Long=0)
/** Metadata only, shared by the four engines; never stores commands or private arguments. */
class PolishActionJournal {
    private val mutable=MutableStateFlow(ActionProgress());val state=mutable.asStateFlow()
    @Synchronized fun record(action:String,stage:String,reason:String=""){
        require(action.matches(Regex("[a-z._]{1,60}")) && stage.matches(Regex("[A-Z_]{2,40}")))
        require(reason.length<=120 && reason.none(Char::isISOControl))
        mutable.value=ActionProgress(action,stage,reason,System.currentTimeMillis())
    }
}
