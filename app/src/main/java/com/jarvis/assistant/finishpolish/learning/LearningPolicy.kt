package com.jarvis.assistant.finishpolish.learning

import com.jarvis.assistant.neural.AppMatching
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class LearnedAction { LOCAL_APP_OPEN, DESKTOP_CHROME, DESKTOP_SEARCH }
data class LearnedStep(val action:LearnedAction,val argument:String="",val targetDeviceId:String?=null)
data class LearnedRecipe(val id:String,val title:String,val aliases:List<String>,val steps:List<LearnedStep>,val enabled:Boolean,val revision:Int,val approvedAt:Long,val updatedAt:Long)
data class ApprovedVocabulary(val id:String,val heard:String,val meaning:String,val approvedAt:Long,val updatedAt:Long,val source:String="USER_APPROVED")
data class LearningProfile(val enabled:Boolean=false,val recipes:List<LearnedRecipe> = emptyList(),val vocabulary:List<ApprovedVocabulary> = emptyList(),val revision:Int=0,val updatedAt:Long=0)
data class TeachingDraft(val alias:String,val request:String)

/** Explicit exact patterns only. No ambient learning, generated executable code or guessed identity. */
object LearningPolicy {
    const val SCHEMA=1
    const val MAX_RECIPES=50
    const val MAX_STEPS=6
    fun normalize(text:String)=AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ")
    fun uuid(value:String)=runCatching{UUID.fromString(value).toString()==value}.getOrDefault(false)
    fun publicText(value:String,max:Int)=value.length in 1..max && value.none(Char::isISOControl)
    fun reserved(alias:String):Boolean {
        val n=normalize(alias)
        if(n.length !in 3..80 || n.split(' ').size>12)return true
        if(com.jarvis.assistant.core.AppLanguage.supported.any{com.jarvis.assistant.voice.WakePhraseMatcher.extractCommand(alias,it)!=null})return true
        if(com.jarvis.assistant.core.LanguageRules.confirmation(n)!=null || com.jarvis.assistant.rebirth.ConversationCompletionPolicy.matches(n) || com.jarvis.assistant.voice.LocalArithmetic.request(n)!=null || com.jarvis.assistant.core.LanguageRules.isStop(n) || com.jarvis.assistant.core.LanguageRules.languageRequest(n)!=null || com.jarvis.assistant.voice.LocalVoiceQueries.readOnly(n) || com.jarvis.assistant.call.AiCallPolicy.command(n)!=null)return true
        if(n in setOf("hey jarvis","jarvis","oye jarvis","system doctor","run diagnostics","teach jarvis","intelligence settings","mission control","your next action","why didn t you hear me","why didn t you understand my last command"))return true
        // Prevent personal phrases from intercepting existing action/question/credential routes.
        return Regex("^(?:please |por favor )?(?:open|launch|start|stop|call|answer|cancel|end|unlock|lock|send|draft|delete|erase|transfer|pay|buy|tap|type|turn|set|remind|show|search|what|why|how|who|where|when|tell|explain|can you|could you|abre|abrir|inicia|para|llama|envia|borra|bloquea|desbloquea|que|como|cuando|donde|dime|busca)\\b").containsMatchIn(n) || Regex("\\b(?:password|passcode|secret|api key|token|contrasena)\\b").containsMatchIn(n)
    }
    fun conflict(aliases:List<String>,recipes:List<LearnedRecipe>,editingId:String?=null,reservedAliases:Collection<String> = emptyList()):String? {
        if(aliases.isEmpty() || aliases.size>10)return "Choose one to ten trigger phrases."
        if(aliases.any{!publicText(it,80) || reserved(it)})return "A trigger conflicts with an existing/system command or is invalid. Choose a distinct phrase such as coding time."
        val normalized=aliases.map(::normalize)
        if(reservedAliases.filter{it.isNotBlank()}.map(::normalize).any{it in normalized})return "A trigger conflicts with your optional wake phrase. Choose a distinct phrase."
        if(normalized.distinct().size!=normalized.size)return "Two trigger phrases normalize to the same words."
        if(recipes.filterNot{it.id==editingId}.flatMap{it.aliases}.map(::normalize).any{it in normalized})return "A trigger already belongs to another recipe, including a disabled recipe. Edit that recipe or choose another phrase."
        return null
    }
    fun validateStep(step:LearnedStep){
        when(step.action){
            LearnedAction.LOCAL_APP_OPEN->{require(step.argument.matches(Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+")));require(step.targetDeviceId==null);require(!com.jarvis.assistant.tools.ToolPolicy.protectedPackage(step.argument))}
            LearnedAction.DESKTOP_CHROME->{require(step.argument.isEmpty());require(uuid(step.targetDeviceId.orEmpty()))}
            LearnedAction.DESKTOP_SEARCH->{require(publicText(step.argument,300));require(step.argument.toByteArray().size<=1000);require(uuid(step.targetDeviceId.orEmpty()))}
        }
    }
    fun validate(profile:LearningProfile){
        require(profile.revision>=0 && profile.updatedAt>=0 && profile.recipes.size<=MAX_RECIPES && profile.vocabulary.size<=100)
        require(profile.recipes.map{it.id}.distinct().size==profile.recipes.size)
        for(r in profile.recipes){
            require(uuid(r.id) && publicText(r.title,80) && r.revision>0 && r.approvedAt>0 && r.updatedAt>=r.approvedAt)
            require(conflict(r.aliases,profile.recipes,r.id)==null);require(r.steps.size in 1..MAX_STEPS);r.steps.forEach(::validateStep)
        }
        require(profile.vocabulary.map{it.id}.distinct().size==profile.vocabulary.size);require(profile.vocabulary.map{normalize(it.heard)}.distinct().size==profile.vocabulary.size)
        for(v in profile.vocabulary){require(normalize(v.heard).length in 2..80 && normalize(v.meaning).isNotBlank());require(uuid(v.id) && publicText(v.heard,80) && publicText(v.meaning,160) && v.approvedAt>0 && v.updatedAt>=v.approvedAt && v.source=="USER_APPROVED");require(!Regex("\\b(?:password|passcode|secret|api key|token|contrasena)\\b").containsMatchIn(normalize(v.heard+" "+v.meaning)))}
    }
    fun match(profile:LearningProfile,text:String):LearnedRecipe? {
        if(!profile.enabled)return null
        val n=normalize(text)
        return profile.recipes.singleOrNull{r->r.enabled && r.aliases.any{normalize(it)==n}}
    }
    fun parseTeachingDraft(text:String):TeachingDraft? {
        val clean=text.trim().take(2000).replace(Regex("^(?:(?:hey |oye )?jarvis[, ]+)" ,RegexOption.IGNORE_CASE),"")
        val punctuated=Regex("^(?:when i say|cuando diga) (.{3,80}?)[,;:]\\s*(.{3,1200})$",RegexOption.IGNORE_CASE).matchEntire(clean)
        // Offline ASR normally omits punctuation. A first explicit supported action verb
        // can delimit a review draft; it never authorizes, saves or executes that prose.
        val spoken=if(punctuated==null)Regex("^(?:when i say|cuando diga) (.{3,80}?)\\s+((?:open|launch|start|abre|abrir|inicia)\\s+.{2,1190})$",RegexOption.IGNORE_CASE).matchEntire(clean) else null
        val match=punctuated ?: spoken ?: return null
        val alias=match.groupValues[1].trim().trim('"','“','”',',',';',':');val request=match.groupValues[2].trim()
        if(reserved(alias))return null
        return TeachingDraft(alias,request)
    }
    fun preview(recipe:LearnedRecipe)=recipe.steps.mapIndexed{i,s->"${i+1}. "+when(s.action){LearnedAction.LOCAL_APP_OPEN->"Open Android app ${s.argument}; dispatch is not proof of foreground visibility.";LearnedAction.DESKTOP_CHROME->"Open Chrome on paired device ${s.targetDeviceId}; require verified page receipt.";LearnedAction.DESKTOP_SEARCH->"Search Chrome on paired device ${s.targetDeviceId} for ${s.argument}; require verified page receipt."}}.joinToString("\n")
    fun encode(profile:LearningProfile):JSONObject {
        validate(profile)
        return JSONObject().put("schema",SCHEMA).put("enabled",profile.enabled).put("revision",profile.revision).put("updated_at",profile.updatedAt).put("recipes",JSONArray().apply{profile.recipes.forEach{r->put(JSONObject().put("id",r.id).put("title",r.title).put("aliases",JSONArray(r.aliases)).put("enabled",r.enabled).put("revision",r.revision).put("approved_at",r.approvedAt).put("updated_at",r.updatedAt).put("source","USER_APPROVED").put("steps",JSONArray().apply{r.steps.forEach{s->put(JSONObject().put("action",s.action.name).put("argument",s.argument).put("device",s.targetDeviceId ?: JSONObject.NULL))}}))}}).put("vocabulary",JSONArray().apply{profile.vocabulary.forEach{v->put(JSONObject().put("id",v.id).put("heard",v.heard).put("meaning",v.meaning).put("approved_at",v.approvedAt).put("updated_at",v.updatedAt).put("source",v.source))}})
    }
    fun decode(json:JSONObject):LearningProfile {
        require(json.getInt("schema")==SCHEMA)
        val recipes=json.getJSONArray("recipes");require(recipes.length()<=MAX_RECIPES)
        val vocabulary=json.getJSONArray("vocabulary");require(vocabulary.length()<=100)
        return LearningProfile(json.getBoolean("enabled"),(0 until recipes.length()).map{i->val r=recipes.getJSONObject(i);require(r.getString("source")=="USER_APPROVED");val a=r.getJSONArray("aliases");val steps=r.getJSONArray("steps");require(a.length() in 1..10 && steps.length() in 1..MAX_STEPS)
            LearnedRecipe(r.getString("id"),r.getString("title"),(0 until a.length()).map(a::getString),(0 until steps.length()).map{k->val s=steps.getJSONObject(k);LearnedStep(LearnedAction.valueOf(s.getString("action")),s.getString("argument"),if(s.isNull("device"))null else s.getString("device"))},r.getBoolean("enabled"),r.getInt("revision"),r.getLong("approved_at"),r.getLong("updated_at"))
        },(0 until vocabulary.length()).map{i->val v=vocabulary.getJSONObject(i);ApprovedVocabulary(v.getString("id"),v.getString("heard"),v.getString("meaning"),v.getLong("approved_at"),v.getLong("updated_at"),v.getString("source"))},json.getInt("revision"),json.getLong("updated_at")).also(::validate)
    }
}
