package com.jarvis.assistant.google

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bounded, read-only Google REST calls. No remote text is executed or forwarded to an AI provider. */
class GoogleServicesClient internal constructor(
    private val http:OkHttpClient,
    private val clock:Clock=Clock.systemDefaultZone(),
    private val testOrigin:HttpUrl?=null
){
    constructor():this(OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build())
    init{require(testOrigin==null || (testOrigin.host in setOf("localhost","127.0.0.1","::1") && testOrigin.scheme=="http"))}
    private val hosts=setOf("gmail.googleapis.com","www.googleapis.com","tasks.googleapis.com","people.googleapis.com","docs.googleapis.com","sheets.googleapis.com","slides.googleapis.com","forms.googleapis.com","meet.googleapis.com","chat.googleapis.com","classroom.googleapis.com")
    suspend fun read(session:GoogleAccessSession,query:GoogleQuery,accountEmail:String):GoogleReadResult {
        require(session.product==query.product && session.token.isNotBlank())
        return when(query.product){
            GoogleProduct.GMAIL->gmail(session,query,accountEmail)
            GoogleProduct.DRIVE->drive(session,query)
            GoogleProduct.CALENDAR->{val today=LocalDate.now(clock);val zone=clock.zone
                val root=get(session,"https://www.googleapis.com/calendar/v3/calendars/primary/events",listOf("timeMin" to today.atStartOfDay(zone).toInstant().toString(),"timeMax" to today.plusDays(1).atStartOfDay(zone).toInstant().toString(),"singleEvents" to "true","orderBy" to "startTime","maxResults" to "20"))
                list(query.product,root,"items","Google primary calendar · today",{it.optString("summary","Untitled event")},{it.optJSONObject("start")?.let{x->x.optString("dateTime",x.optString("date"))}.orEmpty()})}
            GoogleProduct.TASKS->list(query.product,get(session,"https://tasks.googleapis.com/tasks/v1/lists/@default/tasks",listOf("maxResults" to "20","showCompleted" to "false","showDeleted" to "false")),"items","Google Tasks · default list",{it.optString("title","Untitled task")},{it.optString("due","No due date")})
            GoogleProduct.CONTACTS->list(query.product,get(session,"https://people.googleapis.com/v1/people/me/connections",listOf("personFields" to "names","pageSize" to "20")),"connections","Google Contacts · names only",{it.optJSONArray("names")?.optJSONObject(0)?.optString("displayName").orEmpty()},{"Authorized contact"})
            GoogleProduct.YOUTUBE->list(query.product,get(session,"https://www.googleapis.com/youtube/v3/channels",listOf("part" to "snippet","mine" to "true","maxResults" to "20")),"items","Authorized YouTube channels",{it.optJSONObject("snippet")?.optString("title").orEmpty()},{"Channel metadata"})
            GoogleProduct.MEET->list(query.product,get(session,"https://meet.googleapis.com/v2/conferenceRecords",listOf("pageSize" to "20")),"conferenceRecords","Accessible Meet conference records",{it.optString("name")},{it.optString("startTime")})
            GoogleProduct.CHAT->list(query.product,get(session,"https://chat.googleapis.com/v1/spaces",listOf("pageSize" to "20")),"spaces","Accessible Chat spaces · message content not requested",{it.optString("displayName",it.optString("name"))},{it.optString("spaceType")})
            GoogleProduct.CLASSROOM->list(query.product,get(session,"https://classroom.googleapis.com/v1/courses",listOf("pageSize" to "20")),"courses","Accessible Classroom courses",{it.optString("name")},{it.optString("courseState")})
            GoogleProduct.DOCS,GoogleProduct.SHEETS,GoogleProduct.SLIDES,GoogleProduct.FORMS->document(session,query)
            else->throw GoogleServiceFailure("UNSUPPORTED",message="${GoogleServicesPolicy.definition(query.product).capability}. ${GoogleServicesPolicy.definition(query.product).prerequisite}.")
        }
    }
    private suspend fun gmail(session:GoogleAccessSession,query:GoogleQuery,email:String):GoogleReadResult {
        val profile=get(session,"https://gmail.googleapis.com/gmail/v1/users/me/profile")
        if(!profile.optString("emailAddress").equals(email,true))throw GoogleServiceFailure("ACCOUNT_MISMATCH",message="Gmail returned a different account. No mail was displayed; select the correct Google account and authorize again.")
        if(query.kind==GoogleQueryKind.UNREAD){val label=get(session,"https://gmail.googleapis.com/gmail/v1/users/me/labels/UNREAD")
            if(!label.has("messagesUnread") || label.getLong("messagesUnread")<0)throw invalid()
            return GoogleReadResult(GoogleProduct.GMAIL,"Gmail reports ${label.getLong("messagesUnread")} unread messages at the time of this request.")}
        val parameters=mutableListOf("maxResults" to "10")
        when(query.kind){
            GoogleQueryKind.TODAY->{val day=LocalDate.now(clock);parameters+="q" to "after:${day.atStartOfDay(clock.zone).toEpochSecond()} before:${day.plusDays(1).atStartOfDay(clock.zone).toEpochSecond()}"}
            GoogleQueryKind.UNREAD_MESSAGES->parameters+="labelIds" to "UNREAD"
            GoogleQueryKind.TEACHER->{val sender=query.sender?.takeIf{GoogleServicesPolicy.validAccount(it) && !it.any{c->c in "\"(){}"}} ?: throw GoogleServiceFailure("USER_ACTION_REQUIRED",message="Save your teacher’s email address in Connected Accounts first. I cannot infer who your teacher is.");parameters+="q" to "from:\"$sender\""}
            GoogleQueryKind.IMPORTANT->parameters+="labelIds" to "IMPORTANT"
            else->Unit
        }
        val root=get(session,"https://gmail.googleapis.com/gmail/v1/users/me/messages",parameters)
        val rows=root.optJSONArray("messages") ?: JSONArray()
        val items=(0 until minOf(rows.length(),10)).map{index->
            val id=GoogleServicesPolicy.validateResource(rows.getJSONObject(index).getString("id"))
            val message=get(session,"https://gmail.googleapis.com/gmail/v1/users/me/messages/$id",listOf("format" to "metadata","metadataHeaders" to "From","metadataHeaders" to "Subject","metadataHeaders" to "Date"))
            val headers=message.optJSONObject("payload")?.optJSONArray("headers") ?: JSONArray()
            fun header(name:String)=(0 until headers.length()).mapNotNull{headers.optJSONObject(it)}.firstOrNull{it.optString("name").equals(name,true)}?.optString("value").orEmpty()
            GoogleReadItem(id,GoogleServicesPolicy.preview(header("Subject").ifBlank{"(No subject)"},180),GoogleServicesPolicy.preview("From: ${header("From")}\nDate: ${header("Date")}\nPreview: ${message.optString("snippet")}",800))
        }
        val heading=when(query.kind){GoogleQueryKind.TEACHER->"Available messages from your configured teacher address";GoogleQueryKind.UNREAD_MESSAGES->"Available unread Gmail previews";GoogleQueryKind.IMPORTANT->"Messages carrying Gmail's IMPORTANT label; this is not a personal urgency judgment";GoogleQueryKind.TODAY->"Today's available Gmail message previews";else->"Recent Gmail message previews"}
        return GoogleReadResult(GoogleProduct.GMAIL,render(heading,items,root.has("nextPageToken"))+"\nPreviews are extracted locally. No message was sent, marked read, or uploaded to an AI provider.",items)
    }
    private suspend fun drive(session:GoogleAccessSession,query:GoogleQuery):GoogleReadResult {
        val folders=query.kind==GoogleQueryKind.PROJECTS
        val root=get(session,"https://www.googleapis.com/drive/v3/files",listOf("q" to if(folders)"trashed = false and mimeType = 'application/vnd.google-apps.folder'" else "trashed = false","orderBy" to "modifiedTime desc","pageSize" to "20","fields" to "nextPageToken,files(id,name,mimeType,modifiedTime)"))
        val rows=root.optJSONArray("files") ?: JSONArray()
        val items=(0 until minOf(rows.length(),20)).map{val row=rows.getJSONObject(it);GoogleReadItem(row.getString("id"),GoogleServicesPolicy.preview(row.optString("name"),180),"Modified: ${GoogleServicesPolicy.preview(row.optString("modifiedTime"),80)}",row.optString("mimeType"))}
        return GoogleReadResult(GoogleProduct.DRIVE,render(if(folders)"Recently modified accessible Drive folders · folder names alone do not establish which projects you are working on" else "Recently modified accessible Drive file metadata",items,root.has("nextPageToken"))+"\nOnly this page was read. Document bodies require separate service consent and an explicit file ID. Nothing was indexed automatically.",items)
    }
    private suspend fun document(session:GoogleAccessSession,query:GoogleQuery):GoogleReadResult {
        val id=query.resourceId?.let(GoogleServicesPolicy::validateResource) ?: throw GoogleServiceFailure("USER_ACTION_REQUIRED",message="Choose a file identifier in Connected Accounts before reading this document.")
        val root=when(query.product){
            GoogleProduct.DOCS->get(session,"https://docs.googleapis.com/v1/documents/$id")
            GoogleProduct.SHEETS->get(session,"https://sheets.googleapis.com/v4/spreadsheets/$id")
            GoogleProduct.SLIDES->get(session,"https://slides.googleapis.com/v1/presentations/$id")
            GoogleProduct.FORMS->get(session,"https://forms.googleapis.com/v1/forms/$id")
            else->throw invalid()
        }
        val identityField=when(query.product){GoogleProduct.DOCS->"documentId";GoogleProduct.SHEETS->"spreadsheetId";GoogleProduct.SLIDES->"presentationId";GoogleProduct.FORMS->"formId";else->throw invalid()}
        if((root.opt(identityField) as? String)!=id)throw invalid()
        val title=when(query.product){GoogleProduct.SHEETS->root.optJSONObject("properties")?.optString("title").orEmpty();GoogleProduct.FORMS->root.optJSONObject("info")?.optString("title").orEmpty();else->root.optString("title")}
        val text=StringBuilder()
        var visited=0
        fun collect(node:Any?,depth:Int=0){
            if(depth>20 || ++visited>50_000 || text.length>=12_000)return
            when(node){
                is JSONObject->{
                    node.optJSONObject("textRun")?.optString("content")?.takeIf{it.isNotBlank()}?.let{text.append(it).append('\n')}
                    if(query.product==GoogleProduct.FORMS)node.optString("title").takeIf{it.isNotBlank()}?.let{text.append(it).append('\n')}
                    node.keys().forEach{key->if(key!="textRun")collect(node.opt(key),depth+1)}
                }
                is JSONArray->for(i in 0 until minOf(node.length(),5000))collect(node.opt(i),depth+1)
            }
        }
        if(query.product==GoogleProduct.SHEETS){
            val cells=get(session,"https://sheets.googleapis.com/v4/spreadsheets/$id/values/A1:T20").optJSONArray("values") ?: JSONArray()
            for(i in 0 until minOf(cells.length(),20))text.append((cells.optJSONArray(i) ?: JSONArray()).let{row->(0 until minOf(row.length(),20)).joinToString(" | "){row.optString(it)}}).append('\n')
        }else collect(root)
        val content=GoogleServicesPolicy.preview(text.toString(),12_000).ifBlank{"No supported plain text was returned. Images, attachments and embedded objects were not read."}
        val item=GoogleReadItem(id,GoogleServicesPolicy.preview(title.ifBlank{"Untitled file"},180),content)
        return GoogleReadResult(query.product,"Authorized ${query.product} · ${item.title}\n${item.detail}\nBounded read of this explicit file only${if(query.product==GoogleProduct.SHEETS)" · A1:T20 of the first sheet" else ""}. No content was uploaded to an AI provider.",listOf(item))
    }
    private fun list(product:GoogleProduct,root:JSONObject,key:String,heading:String,title:(JSONObject)->String,detail:(JSONObject)->String):GoogleReadResult {
        val rows=root.optJSONArray(key) ?: JSONArray()
        val items=(0 until minOf(rows.length(),20)).map{val row=rows.getJSONObject(it);GoogleReadItem(row.optString("id",row.optString("resourceName",row.optString("name"))),GoogleServicesPolicy.preview(title(row),180),GoogleServicesPolicy.preview(detail(row),250))}
        return GoogleReadResult(product,render(heading,items,root.has("nextPageToken")),items)
    }
    private fun render(heading:String,items:List<GoogleReadItem>,more:Boolean)=heading+".\n"+if(items.isEmpty())"No entries were returned for this authorized query." else items.mapIndexed{i,item->"${i+1}. ${item.title}\n${item.detail}"}.joinToString("\n")+if(more)"\nMore results exist; this is a bounded first page." else ""
    private fun invalid()=GoogleServiceFailure("INVALID_RESPONSE",message="Google returned an incomplete or invalid response. No successful data retrieval is claimed.")
    private suspend fun get(session:GoogleAccessSession,address:String,params:List<Pair<String,String>> = emptyList()):JSONObject {
        val original=address.toHttpUrl();require(original.isHttps && original.host in hosts && original.username.isEmpty() && original.password.isEmpty())
        val url=(testOrigin?.newBuilder()?.encodedPath(original.encodedPath) ?: original.newBuilder()).apply{params.forEach{addQueryParameter(it.first,it.second)}}.build()
        val call=http.newCall(Request.Builder().url(url).header("Authorization","Bearer ${session.token}").header("Accept","application/json").get().build())
        return suspendCancellableCoroutine{continuation->
            continuation.invokeOnCancellation{call.cancel()}
            call.enqueue(object:Callback {
                override fun onFailure(call:Call,e:IOException){if(continuation.isActive)continuation.resumeWithException(GoogleServiceFailure(if(e is java.net.SocketTimeoutException)"TIMEOUT" else "NETWORK",message="The Google request could not complete. Check connectivity; no data result is claimed."))}
                override fun onResponse(call:Call,response:Response){
                    val result=runCatching{response.use{r->
                        if(!r.isSuccessful)throw GoogleServiceFailure(when(r.code){401->"AUTHENTICATION";403->"AUTHORIZATION";429->"QUOTA";in 500..599->"SERVICE";else->"HTTP"},r.code,when(r.code){401->"Google authorization expired or was revoked. Reconnect this service.";403->"Google denied this service. Check the API, scope, OAuth project/test-user setup and account or administrator eligibility.";429->"Google rate or quota limits were reached. Retry later; no automatic loop was started.";else->"Google could not complete the read request (HTTP ${r.code}). No successful retrieval is claimed."})
                        val body=r.body ?: throw invalid();if(body.contentLength()>2_000_000)throw invalid()
                        val source=body.source();if(source.request(2_000_001) && source.buffer.size>2_000_000)throw invalid()
                        try{JSONObject(source.readUtf8()).also{if(it.has("error"))throw invalid()}}catch(_:org.json.JSONException){throw invalid()}
                    }}
                    if(continuation.isActive)result.fold({continuation.resume(it)},{continuation.resumeWithException(it)})
                }
            })
        }
    }
}
