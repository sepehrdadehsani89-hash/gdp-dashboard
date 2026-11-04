package com.jarvis.assistant.rebirth

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import com.jarvis.assistant.omnisystem.*
import okhttp3.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.*
import java.util.UUID
import java.util.concurrent.TimeUnit

object GmailScopes { const val READ_ONLY="https://www.googleapis.com/auth/gmail.readonly" }
data class AccountMetadata(val id:String,val email:String,val scopes:List<String>,val cachedUntil:Long)
class AccountAccessFailure(val status:String):Exception(status)

/** OAuth access tokens live only in the existing Android Keystore-encrypted vault. No passwords. */
class GoogleAccounts(private val context:Context,private val vault:FeatureVault){
    private val name="rebirth_google_accounts"
    private var authorizedAccount:String?=null
    private var authorizedUntil=0L
    val http=ModuleHttp(OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build())
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
    @Synchronized private fun rows():List<JSONObject>{
        val raw=vault.read(name) ?: run{if(vault.contains(name))throw AccountAccessFailure("PRIVATE_STORAGE_UNAVAILABLE");return emptyList()}
        val a=JSONArray(raw);require(a.length()<=5)
        return (0 until a.length()).map{a.getJSONObject(it)}
    }
    @Synchronized fun metadata():List<AccountMetadata>{
        check(unlocked())
        return rows().map{j->val a=j.getJSONArray("scopes");AccountMetadata(j.getString("id"),j.getString("email"),(0 until a.length()).map{a.getString(it)},j.getLong("until"))}
    }
    @Synchronized fun save(email:String,token:String,scopes:List<String>):String {
        check(unlocked());require(email.length in 3..254 && '@' in email && email.none(Char::isISOControl))
        require(token.length in 10..10000 && GmailScopes.READ_ONLY in scopes)
        val old=rows();require(old.size<5 || old.any{it.optString("email")==email})
        val id=old.firstOrNull{it.optString("email")==email}?.getString("id") ?: UUID.randomUUID().toString()
        val row=JSONObject().put("id",id).put("email",email).put("token",token).put("scopes",JSONArray(listOf(GmailScopes.READ_ONLY))).put("until",System.currentTimeMillis()+50*60_000L)
        vault.write(name,JSONArray(old.filterNot{it.optString("id")==id}+row).toString());clearReadSession();return id
    }
    @Synchronized fun authorizeReadSession(id:String){check(context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure==true){"Set a secure Android screen lock before authorizing private mailbox access."};check(unlocked() && metadata().any{it.id==id});authorizedAccount=id;authorizedUntil=SystemClock.elapsedRealtime()+120_000}
    @Synchronized fun clearReadSession(){authorizedAccount=null;authorizedUntil=0}
    @Synchronized fun sessionAccount():String?=authorizedAccount?.takeIf{unlocked() && SystemClock.elapsedRealtime()<authorizedUntil}
    @Synchronized fun accessToken(id:String,requireReadSession:Boolean=true):String {
        if(!unlocked())throw AccountAccessFailure("AUTHENTICATION_REQUIRED")
        if(requireReadSession && sessionAccount()!=id)throw AccountAccessFailure("AUTHENTICATION_REQUIRED")
        val row=rows().firstOrNull{it.optString("id")==id} ?: throw AccountAccessFailure("ACCOUNT_NOT_CONNECTED")
        if(row.optLong("until")<=System.currentTimeMillis())throw AccountAccessFailure("AUTHORIZATION_EXPIRED")
        return row.getString("token")
    }
    @Synchronized fun expire(id:String){val all=rows();all.firstOrNull{it.optString("id")==id}?.put("until",0);vault.write(name,JSONArray(all).toString());clearReadSession()}
    @Synchronized fun remove(id:String){vault.write(name,JSONArray(rows().filterNot{it.optString("id")==id}).toString());clearReadSession()}
    suspend fun profile(token:String):String = http.json(Request.Builder().url("https://gmail.googleapis.com/gmail/v1/users/me/profile").header("Authorization","Bearer $token").build(),100_000).getString("emailAddress")
    suspend fun disconnect(id:String):Boolean {
        val token=runCatching{accessToken(id,false)}.getOrNull()
        val revoked=if(token==null)false else try{http.bytes(Request.Builder().url("https://oauth2.googleapis.com/revoke").post(FormBody.Builder().add("token",token).build()).build(),100_000);true}catch(e:CancellationException){throw e}catch(_:Exception){false}
        currentCoroutineContext().ensureActive();remove(id);return revoked
    }
}
