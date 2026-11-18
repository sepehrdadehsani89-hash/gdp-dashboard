package com.jarvis.assistant.google

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class GoogleCloudConfigurationActivity:PrivateModuleActivity(){
    private val configuration by lazy{GoogleCloudConfigurationStore(vault)}
    private var project by mutableStateOf("");private var client by mutableStateOf("");private var key by mutableStateOf("")
    private var publicResult by mutableStateOf<GooglePublicKeyResult?>(null)
    private fun restore(){runCatching{configuration.load()}.onSuccess{project=it.projectId;client=it.androidClientId;key=it.apiKey;publicResult=null;status="Saved configuration loaded. Saving metadata does not establish a connected Google service."}.onFailure{status="Private Google configuration is unavailable. Existing data was preserved. No connected state is assumed."}}
    private fun services(product:GoogleProduct?=null){val i=Intent(this,GoogleAccountsActivity::class.java);if(product!=null)i.putExtra("google_product",product.name);startActivity(i)}
    private fun signingSha1():String=runCatching{val info=packageManager.getPackageInfo(packageName,PackageManager.GET_SIGNING_CERTIFICATES);val signer=info.signingInfo?.apkContentsSigners?.singleOrNull() ?: error("No unique signer");MessageDigest.getInstance("SHA-1").digest(signer.toByteArray()).joinToString(":"){"%02X".format(it)}}.getOrDefault("UNAVAILABLE · inspect the installed APK signature")
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return;restore()
        val fingerprint=signingSha1()
        screen("JARVIS · GOOGLE CLOUD CONFIGURATION"){
            Text("An API key tests permitted public API access. Private Gmail, Calendar and Drive require Google's official account selection and per-service OAuth consent. A saved email or configuration is not a verified connection.")
            Text("This installed package: $packageName\nActual APK signing SHA-1: $fingerprint")
            OutlinedTextField(project,{project=it;publicResult=null},label={Text("Google Cloud Project ID · registration metadata")},enabled=!working,singleLine=true)
            OutlinedTextField(client,{client=it;publicResult=null},label={Text("Android OAuth Client ID · registration metadata")},enabled=!working,singleLine=true)
            OutlinedTextField(key,{key=it;publicResult=null},label={Text("Optional Google API key · encrypted, never a password")},visualTransformation=PasswordVisualTransformation(),enabled=!working,singleLine=true)
            Text("Google AuthorizationClient resolves the Android registration from this package and certificate. This client-ID field records your setup; it cannot override Google's registration or turn a Web client secret into Android authorization. The project ID is not automatically treated as a verified billing or quota project.")
            Button(enabled=!working,onClick={val reviewed=GoogleCloudConfiguration(project.trim(),client.trim(),key.trim());authorize{try{configuration.save(reviewed);restore();status="Configuration saved encrypted. No account, Gmail access or project ownership was verified."}catch(e:IllegalArgumentException){status=e.message ?: "Invalid configuration"}}}){Text("Save actual Google configuration")}
            OutlinedButton(enabled=!working,onClick=::restore){Text("Reconfigure · reload saved values")}
            OutlinedButton(enabled=!working,onClick={task{publicResult=null;val saved=configuration.load();if(saved.apiKey.isBlank()){status="USER ACTION REQUIRED · Save an authorized API key and enable YouTube Data API v3 for this public-only test.";return@task};try{val result=withContext(Dispatchers.IO){GooglePublicKeyTest().read(saved.apiKey,packageName,fingerprint.replace(":",""))};check(configuration.load()==saved);publicResult=result;status="LIVE PUBLIC API READ VERIFIED · YouTube language metadata returned. This verifies neither a Google account nor private Gmail access."}catch(e:GoogleKeyFailure){status="${e.category} · Public API-key test failed. Check the key's restrictions, enabled YouTube Data API v3, quota and network. No account verification is claimed."}}}){Text("Test saved API key · real public metadata request")}
            publicResult?.let{Text("Actual public API result: ${it.languages} language records, ${it.elapsedMillis} ms. Historical result for this screen only; not a private-account connection.")}
            Button(onClick={services()}){Text("Google account selector · authorized services · permission management")}
            OutlinedButton(onClick={services(GoogleProduct.GMAIL)}){Text("Test Google identity through Gmail's authenticated profile")}
            OutlinedButton(onClick={services(GoogleProduct.GMAIL)}){Text("Test Gmail · consent and real read")}
            OutlinedButton(onClick={services(GoogleProduct.CALENDAR)}){Text("Test Calendar · consent and real read")}
            OutlinedButton(onClick={services(GoogleProduct.DRIVE)}){Text("Test Drive · consent and real read")}
            TextButton(onClick={services()}){Text("Reauthorize / remove Google account connection")}
            TextButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://console.cloud.google.com/")))}){Text("Open official Google Cloud Console")}
            Text("Setup: create a project; configure Google Auth Platform branding, audience/test users and required read scopes; enable only the APIs you need; create an Android OAuth client for the package and signing SHA-1 shown above. Add this account as a test user when required. Review Google's sensitive/restricted scope policies for public distribution. Never enter a client secret, Google password or cookie in Jarvis.")
            Text("In Connected Services choose the real Google account, authorize one service and perform its actual read. A Gmail profile must match the selected account before previews are returned. Each service is independent; denied or skipped consent remains unverified.")
            Text("Gmail push (watch / Pub/Sub / history) needs a configured owner-controlled backend and Google Cloud project. This preview does not create an unauthenticated webhook or pretend that this infrastructure exists. Android notification awareness is a separate permission-gated source, not Gmail unread-count proof.")
            TextButton(enabled=!working,onClick={authorize{configuration.clear();project="";client="";key="";publicResult=null;status="Only local Cloud configuration was removed. Existing Google service grants/accounts were preserved; manage or revoke them in Connected Services."}}){Text("Remove local Cloud configuration only")}
        }
    }
    override fun onStop(){key="";publicResult=null;super.onStop()}
    override fun onResume(){super.onResume();if(!isFinishing)restore()}
}
