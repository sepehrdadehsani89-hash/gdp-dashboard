package com.jarvis.assistant.google

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.core.AssistantReply
import com.jarvis.assistant.rebirth.AppLaunchCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

class GoogleAssistant(private val context:Context,private val store:GoogleConnectionsStore,private val authorization:GoogleAuthorization,private val client:GoogleServicesClient,private val index:GoogleProjectIndex,private val launches:AppLaunchCoordinator,private val onVerified:(GoogleAccessSession,GoogleReadResult)->Unit={_,_->}){
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
    private fun setup(product:GoogleProduct?=null):String=launches.open(Intent(context,GoogleAccountsActivity::class.java).apply{product?.let{putExtra("google_product",it.name)}},"AI & Connected Accounts").detail
    suspend fun route(text:String):AssistantReply? {
        val projects=com.jarvis.assistant.neural.AppMatching.normalize(text) in setOf("summarize my project documents","show my indexed google projects")
        if(!GoogleCommandParser.navigation(text) && !GoogleCommandParser.requiresNativeRead(text) && !projects)return null
        if(!unlocked())return AssistantReply("Unlock with Android before accessing Google accounts, email or project data.")
        if(GoogleCommandParser.navigation(text))return AssistantReply(setup())
        val row=try{store.selected()}catch(_:Exception){return AssistantReply("Saved Google connection metadata is unavailable. It was preserved; no mailbox was checked.")}
        if(row==null)return AssistantReply("Google is not connected yet. Register Jarvis in Google Cloud using the setup guide, then choose an account and authorize the requested service. ${setup()}")
        if(projects)return AssistantReply(index.summarize(row.id))
        val query=GoogleCommandParser.parse(text)?.let{if(it.kind==GoogleQueryKind.TEACHER)it.copy(sender=row.teacherEmail)else it}
            ?: return AssistantReply("That mailbox query is not implemented by the authorized Gmail reader. No inbox result was obtained or invented. Try ‘catch me up on my emails’, ‘how many unread emails do I have’, or ‘did my teacher email me’ after configuring that sender in Connected Accounts.")
        if(query.product !in row.services)return AssistantReply("${GoogleServicesPolicy.definition(query.product).label} read access has not been authorized. ${setup(query.product)} No Google data was read.")
        var session:GoogleAccessSession?=null
        return try{withTimeout(45_000){
            when(val access=authorization.request(query.product)){
                is GoogleAuthorizationResult.Consent->AssistantReply("Google requires your account consent again. ${setup(query.product)} No Google data was read.")
                is GoogleAuthorizationResult.Ready->{session=access.session;val result=client.read(access.session,query,row.email)
                    authorization.requireCurrent(row.id,access.session.epoch)
                    if(!unlocked())AssistantReply("Google data is hidden while the phone is locked.")else{store.markVerified(row.id,query.product,access.session.epoch);onVerified(access.session,result);AssistantReply(result.summary)}}
            }
        }}catch(e:CancellationException){throw e}catch(e:GoogleServiceFailure){store.markFailure(row.id,query.product);if(e.status==401)session?.let{authorization.invalidate(it)};AssistantReply("${e.category}: ${e.message}")}
    }
}
