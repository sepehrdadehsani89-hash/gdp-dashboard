package com.jarvis.assistant.google

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.CancellationException

/** Foreground account wizard; Android/Google own all account selection and consent screens. */
class GoogleAccountsActivity:PrivateModuleActivity(){
    private var handoff:Long?=null
    private var pending:GoogleAuthorizationResult.Consent?=null
    private var lastResult by mutableStateOf<GoogleReadResult?>(null)
    private var resultAccount:String?=null
    private var resultEpoch:Long?=null
    private var viewRevision by mutableIntStateOf(0)
    private var removal by mutableStateOf<GoogleAccountConnection?>(null)
    private val accountPicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        endHandoff()
        if(result.resultCode!=Activity.RESULT_OK || locked()){status="Account selection cancelled. No Google service is connected.";return@registerForActivityResult}
        val email=result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val type=result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
        if(type!="com.google" || email==null || !GoogleServicesPolicy.validAccount(email)){status="Android did not return a valid Google account. No connection was saved.";return@registerForActivityResult}
        runCatching{runtime.googleConnections.addAccount(email)}.onSuccess{clearResult();status="Account chosen. No service has been verified. Authorize only a service you want to use."}.onFailure{status="Account metadata could not be saved. Existing storage was preserved."}
    }
    private val consent=registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){result->
        endHandoff();val request=pending;pending=null
        if(result.resultCode!=Activity.RESULT_OK || request==null || locked()){status="Google consent cancelled or expired. No service verification is claimed.";return@registerForActivityResult}
        googleTask{val session=runtime.googleAuthorization.complete(result.data,request);read(session,GoogleQuery(session.product))}
    }
    private fun endHandoff(){handoff?.let(runtime::finishAndroidUi);handoff=null}
    private fun clearResult(){lastResult=null;resultAccount=null;resultEpoch=null;viewRevision++}
    private fun googleTask(block:suspend ()->Unit)=task{try{block()}catch(e:CancellationException){throw e}catch(e:GoogleServiceFailure){clearResult();status="${e.category}: ${e.message}"}}
    private fun request(product:GoogleProduct,resource:String?=null){clearResult();googleTask{
        when(val answer=runtime.googleAuthorization.request(product)){
            is GoogleAuthorizationResult.Ready->read(answer.session,GoogleQuery(product,if(resource.isNullOrBlank())GoogleQueryKind.RECENT else GoogleQueryKind.DETAIL,resource?.takeIf{it.isNotBlank()}))
            is GoogleAuthorizationResult.Consent->{pending=answer;handoff=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide()
                try{consent.launch(IntentSenderRequest.Builder(answer.intent.intentSender).build());status="Google consent is pending; no service is verified. After consent, select a file ID again if needed."}
                catch(_:Exception){pending=null;endHandoff();status="Android could not show Google consent. Check Play services and OAuth configuration."}}
        }
    }}
    private suspend fun read(session:GoogleAccessSession,query:GoogleQuery){
        val row=runtime.googleConnections.selected() ?: throw GoogleServiceFailure("STALE",message="Choose the account again.")
        val result=try{runtime.googleClient.read(session,query,row.email)}catch(e:GoogleServiceFailure){runtime.googleConnections.markFailure(row.id,query.product);if(e.status==401)runtime.googleAuthorization.invalidate(session);throw e}
        runtime.googleAuthorization.requireCurrent(row.id,session.epoch)
        if(locked())return
        runtime.googleConnections.markVerified(row.id,session.product,session.epoch)
        lastResult=result;resultAccount=row.id;resultEpoch=session.epoch;viewRevision++
        status="VERIFIED · A real ${GoogleServicesPolicy.definition(session.product).label} read succeeded. Result below; this does not verify other services."
    }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("JARVIS · AI & CONNECTED ACCOUNTS"){
            LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(30_000);viewRevision++}}
            val connections by runtime.googleConnections.state.collectAsStateWithLifecycle()
            var filter by remember{mutableStateOf(intent.getStringExtra("google_product").orEmpty())}
            var resource by remember{mutableStateOf("")}
            Text("Your AI brain and Google account permissions are separate. Jarvis never asks for a Google password. Service consent alone is not an API verification.")
            Button(onClick={startActivity(Intent(this@GoogleAccountsActivity,com.jarvis.assistant.brain.BrainManagerActivity::class.java))}){Text("Existing AI Brain Manager · accounts & API keys")}
            Text("First-time Google setup: create a Google Cloud project; configure Google Auth Platform consent and test users; enable each API you need; create an Android OAuth client for ${packageName} with this APK's signing SHA-1. No client secret belongs in the app. The full guide is docs/jarvis/GOOGLE_SETUP.md in the repository.")
            TextButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://console.cloud.google.com/apis/credentials")))}){Text("Open official Google Cloud setup")}
            connections.error?.let{Text(it)}
            Button(enabled=!working,onClick={authorize{clearResult();handoff=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide()
                try{accountPicker.launch(AccountManager.newChooseAccountIntent(null,null,arrayOf("com.google"),null,null,null,null))}
                catch(_:Exception){endHandoff();status="Android account selection is unavailable. Check Google Play services."}}}){Text("Choose / add a Google account using Android")}
            Text("Removing a saved connection only removes Jarvis access. It does not delete your Google account, emails or files. Revoke is also available in your Google Account → Third-party connections.")
            connections.accounts.forEach{row->
                Text(row.email+if(connections.selected==row.id)" · selected" else "")
                OutlinedButton(enabled=!working,onClick={authorize{runtime.googleConnections.select(row.id);clearResult()}}){Text("Select this account")}
                TextButton(enabled=!working,onClick={removal=row}){Text("Disconnect locally / revoke access…")}
            }
            val selected=connections.accounts.firstOrNull{it.id==connections.selected}
            var teacher by remember(connections.selected){mutableStateOf(selected?.teacherEmail.orEmpty())}
            if(selected!=null){OutlinedTextField(teacher,{teacher=it.take(254)},label={Text("Teacher email · only if you want sender-specific queries")});OutlinedButton(enabled=!working,onClick={authorize{runtime.googleConnections.setTeacher(selected.id,teacher);clearResult();status="Teacher address saved locally. This is a sender filter, not an inferred identity."}}){Text("Save / clear teacher sender filter")}}
            OutlinedTextField(filter,{filter=it.take(100)},label={Text("Find a Google service")})
            OutlinedTextField(resource,{resource=it.take(180)},label={Text("Explicit Docs / Sheets / Slides / Forms file ID")})
            GoogleServicesPolicy.services.filter{filter.isBlank() || it.label.contains(filter,true) || it.product.name.contains(filter,true)}.forEach{definition->
                HorizontalDivider();Text(definition.label,style=MaterialTheme.typography.titleMedium);Text(definition.capability);Text(definition.prerequisite)
                val granted=selected?.services?.contains(definition.product)==true
                val verified=selected?.let{runtime.googleConnections.currentlyVerified(it.id,definition.product)}==true
                Text(if(verified)"VERIFIED · successful read in this process within the last hour" else if(granted)"Authorized previously · no current successful read is claimed" else "Not authorized / setup required")
                selected?.lastVerified?.get(definition.product)?.let{Text("Last recorded successful read: ${java.util.Date(it)} · historical status")}
                if(definition.scope!=null){
                    Text("Requested scope: ${definition.scope}")
                    Button(enabled=selected!=null && !working,onClick={request(definition.product,resource)}){Text("Authorize / test real read")}
                    if(granted)TextButton(enabled=!working,onClick={authorize{runtime.googleConnections.removeService(selected!!.id,definition.product);clearResult();status="This service was disabled locally. To revoke Google's grant, disconnect/revoke the account or use Google Account settings."}}){Text("Disable this service locally")}
                }else TextButton(onClick={val intent=when(definition.product){GoogleProduct.PHOTOS->Intent(this@GoogleAccountsActivity,com.jarvis.assistant.rebirth.PersonalContentActivity::class.java);GoogleProduct.MAPS->Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="));else->Intent(Intent.ACTION_VIEW,Uri.parse("https://keep.google.com/"))};status=runtime.googleLaunch(intent,definition.label)}){Text("Open official app / existing content picker")}
            }
            lastResult?.let{result->
                HorizontalDivider();Text("Actual read result · private, not sent to an AI provider");Text(result.summary)
                val indexable=result.product in setOf(GoogleProduct.DRIVE,GoogleProduct.DOCS,GoogleProduct.SHEETS,GoogleProduct.SLIDES,GoogleProduct.CALENDAR,GoogleProduct.TASKS)
                if(indexable)OutlinedButton(enabled=!working,onClick={authorize{val id=resultAccount;check(id!=null && id==runtime.googleConnections.selected()?.id && resultEpoch==runtime.googleConnections.epoch);runtime.googleProjectIndex.index(id,result);viewRevision++;status="This bounded result was indexed in encrypted local storage. It is separate from general AI retrieval. Nothing was indexed automatically."}}){Text("Index this result locally · explicit snapshot")}
            }
            if(selected!=null){
                val pages=remember(viewRevision,selected.id){runCatching{runtime.googleProjectIndex.list(selected.id)}.getOrDefault(emptyList())}
                Text("Indexed snapshots: ${pages.size}. Snapshots can become stale; no background or historical access is implied.")
                pages.forEach{page->Text("${page.product} · ${java.util.Date(page.at)}");TextButton(onClick={authorize{runtime.googleProjectIndex.delete(selected.id,page.product);viewRevision++}}){Text("Delete this local index")}}
                TextButton(onClick={authorize{runtime.googleProjectIndex.delete(selected.id);viewRevision++;status="All Google project snapshots for this account were deleted locally."}}){Text("Delete all this account's local Google indexes")}
            }
            removal?.let{row->AlertDialog(onDismissRequest={removal=null},title={Text("Disconnect ${row.email}?")},text={Text("Only Jarvis connection metadata and this account's local Google snapshots will be removed. Google account data will remain. A remote revoke can fail offline.")},confirmButton={TextButton(onClick={removal=null;authorize{googleTask{runtime.googleProjectIndex.delete(row.id);val revoked=runtime.googleAuthorization.revoke(row);clearResult();status=if(revoked)"Saved connection removed; Google revocation completed (or no service grant existed)." else "Saved connection removed locally. Remote revocation was not confirmed; revoke Jarvis in Google Account settings."}}}){Text("Disconnect and request revoke")}},dismissButton={TextButton(onClick={removal=null;authorize{runtime.googleProjectIndex.delete(row.id);runtime.googleConnections.removeAccount(row.id);clearResult();status="Saved connection removed locally. Google's grant was not revoked; use Google Account settings if needed."}}){Text("Remove locally only")}})}
        }
    }
    override fun onStop(){lastResult=null;super.onStop()}
    override fun onDestroy(){pending=null;endHandoff();super.onDestroy()}
}
