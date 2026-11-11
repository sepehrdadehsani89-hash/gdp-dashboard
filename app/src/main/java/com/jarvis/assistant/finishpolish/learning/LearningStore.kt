package com.jarvis.assistant.finishpolish.learning

import com.jarvis.assistant.omnisystem.FeatureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** One encrypted, additive document per explicitly selected local profile; no account guessing. */
class LearningStore(private val read:(String)->String?,private val write:(String,String)->Unit,profileId:String="local",private val now:()->Long=System::currentTimeMillis){
    constructor(vault:FeatureVault,profileId:String="local"):this({name->vault.read(name).also{check(it!=null || !vault.contains(name)){"Encrypted personal commands are unavailable; no data was overwritten."}}},vault::write,profileId)
    private val key=profileKey(profileId)
    private val mutable=MutableStateFlow(LearningProfile());val state=mutable.asStateFlow()
    private var undo=emptyList<LearningProfile>()
    init{reload()}
    @Synchronized fun reload():LearningProfile {
        val raw=read(key)
        if(raw==null){mutable.value=LearningProfile();undo=emptyList();return mutable.value}
        require(raw.toByteArray().size<=900_000)
        val j=JSONObject(raw);require(j.getInt("schema")==1);val profile=LearningPolicy.decode(j.getJSONObject("current"));val h=j.getJSONArray("undo");require(h.length()<=5)
        undo=(0 until h.length()).map{LearningPolicy.decode(h.getJSONObject(it))};mutable.value=profile;return profile
    }
    private fun persist(next:LearningProfile,history:List<LearningProfile>){
        LearningPolicy.validate(next)
        var retained=history.takeLast(5)
        fun encoded()=JSONObject().put("schema",1).put("current",LearningPolicy.encode(next)).put("undo",JSONArray().apply{retained.forEach{put(LearningPolicy.encode(it))}}).toString()
        var raw=encoded()
        while(raw.toByteArray().size>900_000 && retained.isNotEmpty()){retained=retained.drop(1);raw=encoded()}
        require(raw.toByteArray().size<=900_000);write(key,raw)
        check(read(key)==raw){"Learning storage readback failed; no saved result is claimed."}
        undo=retained;mutable.value=next
    }

    private fun edit(change:(LearningProfile,Int,Long)->LearningProfile):LearningProfile {
        val old=reload();require(old.revision<Int.MAX_VALUE-1)
        val revision=old.revision+1;val at=maxOf(now(),old.updatedAt+1,1)
        val next=change(old,revision,at).copy(revision=revision,updatedAt=at)
        persist(next,undo+old);return next
    }
    @Synchronized fun setEnabled(enabled:Boolean){edit{p,_,_->p.copy(enabled=enabled)}}
    @Synchronized fun match(text:String)=LearningPolicy.match(reload(),text)
    @Synchronized fun find(id:String)=reload().recipes.firstOrNull{it.id==id}
    @Synchronized fun save(title:String,aliases:List<String>,steps:List<LearnedStep>,editingId:String?=null,enabled:Boolean=true,approved:Boolean):LearnedRecipe {
        require(approved){"Review and explicitly approve this recipe before saving."}
        var result:LearnedRecipe?=null
        edit{p,revision,at->
            check(p.enabled){"Opt in to personal command learning first."}
            require(editingId==null || p.recipes.any{it.id==editingId})
            require(LearningPolicy.conflict(aliases,p.recipes,editingId)==null)
            require(p.recipes.size<LearningPolicy.MAX_RECIPES || editingId!=null)
            result=LearnedRecipe(editingId ?: UUID.randomUUID().toString(),title.trim(),aliases.map(String::trim),steps.toList(),enabled,revision,p.recipes.firstOrNull{it.id==editingId}?.approvedAt ?: at,at)
            p.copy(recipes=p.recipes.filterNot{it.id==editingId}+requireNotNull(result))
        };return requireNotNull(result)
    }
    @Synchronized fun delete(id:String){edit{p,_,_->require(p.recipes.any{it.id==id});p.copy(recipes=p.recipes.filterNot{it.id==id})}}
    @Synchronized fun addVocabulary(heard:String,meaning:String,approved:Boolean):ApprovedVocabulary {
        require(approved);var saved:ApprovedVocabulary?=null
        edit{p,_,at->check(p.enabled);val previous=p.vocabulary.firstOrNull{LearningPolicy.normalize(it.heard)==LearningPolicy.normalize(heard)};require(previous!=null || p.vocabulary.size<100)
            saved=ApprovedVocabulary(previous?.id ?: UUID.randomUUID().toString(),heard.trim(),meaning.trim(),previous?.approvedAt ?: at,at)
            p.copy(vocabulary=p.vocabulary.filterNot{it.id==previous?.id}+requireNotNull(saved))
        };return requireNotNull(saved)
    }
    @Synchronized fun deleteVocabulary(id:String){edit{p,_,_->p.copy(vocabulary=p.vocabulary.filterNot{it.id==id})}}
    @Synchronized fun undo():Boolean {
        val current=reload();require(current.revision<Int.MAX_VALUE-1);val previous=undo.lastOrNull() ?: return false;val at=maxOf(now(),current.updatedAt+1,1);val revision=current.revision+1
        val restored=previous.copy(revision=revision,updatedAt=at,recipes=previous.recipes.map{it.copy(revision=revision,updatedAt=at)})
        persist(restored,undo.dropLast(1));return true
    }
    @Synchronized fun clear(){val old=reload();require(old.revision<Int.MAX_VALUE-1);persist(LearningProfile(revision=old.revision+1,updatedAt=maxOf(now(),old.updatedAt+1,1)),emptyList())}
    @Synchronized fun exportApproved():String=LearningPolicy.encode(reload()).toString(2)
    /** Import is a reviewed replacement of this profile only, disabled until explicitly opted in. */
    @Synchronized fun previewImport(raw:String):LearningProfile {require(raw.toByteArray().size<=300_000);return LearningPolicy.decode(JSONObject(raw))}
    @Synchronized fun importApproved(raw:String,approved:Boolean){
        require(approved);val incoming=previewImport(raw)
        edit{_,revision,at->incoming.copy(enabled=false,revision=revision,updatedAt=at,recipes=incoming.recipes.map{it.copy(enabled=false,revision=revision,approvedAt=at,updatedAt=at)},vocabulary=incoming.vocabulary.map{it.copy(approvedAt=at,updatedAt=at)})}
    }
    companion object {
        fun profileKey(profileId:String):String {
            require(profileId.length in 1..100 && profileId.none(Char::isISOControl))
            val hash=MessageDigest.getInstance("SHA-256").digest(profileId.toByteArray()).take(16).joinToString(""){b->val n=b.toInt() and 255;"${('a'.code+(n ushr 4)).toChar()}${('a'.code+(n and 15)).toChar()}"}
            return "learning_profile_$hash"
        }
    }
}
