package com.jarvis.assistant.google

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleAccessSession internal constructor(val accountId:String,val product:GoogleProduct,internal val token:String,val epoch:Long){
    override fun toString()="GoogleAccessSession(product=$product, credentials=redacted)"
}
sealed interface GoogleAuthorizationResult {
    class Ready(val session:GoogleAccessSession):GoogleAuthorizationResult
    class Consent(val intent:PendingIntent,val accountId:String,val product:GoogleProduct,val epoch:Long):GoogleAuthorizationResult
}
/** Native Google consent only. Play services, rather than Jarvis storage, owns renewable grants. */
class GoogleAuthorization(context:Context,private val store:GoogleConnectionsStore){
    private val client=Identity.getAuthorizationClient(context.applicationContext)
    suspend fun request(product:GoogleProduct):GoogleAuthorizationResult {
        val row=store.selected() ?: throw GoogleServiceFailure("SETUP",message="Open AI & Connected Accounts, choose a Google account, and authorize this service first.")
        val scope=GoogleServicesPolicy.definition(product).scope ?: throw GoogleServiceFailure("UNSUPPORTED",message="This service uses an official app/picker shortcut; no unrestricted account API is connected.")
        val expected=store.epoch
        val result=try{client.authorize(AuthorizationRequest.builder().setAccount(Account(row.email,"com.google"))
            .setRequestedScopes(listOf(Scope(scope))).setOptOutIncludingGrantedScopes(true).build()).awaitResult()}catch(e:Exception){throw failure(e)}
        requireCurrent(row.id,expected)
        return if(result.hasResolution())GoogleAuthorizationResult.Consent(result.pendingIntent ?: throw GoogleServiceFailure("AUTHORIZATION",message="Google returned no consent intent. Retry from account setup."),row.id,product,expected)
        else GoogleAuthorizationResult.Ready(accept(result,row.id,product,expected))
    }
    fun complete(data:Intent?,pending:GoogleAuthorizationResult.Consent):GoogleAccessSession {
        requireCurrent(pending.accountId,pending.epoch)
        val result=try{client.getAuthorizationResultFromIntent(data ?: throw GoogleServiceFailure("CANCELLED",message="Google consent was cancelled. Nothing was connected."))}catch(e:Exception){throw failure(e)}
        return accept(result,pending.accountId,pending.product,pending.epoch)
    }
    private fun accept(result:com.google.android.gms.auth.api.identity.AuthorizationResult,id:String,product:GoogleProduct,expected:Long):GoogleAccessSession {
        requireCurrent(id,expected)
        val token=result.accessToken?.takeIf{it.isNotBlank() && it.length<=16_384 && !it.any(Char::isISOControl)}
            ?: throw GoogleServiceFailure("AUTHORIZATION",message="Google did not issue an access token. No service is verified.")
        try{store.grant(id,product,result.grantedScopes)}catch(_:IllegalArgumentException){throw GoogleServiceFailure("SCOPE",message="The requested read permission was not granted. Authorize this service separately.")}
        return GoogleAccessSession(id,product,token,store.epoch)
    }
    fun requireCurrent(id:String,expected:Long){if(store.epoch!=expected || store.selected()?.id!=id)throw GoogleServiceFailure("STALE",message="The Google account or permissions changed. Repeat the request for the selected account.")}
    suspend fun revoke(row:GoogleAccountConnection):Boolean {
        // Remove local access before a possibly unavailable remote call. No account/cloud data is deleted.
        store.removeAccount(row.id)
        if(row.services.isEmpty())return true
        return try{client.revokeAccess(RevokeAccessRequest.builder().setAccount(Account(row.email,"com.google"))
            .setScopes(row.services.mapNotNull{GoogleServicesPolicy.definition(it).scope?.let(::Scope)}).build()).awaitResult();true}
        catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){false}
    }
    suspend fun invalidate(session:GoogleAccessSession){try{client.clearToken(ClearTokenRequest.builder().setToken(session.token).build()).awaitResult()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){/* Remote/cache invalidation is not claimed. */}}
    companion object {
        fun failure(e:Exception):Exception {
            if(e is kotlinx.coroutines.CancellationException || e is GoogleServiceFailure)return e
            return when((e as? ApiException)?.statusCode){
                10->GoogleServiceFailure("SETUP",10,"Google rejected the Android OAuth configuration. Register this package and signing SHA-1, enable the API, and configure consent/test users. See the Google setup guide.")
                7->GoogleServiceFailure("NETWORK",7,"Google authorization could not reach the network. Saved account metadata was preserved.")
                16,12501->GoogleServiceFailure("CANCELLED",message="Google authorization was cancelled. No service verification is claimed.")
                else->GoogleServiceFailure("AUTHORIZATION",message="Google authorization is unavailable. Check Google Play services, account eligibility, consent and the Android OAuth setup.")
            }
        }
    }
}
private suspend fun <T> Task<T>.awaitResult():T=suspendCancellableCoroutine{continuation->
    addOnSuccessListener{if(continuation.isActive)continuation.resume(it)}
    addOnFailureListener{if(continuation.isActive)continuation.resumeWithException(it)}
    addOnCanceledListener{continuation.cancel()}
}
