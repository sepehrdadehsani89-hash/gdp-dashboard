package com.jarvis.assistant.finishpolish.learning

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.companion.CompanionPolicy
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.neural.DiscoveredApp

/** Existing private Settings surface: review, approve and persist; no ambient recording. */
class LearningActivity:PrivateModuleActivity(){
    private var title by mutableStateOf("")
    private var aliases by mutableStateOf("")
    private var appQuery by mutableStateOf("")
    private var search by mutableStateOf("")
    private var editing by mutableStateOf<String?>(null)
    private var enabled by mutableStateOf(true)
    private val steps=mutableStateListOf<LearnedStep>()
    private var apps by mutableStateOf<List<DiscoveredApp>>(emptyList())
    private var pairedDevice by mutableStateOf<String?>(null)
    private var review by mutableStateOf(false)
    private var approved by mutableStateOf(false)
    private var vocabHeard by mutableStateOf("")
    private var vocabMeaning by mutableStateOf("")
    private var pendingExport:String?=null
    private var pendingImport by mutableStateOf<String?>(null)
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        val raw=pendingExport;pendingExport=null
        if(uri!=null && raw!=null)task{contentResolver.openOutputStream(uri,"wt")?.use{it.write(raw.toByteArray())} ?: error("Destination unavailable");status="Approved personal commands exported to the file you selected. Treat that file as private."}
    }
    private val import=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)task{
            val bytes=contentResolver.openInputStream(uri)?.use{input->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096)
                while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=300_000);out.write(buffer,0,n)}
                out.toByteArray()
            } ?: error("File unavailable")
            try{require(bytes.size<=300_000);val raw=bytes.toString(Charsets.UTF_8);val incoming=runtime.learning.previewImport(raw);pendingImport=raw;status="Import preview: ${incoming.recipes.size} recipes, ${incoming.vocabulary.size} vocabulary notes. Nothing imported yet."}finally{bytes.fill(0)}
        }
    }
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        apps=runtime.appRegistry.apps();pairedDevice=runCatching{runtime.companion.paired()?.deviceId}.getOrNull()
        val learning=try{runtime.learning.also{it.reload()}}catch(_:Exception){
            screen("TEACH J.A.R.V.I.S."){Text("Encrypted learning data unavailable. No profile was reset or overwritten. Restore its private backup or review storage diagnostics before retrying.")};return
        }
        val request=intent.getStringExtra("request").orEmpty()
        LearningPolicy.parseTeachingDraft(request)?.let{draft->
            aliases=draft.alias;title=draft.alias
            val desktop=CompanionPolicy.parse(draft.request)
            val pair=pairedDevice
            if(desktop!=null && pair!=null)steps+=LearnedStep(if(desktop.query==null)LearnedAction.DESKTOP_CHROME else LearnedAction.DESKTOP_SEARCH,desktop.query.orEmpty(),pair)
            else AppMatching.commandTarget(draft.request)?.let{target->apps.filter{AppMatching.normalize(it.label)==AppMatching.normalize(target) || it.aliases.any{a->AppMatching.normalize(a)==AppMatching.normalize(target)}}.singleOrNull()?.let{steps+=LearnedStep(LearnedAction.LOCAL_APP_OPEN,it.packageId)}}
            status="Teaching draft only. Review the target and every step, opt in, then explicitly Save. Unsupported multi-app/file requests require a supported plan; nothing was executed."
        }
        screen("TEACH J.A.R.V.I.S. · APPROVED COMMANDS"){
            val profile by learning.state.collectAsStateWithLifecycle()
            Text("Personal learning is exact approved phrase interpretation, not foundation-model retraining. No ambient audio, private conversations, files or screen contents are collected. This profile belongs to this local app installation; no Google account or invisible cloud sync is created.")
            Switch(profile.enabled,{value->authorize{runtime.learning.setEnabled(value);status=if(value)"Approved command learning enabled." else "Learning and recipe matching are off. Existing private entries are retained for review/deletion."}})
            Text("${profile.recipes.size} approved recipes · ${profile.recipes.sumOf{it.aliases.size}} trigger phrases · ${profile.vocabulary.size} vocabulary notes. These are storage counts, not accuracy or training scores.")
            OutlinedTextField(title,{title=it.take(80);approved=false},label={Text("What you meant · recipe title")})
            OutlinedTextField(aliases,{aliases=it.take(900);approved=false},label={Text("What I heard · trigger phrases, one per line")})
            Switch(enabled,{enabled=it;approved=false});Text("Enable this recipe after saving. Every run still reviews its current typed plan.")
            Text("Add up to six supported steps. Stable Android package IDs and the currently paired computer ID are saved; missing apps or a different paired device must not silently substitute another target.")
            OutlinedTextField(appQuery,{appQuery=it.take(80)},label={Text("Find an available Android app")})
            if(appQuery.length>=2)apps.filter{it.label.contains(appQuery,true) || it.packageId.contains(appQuery,true)}.take(8).forEach{app->
                OutlinedButton(enabled=steps.size<LearningPolicy.MAX_STEPS,onClick={steps+=LearnedStep(LearnedAction.LOCAL_APP_OPEN,app.packageId);approved=false}){Text("Add ${app.label} · ${app.packageId}")}
            }
            Text("Computer target: ${pairedDevice ?: "not paired; use existing Paired computer settings first"}")
            OutlinedButton(enabled=pairedDevice!=null && steps.size<LearningPolicy.MAX_STEPS,onClick={steps+=LearnedStep(LearnedAction.DESKTOP_CHROME,targetDeviceId=pairedDevice);approved=false}){Text("Add Chrome on this paired computer")}
            OutlinedTextField(search,{search=it.take(300)},label={Text("Reviewed desktop browser search")})
            OutlinedButton(enabled=pairedDevice!=null && search.isNotBlank() && steps.size<LearningPolicy.MAX_STEPS,onClick={steps+=LearnedStep(LearnedAction.DESKTOP_SEARCH,search.trim(),pairedDevice);approved=false}){Text("Add reviewed desktop search")}
            steps.toList().forEachIndexed{index,step->
                Text("${index+1}. ${step.action} · ${step.argument}"+(step.targetDeviceId?.let{" · $it"} ?: ""))
                TextButton(onClick={steps.removeAt(index);approved=false}){Text("Remove step ${index+1}")}
            }
            val triggers=aliases.lines().map(String::trim).filter(String::isNotEmpty)
            val conflict=LearningPolicy.conflict(triggers,profile.recipes,editing,listOf(com.jarvis.assistant.titan.VoicePreferences(this@LearningActivity).customWakePhrase))
            if(aliases.isNotBlank() && conflict!=null)Text(conflict)
            Button(enabled=profile.enabled && title.isNotBlank() && steps.isNotEmpty() && conflict==null,onClick={approved=false;review=true}){Text(if(editing==null)"Review and save recipe" else "Review changed recipe")}
            TextButton(onClick={editing=null;title="";aliases="";steps.clear();enabled=true;approved=false}){Text("Discard editor draft")}
            if(review)AlertDialog(onDismissRequest={review=false;approved=false},title={Text("Approve this personal command?")},text={
                androidx.compose.foundation.layout.Column{
                    Text("$title\nTriggers: ${triggers.joinToString()}\n"+steps.mapIndexed{i,s->"${i+1}. ${s.action}: ${s.argument} ${s.targetDeviceId.orEmpty()}"}.joinToString("\n"))
                    Switch(approved,{approved=it});Text("I approve these explicit phrases and targets. Saving never executes them; running requires a new reviewed plan. No model training or raw-audio storage occurs.")
                }
            },confirmButton={TextButton(enabled=approved,onClick={
                val snapshot=steps.toList();val id=editing;val label=title;val names=triggers.toList();val active=enabled
                review=false;approved=false
                authorize{require(LearningPolicy.conflict(names,runtime.learning.reload().recipes,id,listOf(com.jarvis.assistant.titan.VoicePreferences(this@LearningActivity).customWakePhrase))==null);val saved=runtime.learning.save(label,names,snapshot,id,active,true);editing=saved.id;status="Saved and read back revision ${saved.revision}. No action was executed."}
            }){Text("Approve and save")}},dismissButton={TextButton(onClick={review=false;approved=false}){Text("Cancel")}})
            HorizontalDivider()
            profile.recipes.forEach{recipe->
                Text("${recipe.title} · revision ${recipe.revision} · ${if(recipe.enabled)"enabled" else "disabled"}\n${recipe.aliases.joinToString()}\n${LearningPolicy.preview(recipe)}")
                OutlinedButton(enabled=profile.enabled && recipe.enabled,onClick={runtime.runLearnedRecipe(recipe.id)}){Text("Try · review current plan first")}
                TextButton(onClick={editing=recipe.id;title=recipe.title;aliases=recipe.aliases.joinToString("\n");enabled=recipe.enabled;steps.clear();steps.addAll(recipe.steps);approved=false}){Text("Edit recipe and aliases")}
                TextButton(onClick={authorize{runtime.learning.delete(recipe.id);status="Recipe deleted. Undo can restore the last approved profile change."}}){Text("Delete recipe")}
            }
            HorizontalDivider()
            Text("Approved vocabulary notes: explicit meanings for review. They do not rewrite recognized words into executable commands. For an app nickname or an executable correction, save a recipe bound to the actual app above.")
            OutlinedTextField(vocabHeard,{vocabHeard=it.take(80)},label={Text("Preferred term / what I heard")})
            OutlinedTextField(vocabMeaning,{vocabMeaning=it.take(160)},label={Text("Approved meaning / what you meant")})
            Button(enabled=profile.enabled && vocabHeard.isNotBlank() && vocabMeaning.isNotBlank(),onClick={val heard=vocabHeard;val meaning=vocabMeaning;authorize{runtime.learning.addVocabulary(heard,meaning,true);status="Approved vocabulary note saved locally and read back. No model weights changed."}}){Text("Approve this vocabulary note")}
            profile.vocabulary.forEach{v->Text("${v.heard} → ${v.meaning}");TextButton(onClick={authorize{runtime.learning.deleteVocabulary(v.id);status="Vocabulary note forgotten."}}){Text("Forget this preference")}}
            OutlinedButton(onClick={authorize{status=if(runtime.learning.undo())"Previous approved profile content restored with a new revision; stale execution plans must be reviewed again." else "No undo snapshot is available."}}){Text("Undo last profile change")}
            Text("Export is plaintext only in your explicitly chosen file and contains approved triggers/search text. Keep it private. Imports replace this profile after review, with imported recipes disabled until individually reviewed and enabled. Existing app data, voices, pairings and models are separate.")
            OutlinedButton(onClick={authorize{pendingExport=runtime.learning.exportApproved();export.launch("Jarvis-approved-commands.json")}}){Text("Export approved personal profile…")}
            OutlinedButton(onClick={import.launch(arrayOf("application/json","text/plain"))}){Text("Choose profile to preview import…")}
            pendingImport?.let{raw->AlertDialog(onDismissRequest={pendingImport=null},title={Text("Replace this learning profile?")},text={Text("${runtime.learning.previewImport(raw).recipes.size} recipes will replace this profile. Imported matching is disabled. The previous approved state remains available through Undo. Review each recipe's device and app before enabling it.")},confirmButton={TextButton(onClick={pendingImport=null;authorize{runtime.learning.importApproved(raw,true);status="Imported and read back, with learning and imported recipes disabled for review."}}){Text("Review complete · import disabled")}},dismissButton={TextButton(onClick={pendingImport=null}){Text("Cancel")}})}
            TextButton(onClick={authorize{runtime.learning.clear();status="This learning profile and its undo snapshots were deleted. Other memories, files, models, voice settings and pairings were preserved."}}){Text("Delete all personal commands and vocabulary")}
        }
    }
    override fun onDestroy(){pendingExport=null;pendingImport=null;super.onDestroy()}
}
