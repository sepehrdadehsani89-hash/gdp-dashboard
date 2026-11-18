package com.jarvis.assistant.google

import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.tools.ToolPolicy
import java.security.MessageDigest

enum class GoogleProduct { GMAIL,CALENDAR,DRIVE,DOCS,SHEETS,SLIDES,TASKS,CONTACTS,PHOTOS,YOUTUBE,MAPS,MEET,CHAT,FORMS,CLASSROOM,KEEP }
data class GoogleServiceDefinition(val product:GoogleProduct,val label:String,val scope:String?,val capability:String,val prerequisite:String)
object GoogleServicesPolicy {
    val services=listOf(
        GoogleServiceDefinition(GoogleProduct.GMAIL,"Gmail","https://www.googleapis.com/auth/gmail.readonly","Read real message metadata/previews and unread-label count","Gmail API enabled; restricted scope consent/verification requirements apply"),
        GoogleServiceDefinition(GoogleProduct.CALENDAR,"Google Calendar","https://www.googleapis.com/auth/calendar.events.readonly","Read authorized primary-calendar events","Calendar API enabled"),
        GoogleServiceDefinition(GoogleProduct.DRIVE,"Google Drive","https://www.googleapis.com/auth/drive.metadata.readonly","List actual recent files/project folders; explicit indexing only","Drive API enabled; metadata permission does not authorize reading every document body"),
        GoogleServiceDefinition(GoogleProduct.DOCS,"Google Docs","https://www.googleapis.com/auth/documents.readonly","Read an explicitly selected document ID","Docs API enabled; choose a document you can access"),
        GoogleServiceDefinition(GoogleProduct.SHEETS,"Google Sheets","https://www.googleapis.com/auth/spreadsheets.readonly","Read metadata and a bounded range from an explicitly selected spreadsheet","Sheets API enabled; read-only, no cell writes"),
        GoogleServiceDefinition(GoogleProduct.SLIDES,"Google Slides","https://www.googleapis.com/auth/presentations.readonly","Read text from an explicitly selected presentation","Slides API enabled"),
        GoogleServiceDefinition(GoogleProduct.TASKS,"Google Tasks","https://www.googleapis.com/auth/tasks.readonly","List actual default-list tasks","Tasks API enabled"),
        GoogleServiceDefinition(GoogleProduct.CONTACTS,"Google Contacts","https://www.googleapis.com/auth/contacts.readonly","Read up to20 authorized contact names","People API enabled; phone's local Contacts permission remains separate"),
        GoogleServiceDefinition(GoogleProduct.PHOTOS,"Google Photos",null,"Select photos using the existing Android photo picker","No unrestricted Photos library access is claimed; cloud-provider availability depends on Android and installed apps"),
        GoogleServiceDefinition(GoogleProduct.YOUTUBE,"YouTube","https://www.googleapis.com/auth/youtube.readonly","Read the authorized channel's metadata","YouTube Data API enabled; a channel may not exist for every Google account"),
        GoogleServiceDefinition(GoogleProduct.MAPS,"Google Maps",null,"Launch the installed Maps app","No private Maps history/account API is assumed"),
        GoogleServiceDefinition(GoogleProduct.MEET,"Google Meet","https://www.googleapis.com/auth/meetings.space.readonly","List authorized conference records where eligible","Meet REST API enabled; account/Workspace eligibility is decided by Google"),
        GoogleServiceDefinition(GoogleProduct.CHAT,"Google Chat","https://www.googleapis.com/auth/chat.spaces.readonly","List authorized Chat spaces, not private messages","Chat API enabled; messages require their own separately supported authorization"),
        GoogleServiceDefinition(GoogleProduct.FORMS,"Google Forms","https://www.googleapis.com/auth/forms.body.readonly","Read an explicitly selected authorized form's title/questions","Forms API enabled; this does not grant access to submitted responses"),
        GoogleServiceDefinition(GoogleProduct.CLASSROOM,"Google Classroom","https://www.googleapis.com/auth/classroom.courses.readonly","List actual authorized courses","Classroom API enabled; grades/coursework require separate scopes not requested here"),
        GoogleServiceDefinition(GoogleProduct.KEEP,"Google Keep",null,"Launch the installed Keep app","Consumer Keep data access is not assumed; enterprise APIs require separate administrator eligibility")
    )
    fun definition(product:GoogleProduct)=services.single{it.product==product}
    fun accountId(email:String):String {require(validAccount(email));return MessageDigest.getInstance("SHA-256").digest(email.lowercase(java.util.Locale.ROOT).toByteArray()).joinToString(""){"%02x".format(it)}}
    fun validAccount(email:String)=email.length in 3..254 && email.matches(Regex("[^\\s@\\p{Cntrl}]+@[^\\s@\\p{Cntrl}]+"))
    fun validateResource(id:String):String {require(id.matches(Regex("[A-Za-z0-9_-]{1,180}"))){"Use the exact authorized Google file identifier, without URL parameters."};return id}
    fun preview(text:String,max:Int=500)=ToolPolicy.sensitiveText(text.filter{!it.isISOControl() || it=='\n'}.take(max))
}
enum class GoogleQueryKind { RECENT,UNREAD,UNREAD_MESSAGES,TODAY,IMPORTANT,TEACHER,PROJECTS,DETAIL }
data class GoogleQuery(val product:GoogleProduct,val kind:GoogleQueryKind=GoogleQueryKind.RECENT,val resourceId:String?=null,val sender:String?=null)
object GoogleCommandParser {
    /** Unsupported private-mail reads stay out of general AI; no invented inbox answer. */
    fun requiresNativeRead(raw:String):Boolean {
        val text=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return parse(raw)!=null || com.jarvis.assistant.rebirth.RemovedMailIntegration.matches(raw) ||
            Regex("^(?:please )?(?:read|show|summarize|summarise|check|search|find) (?:my |the |all )?(?:gmail|emails?|inbox)(?: .*)?$").matches(text) ||
            Regex("^(?:did|has) my .+ (?:email|emailed) me(?: .*)?$").matches(text)
    }
    fun parse(raw:String):GoogleQuery? {
        val text=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return when(text){
            "check my gmail","summarize my inbox","catch me up on my emails","catch me up on emails","summarize my gmail","check my email","show my email","read my latest email","read my last email","check my last email","what email did i just get","what s my newest gmail","whats my newest gmail","who emailed me","summarize the last email","summarize my latest email","lee mi ultimo correo","revisa mi correo","resume el ultimo correo"->GoogleQuery(GoogleProduct.GMAIL)
            "show my unread messages","show unread emails","muestra correos sin leer"->GoogleQuery(GoogleProduct.GMAIL,GoogleQueryKind.UNREAD_MESSAGES)
            "did my teacher email me","has my teacher emailed me"->GoogleQuery(GoogleProduct.GMAIL,GoogleQueryKind.TEACHER)
            "how many unread emails do i have","count my unread emails","unread gmail count"->GoogleQuery(GoogleProduct.GMAIL,GoogleQueryKind.UNREAD)
            "summarize today s messages","summarize today s emails","summarize todays emails","summarize today s gmail"->GoogleQuery(GoogleProduct.GMAIL,GoogleQueryKind.TODAY)
            "what s my most important email","whats my most important email","show important emails"->GoogleQuery(GoogleProduct.GMAIL,GoogleQueryKind.IMPORTANT)
            "what projects am i working on","find my latest project","show my project folders"->GoogleQuery(GoogleProduct.DRIVE,GoogleQueryKind.PROJECTS)
            "which files did i recently edit","show my recent google files","show recent drive files"->GoogleQuery(GoogleProduct.DRIVE)
            "my google calendar today","show my google calendar"->GoogleQuery(GoogleProduct.CALENDAR,GoogleQueryKind.TODAY)
            "show my google tasks","what are my google tasks"->GoogleQuery(GoogleProduct.TASKS)
            else->null
        }
    }
    fun navigation(raw:String)=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ") in setOf("open account center","open my account settings","open account settings","open google setup","open connected accounts","open ai and connected accounts")
}
class GoogleServiceFailure(val category:String,val status:Int?=null,message:String):Exception(message)
class GoogleReadResult(val product:GoogleProduct,val summary:String,val items:List<GoogleReadItem> = emptyList(),val unreadCount:Long?=null) {override fun toString()="GoogleReadResult(product=$product, content=redacted)"}
data class GoogleReadItem(val id:String,val title:String,val detail:String,val mimeType:String?=null)
