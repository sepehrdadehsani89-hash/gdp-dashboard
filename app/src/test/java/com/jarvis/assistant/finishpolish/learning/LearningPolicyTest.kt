package com.jarvis.assistant.finishpolish.learning

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LearningPolicyTest {
    private class Fixture(profile:String="local"){
        val values=mutableMapOf<String,String>();var writes=0;var allowWrites=true;var at=1000L
        val store=LearningStore({values[it]},{k,v->writes++;if(allowWrites)values[k]=v},profile){at++}
        fun recipe(alias:String="coding time",id:String?=null,active:Boolean=true)=store.save("My workspace",listOf(alias),listOf(LearnedStep(LearnedAction.LOCAL_APP_OPEN,"com.example.editor")),id,active,true)
    }
    private fun rejected(block:()->Unit){var rejected=false;try{block()}catch(_:IllegalArgumentException){rejected=true}catch(_:IllegalStateException){rejected=true};assertTrue("Input must be rejected",rejected)}
    @Test fun twentyExplicitAliasesBecomeExactBindingsWithoutTrainingOrAmbientWrites(){
        val f=Fixture();val aliases=(1..20).map{"coding phrase $it"}
        assertTrue(aliases.all{f.store.match(it)==null});f.store.setEnabled(true)
        val saved=aliases.map{f.recipe(it)};val writes=f.writes
        for((index,alias) in aliases.withIndex())assertEquals(saved[index].id,f.store.match("Hey Jarvis, ${alias.uppercase()}")?.id)
        assertEquals(writes,f.writes);assertNull(f.store.match("coding phrase 21"));assertNull(f.store.match("almost coding phrase 1"))
    }
    @Test fun optInAndExplicitApprovalAreRequiredBeforeSavingOrMatching(){
        val f=Fixture();rejected{f.recipe()};f.store.setEnabled(true)
        rejected{f.store.save("Workspace",listOf("coding time"),listOf(LearnedStep(LearnedAction.LOCAL_APP_OPEN,"com.example.editor")),approved=false)}
        f.recipe();assertNotNull(f.store.match("coding time"));f.store.setEnabled(false);assertNull(f.store.match("coding time"));assertEquals(1,f.store.reload().recipes.size)
    }
    @Test fun systemCommandsWakeWordsSensitiveRequestsAndNormalizedDuplicatesCannotBeHijacked(){
        for(alias in listOf("hey jarvis","Jarvis, are you up?","stop","what time is it","lock my phone","call me now","change to Spanish","open my banking app","send a message","system doctor","my password","run diagnostics","intelligence settings","yes","no","confirm","sí","two plus two","what is 7 plus 3","what s your name","what time zone is it"))assertNotNull(alias,LearningPolicy.conflict(listOf(alias),emptyList()))
        assertNotNull(LearningPolicy.conflict(listOf("Código favorito","codigo favorito"),emptyList()))
        assertNotNull(LearningPolicy.conflict(listOf("ready computer"),emptyList(),reservedAliases=listOf("Ready computer")))
    }
    @Test fun aliasCollisionIncludesDisabledRecipesButEditingItsOwnAliasesIsAllowed(){
        val f=Fixture();f.store.setEnabled(true);val first=f.recipe(active=false)
        rejected{f.recipe("Coding Time")};assertNull(LearningPolicy.conflict(listOf("coding time"),f.store.reload().recipes,first.id))
        f.recipe("writing time",first.id);assertNull(f.store.match("coding time"));assertNotNull(f.store.match("writing time"))
    }
    @Test fun approvedEditsAndUndoUseNewRevisionsSoOldExecutionPlansCannotResume(){
        val f=Fixture();f.store.setEnabled(true);val before=f.recipe();val edited=f.store.save("Changed",listOf("review time"),before.steps,before.id,true,true)
        assertTrue(edited.revision>before.revision);assertNull(f.store.match("coding time"));assertTrue(f.store.undo())
        val restored=f.store.match("coding time")!!;assertEquals(before.id,restored.id);assertTrue(restored.revision>edited.revision)
        f.store.delete(restored.id);assertNull(f.store.find(restored.id));assertTrue(f.store.undo());assertTrue(f.store.find(restored.id)!!.revision>restored.revision)
    }
    @Test fun clearRemovesRecipesVocabularyConsentAndUndoWithoutTouchingOtherVaultKeys(){
        val f=Fixture();f.values["desktop_companion"]="unrelated existing pairing";f.store.setEnabled(true);f.recipe();f.store.addVocabulary("calander","Calendar app",true);f.store.clear()
        assertFalse(f.store.state.value.enabled);assertTrue(f.store.state.value.recipes.isEmpty());assertTrue(f.store.state.value.vocabulary.isEmpty());assertFalse(f.store.undo());assertEquals("unrelated existing pairing",f.values["desktop_companion"])
    }
    @Test fun exportImportPreservesSupportedContentButImportedCommandsStayDisabledForReview(){
        val a=Fixture();a.store.setEnabled(true);a.recipe();a.store.addVocabulary("calander","Calendar app",true)
        val exported=a.store.exportApproved();assertFalse(exported.contains("undo"));assertFalse(exported.contains("token"))
        val b=Fixture();b.store.importApproved(exported,true);assertFalse(b.store.state.value.enabled);assertFalse(b.store.state.value.recipes.single().enabled);assertNull(b.store.match("coding time"));assertEquals("Calendar app",b.store.state.value.vocabulary.single().meaning)
        b.store.setEnabled(true);assertNull(b.store.match("coding time"))
        val imported=b.store.reload().recipes.single();b.store.save(imported.title,imported.aliases,imported.steps,imported.id,true,true);assertNotNull(b.store.match("coding time"))
    }
    @Test fun importNeedsExplicitApprovalAndRejectsUnsupportedExecutablesAndMalformedSchema(){
        val f=Fixture();f.store.setEnabled(true);f.recipe();val raw=f.store.exportApproved();val original=f.store.state.value
        rejected{f.store.importApproved(raw,false)}
        val shell=JSONObject(raw);shell.getJSONArray("recipes").getJSONObject(0).getJSONArray("steps").getJSONObject(0).put("action","REMOTE_SHELL")
        rejected{f.store.previewImport(shell.toString())}
        rejected{f.store.previewImport(JSONObject(raw).put("schema",999).toString())};assertEquals(original,f.store.reload())
    }
    @Test fun profileNamespacesPreventApprovedCommandsCrossingLocalAccountScopes(){
        val values=mutableMapOf<String,String>();fun store(id:String)=LearningStore({values[it]},{k,v->values[k]=v},id){2000L}
        val a=store("account-a");val b=store("account-b");a.setEnabled(true);a.save("Workspace",listOf("coding time"),listOf(LearnedStep(LearnedAction.LOCAL_APP_OPEN,"com.example.editor")),approved=true)
        assertNotNull(a.match("coding time"));assertNull(b.match("coding time"));assertTrue(b.reload().recipes.isEmpty());b.setEnabled(true);assertNull(b.match("coding time"));assertTrue(LearningStore.profileKey("account-a").matches(Regex("[a-z_]{1,60}")))
    }
    @Test fun failedStorageReadbackCannotClaimARecipeWasSaved(){
        val f=Fixture();f.store.setEnabled(true);f.allowWrites=false;rejected{f.recipe()};assertTrue(f.store.reload().recipes.isEmpty());assertNull(f.store.match("coding time"))
    }
    @Test fun inaccessibleEncryptedReadFailsClosedWithoutCallingAnyWriter(){
        var writes=0;rejected{LearningStore({throw IllegalStateException("Private data unavailable")},{_,_->writes++})};assertEquals(0,writes)
    }
    @Test fun serializedProfileRoundTripsWithoutCredentialOrHistoryFields(){
        val f=Fixture();f.store.setEnabled(true);val device="61e2be90-54a3-4d72-996b-8a6ab4036f90"
        f.store.save("Desktop study",listOf("study time","tiempo de estudio"),listOf(LearnedStep(LearnedAction.DESKTOP_CHROME,targetDeviceId=device),LearnedStep(LearnedAction.DESKTOP_SEARCH,"Kotlin tutorials",device)),approved=true)
        val encoded=LearningPolicy.encode(f.store.reload());assertEquals(f.store.reload(),LearningPolicy.decode(JSONObject(encoded.toString())))
        assertFalse(encoded.toString().contains("session_id"));assertFalse(encoded.toString().contains("certificate"));assertTrue(LearningPolicy.preview(f.store.reload().recipes.single()).contains("verified page receipt"))
    }
    @Test fun desktopRecipesRequireTheReviewedDeviceAndAppTargetsCannotContainShellOrProtectedPackages(){
        rejected{LearningPolicy.validateStep(LearnedStep(LearnedAction.DESKTOP_CHROME))}
        rejected{LearningPolicy.validateStep(LearnedStep(LearnedAction.DESKTOP_SEARCH,"query","not-a-device"))}
        rejected{LearningPolicy.validateStep(LearnedStep(LearnedAction.LOCAL_APP_OPEN,"com.example.editor; rm -rf"))}
        rejected{LearningPolicy.validateStep(LearnedStep(LearnedAction.LOCAL_APP_OPEN,"com.android.settings"))}
        rejected{LearningPolicy.validateStep(LearnedStep(LearnedAction.LOCAL_APP_OPEN,"com.example.wallet"))}
    }
    @Test fun teachingSpeechCreatesOnlyAnEditableDraftAndDoesNotGuessUnsupportedTargets(){
        val f=Fixture();val draft=LearningPolicy.parseTeachingDraft("Jarvis, when I say coding time, open Chrome on my computer")!!
        assertEquals("coding time",draft.alias);assertEquals("open Chrome on my computer",draft.request);assertEquals(0,f.writes)
        val spoken=LearningPolicy.parseTeachingDraft("when i say coding time open chrome on my computer")!!
        assertEquals("coding time",spoken.alias);assertEquals("open chrome on my computer",spoken.request)
        val spanish=LearningPolicy.parseTeachingDraft("cuando diga hora de programar abre chrome en mi computadora")!!
        assertEquals("hora de programar",spanish.alias);assertEquals("abre chrome en mi computadora",spanish.request);assertEquals(0,f.writes)
        assertNull(LearningPolicy.parseTeachingDraft("when I say stop, open Chrome"));assertNull(LearningPolicy.parseTeachingDraft("when i say stop open chrome"));assertNull(LearningPolicy.parseTeachingDraft("coding time means something"));assertNull(LearningPolicy.parseTeachingDraft("when i say coding time send my private files"))
    }
    @Test fun vocabularyNotesRequireApprovalAndNeverRewriteThemselvesIntoExecutableCommands(){
        val f=Fixture();f.store.setEnabled(true);rejected{f.store.addVocabulary("private password","value",true)};rejected{f.store.addVocabulary("calander","Calendar app",false)}
        val v=f.store.addVocabulary("calander","Calendar app",true);assertEquals("USER_APPROVED",v.source);assertNull(f.store.match("calander"));f.store.deleteVocabulary(v.id);assertTrue(f.store.reload().vocabulary.isEmpty())
    }
    @Test fun historyIsBoundedAndMalformedProfilesCannotCreateDuplicateRecipeOrVocabularyIds(){
        val f=Fixture();f.store.setEnabled(true);val r=f.recipe();repeat(8){f.recipe("coding variant $it",r.id)}
        val document=JSONObject(f.values[LearningStore.profileKey("local")]!!);assertTrue(document.getJSONArray("undo").length()<=5)
        val profile=LearningPolicy.encode(f.store.reload());profile.getJSONArray("recipes").put(profile.getJSONArray("recipes").getJSONObject(0));rejected{LearningPolicy.decode(profile)}
    }
}
