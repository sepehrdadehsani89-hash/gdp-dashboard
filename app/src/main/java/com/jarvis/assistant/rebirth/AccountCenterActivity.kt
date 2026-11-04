package com.jarvis.assistant.rebirth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.Scope
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.security.SystemCredentialPrivacy
import kotlinx.coroutines.*

class AccountCenterActivity:PrivateModuleActivity(){
    private val google by lazy{Identity.getAuthorizationClient(this)}
    private val owner=Any()
    private var pending=false
    private var selected:Account?=null
    private var inSystemFlow=false
    private var metadataApproved by mutableStateOf(false)
    private var accounts by mutableStateOf(emptyList<AccountMetadata>())
    private var sequence=0L
    private var connectionJob:Job?=null
    private val choose=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        inSystemFlow=false
        if(!pending){runCatching{SystemCredentialPrivacy.end(this)};return@registerForActivityResult}
        val email=result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if(result.resultCode!=RESULT_OK || email.isNullOrBlank()){finishConnection("Google account selection cancelled.");return@registerForActivityResult}
        selected=Account(email,"com.google");requestAuthorization(sequence)
    }
    private val consent=registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){result->
        inSystemFlow=false
        if(!pending){runCatching{SystemCredentialPrivacy.end(this)};status="Authorization request expired. Reconnect to try again. Restart listening yourself when ready.";return@registerForActivityResult}
        if(result.resultCode!=RESULT_OK || result.data==null){finishConnection("Google authorization cancelled. No account was connected.");return@registerForActivityResult}
        runCatching{google.getAuthorizationResultFromIntent(result.data!!)}.onSuccess{complete(it,sequence)}.onFailure{finishConnection("Google did not grant authorization. Check OAuth registration, scope and consent configuration.")}
    }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("JARVIS ACCOUNT CENTER"){
            Text("Google · Gmail read-only. Connection is verified against the official Gmail profile endpoint. Google handles passwords and consent. Account tokens are encrypted; they are not backed up, logged or sent to an AI model. Google Calendar / Drive / Microsoft are not connected by this grant.")
            Text("One-time developer setup required: enable Gmail API and register the Android OAuth package/signing certificate in Google Cloud. This app contains no developer-owned client secret. Google Play services and internet are required.")
            Button(enabled=!pending && !inSystemFlow,onClick={authorize{beginConnection()}}){Text("Add Google account / reconnect")}
            if(pending)OutlinedButton(onClick={sequence++;finishConnection("Authorization cancelled locally. Late results will be ignored.")}){Text("Cancel connection")}
            OutlinedButton(enabled=!pending && !inSystemFlow,onClick={authorize{if(getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure){metadataApproved=true;refresh()}else status="Set a secure Android screen lock before viewing private account details."}}){Text("Show connected accounts with Android authentication")}
            if(!metadataApproved)Text("Connected-account details are hidden until Android authentication.")
            else if(accounts.isEmpty())Text("ACCOUNT_NOT_CONNECTED · No authorized mailbox configured.")
            accounts.forEach{a->
                Text("${a.email}\nGoogle · ${if(a.cachedUntil>System.currentTimeMillis())"cached authorization (server may revoke it)" else "reconnect required"}\nGranted service: Gmail read-only\n${a.scopes.joinToString()}")
                Button(enabled=!working && !pending,onClick={authorize{runtime.googleAccounts.authorizeReadSession(a.id);runtime.gmail.clear();metadataApproved=true;refresh();status="Mailbox access authorized for two minutes on this unlocked device. Say 'read my latest email'. Lock or Stop ends the read session."}}){Text("Authorize this mailbox for this session")}
                OutlinedButton(enabled=!working && !pending,onClick={authorize{task{val revoked=withContext(Dispatchers.IO){runtime.googleAccounts.disconnect(a.id)};runtime.gmail.clear();refresh();status=if(revoked)"Disconnected locally and Google token revocation acknowledged." else "Disconnected locally. Remote revocation was not verified; remove Jarvis access in your Google Account security settings if needed."}}}){Text("Disconnect and request token revocation")}
            }
            TextButton(onClick={runtime.googleAccounts.clearReadSession();runtime.gmail.clear();status="Temporary mailbox access ended."}){Text("End temporary mailbox access")}
            if(!pending && !inSystemFlow)TextButton(onClick={if(window.decorView.hasWindowFocus() && !locked()){runtime.stopEverything();runCatching{SystemCredentialPrivacy.end(this@AccountCenterActivity)};status="Interrupted authorization pause cleared with listening stopped. Reconnect or start listening explicitly."}}){Text("Recover after an interrupted Android authorization")}
            TextButton(onClick=::refresh){Text("Refresh local connection metadata")}
        }
    }
    private fun refresh(){if(!metadataApproved){accounts=emptyList();return};runCatching{runtime.googleAccounts.metadata()}.onSuccess{accounts=it}.onFailure{status="Encrypted account metadata is unavailable. No connection is assumed."}}
    private fun beginConnection(){
        if(!getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure){status="Set a secure Android screen lock before connecting private accounts.";return}
        if(pending || inSystemFlow || !runtime.beginExclusiveCapture(owner))return
        metadataApproved=true
        pending=true;val request=++sequence
        try{SystemCredentialPrivacy.begin(this);inSystemFlow=true;choose.launch(AccountManager.newChooseAccountIntent(null,null,arrayOf("com.google"),null,null,null,null))}
        catch(_:Exception){inSystemFlow=false;finishConnection("Google account chooser unavailable. Check Google Play services.")}
        lifecycleScope.launch{delay(120_000);if(pending && request==sequence){sequence++;finishConnection("Authorization expired. Reconnect when ready.")}}
    }
    private fun requestAuthorization(request:Long){
        try{google.authorize(AuthorizationRequest.builder().setAccount(checkNotNull(selected)).setOptOutIncludingGrantedScopes(true).setRequestedScopes(listOf(Scope(GmailScopes.READ_ONLY))).build())
            .addOnSuccessListener{result->if(pending && request==sequence && runtime.captureIsCurrent(owner)){
                if(result.hasResolution()){val p=result.pendingIntent;if(p==null)finishConnection("Google consent is unavailable.") else {inSystemFlow=true;runCatching{consent.launch(IntentSenderRequest.Builder(p.intentSender).build())}.onFailure{inSystemFlow=false;finishConnection("Google consent screen unavailable. Retry from Account Center.")}}}
                else complete(result,request)
            }}.addOnFailureListener{if(pending && request==sequence)finishConnection("Google authorization unavailable. Verify Android OAuth registration, Gmail API, consent test users, Google Play services and connectivity.")}
        }catch(_:Exception){finishConnection("Google authorization service unavailable.")}
    }
    private fun complete(result:AuthorizationResult,request:Long){
        val token=result.accessToken
        if(!pending || request!=sequence || !runtime.captureIsCurrent(owner))return
        if(token.isNullOrBlank() || GmailScopes.READ_ONLY !in result.grantedScopes){finishConnection("Gmail read-only scope was not granted. No account connected.");return}
        connectionJob=lifecycleScope.launch{
            try{
                val email=withContext(Dispatchers.IO){runtime.googleAccounts.profile(token)}
                if(!pending || request!=sequence || locked() || !runtime.captureIsCurrent(owner))return@launch
                check(email.equals(selected?.name,true)){"Google returned a different account."}
                runtime.googleAccounts.save(email,token,result.grantedScopes)
                finishConnection("Google Gmail connection verified. Authorize temporary mailbox access separately before reading.");refresh()
            }catch(e:CancellationException){throw e}catch(_:Exception){finishConnection("Gmail profile verification failed. Check API enablement, scope, account access and quota. No verified connection is claimed.")}
        }
    }
    private fun finishConnection(message:String){
        pending=false;selected=null;connectionJob?.cancel();connectionJob=null
        if(inSystemFlow){runtime.stopEverything();runtime.abandonExclusiveCapture(owner)}
        else{runCatching{SystemCredentialPrivacy.end(this)};runtime.endExclusiveCapture(owner)}
        status=message
    }
    override fun onStop(){if(!inSystemFlow){metadataApproved=false;accounts=emptyList()};if(!inSystemFlow && connectionJob?.isActive==true)finishConnection("Verification paused. Reconnect when this screen is visible.");super.onStop()}
    override fun onDestroy(){sequence++;if(pending || inSystemFlow)finishConnection("Connection ended. Close the Android consent screen before restarting listening.");runtime.abandonExclusiveCapture(owner);super.onDestroy()}
}
