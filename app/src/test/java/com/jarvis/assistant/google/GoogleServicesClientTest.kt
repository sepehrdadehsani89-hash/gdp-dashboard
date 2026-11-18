package com.jarvis.assistant.google

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class GoogleServicesClientTest {
    private lateinit var server:MockWebServer
    private lateinit var client:GoogleServicesClient
    private val clock=Clock.fixed(Instant.parse("2026-03-08T16:00:00Z"),ZoneId.of("America/New_York"))
    @Before fun setup(){server=MockWebServer();server.start();client=GoogleServicesClient(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(3,TimeUnit.SECONDS).build(),clock,server.url("/"))}
    @After fun close(){server.shutdown()}
    private fun enqueue(body:String,status:Int=200){server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type","application/json").setBody(body))}
    private fun profile(email:String="owner@example.com")=enqueue("""{"emailAddress":"$email"}""")
    private suspend fun read(product:GoogleProduct=GoogleProduct.GMAIL,kind:GoogleQueryKind=GoogleQueryKind.RECENT,id:String?=null,sender:String?=null)=client.read(GoogleAccessSession("a".repeat(64),product,"test-access-token",1),GoogleQuery(product,kind,id,sender),"owner@example.com")
    private fun failure(status:Int,expected:String)=runBlocking{enqueue("""{"error":{"message":"SECRET-TOKEN-IN-RAW-BODY"}}""",status);val e=try{read(GoogleProduct.TASKS);error("Expected rejection")}catch(e:GoogleServiceFailure){e};assertEquals(expected,e.category);assertEquals(status,e.status);assertFalse(e.message!!.contains("SECRET"))}
    @Test fun emailAccountMismatchStopsBeforeMailboxRead()=runBlocking{profile("other@example.com");val e=try{read();error("Expected mismatch")}catch(e:GoogleServiceFailure){e};assertEquals("ACCOUNT_MISMATCH",e.category);assertEquals(1,server.requestCount)}
    @Test fun unreadCountComesFromUnreadLabelNotAnEstimate()=runBlocking{profile();enqueue("""{"messagesUnread":17,"messagesTotal":99}""");val r=read(kind=GoogleQueryKind.UNREAD);assertTrue(r.summary.contains("17 unread"));server.takeRequest();assertEquals("/gmail/v1/users/me/labels/UNREAD",server.takeRequest().path)}
    @Test fun missingUnreadCountIsNotInventedAsZero()=runBlocking{profile();enqueue("{}");val e=try{read(kind=GoogleQueryKind.UNREAD);error("Expected invalid response")}catch(e:GoogleServiceFailure){e};assertEquals("INVALID_RESPONSE",e.category)}
    @Test fun emptyMailQueryReportsNoEntries()=runBlocking{profile();enqueue("""{"resultSizeEstimate":0}""");val r=read();assertTrue(r.items.isEmpty());assertTrue(r.summary.contains("No entries were returned"))}
    @Test fun previewsShowRealSenderSubjectDateWithoutMarkingRead()=runBlocking{profile();enqueue("""{"messages":[{"id":"msg-one"}]}""");enqueue("""{"id":"msg-one","snippet":"Actual preview","payload":{"headers":[{"name":"From","value":"Teacher <teacher@example.com>"},{"name":"Subject","value":"Assignment"},{"name":"Date","value":"Monday"}]}}""");val r=read();assertEquals("Assignment",r.items.single().title);assertTrue(r.summary.contains("teacher@example.com"));assertTrue(r.summary.contains("Monday"));assertTrue(r.summary.contains("Actual preview"));repeat(3){assertEquals("GET",server.takeRequest().method)}}
    @Test fun gmailReadsMetadataOnlyAndSelectsRequiredHeaders()=runBlocking{profile();enqueue("""{"messages":[{"id":"msg-one"}]}""");enqueue("{}");read();server.takeRequest();server.takeRequest();val req=server.takeRequest();assertEquals("metadata",req.requestUrl!!.queryParameter("format"));assertEquals(listOf("From","Subject","Date"),req.requestUrl!!.queryParameterValues("metadataHeaders"))}
    @Test fun messageIdsCannotRedirectCredentialRequests()=runBlocking{profile();enqueue("""{"messages":[{"id":"https://evil.example/"}]}""");try{read();fail("Invalid id accepted")}catch(_:IllegalArgumentException){};assertEquals(2,server.requestCount)}
    @Test fun importantMessagesUseActualGmailLabel()=runBlocking{profile();enqueue("{}");val r=read(kind=GoogleQueryKind.IMPORTANT);server.takeRequest();assertEquals("IMPORTANT",server.takeRequest().requestUrl!!.queryParameter("labelIds"));assertTrue(r.summary.contains("not a personal urgency judgment"))}
    @Test fun unreadPreviewsUseUnreadFilter()=runBlocking{profile();enqueue("{}");read(kind=GoogleQueryKind.UNREAD_MESSAGES);server.takeRequest();assertEquals("UNREAD",server.takeRequest().requestUrl!!.queryParameter("labelIds"))}
    @Test fun todayUsesActualLocalDayAcrossDST()=runBlocking{profile();enqueue("{}");read(kind=GoogleQueryKind.TODAY);server.takeRequest();val q=server.takeRequest().requestUrl!!.queryParameter("q")!!;assertEquals("after:1772946000 before:1773028800",q)}
    @Test fun teacherIdentityMustBeConfigured()=runBlocking{profile();val e=try{read(kind=GoogleQueryKind.TEACHER);error("Expected configured sender")}catch(e:GoogleServiceFailure){e};assertEquals("USER_ACTION_REQUIRED",e.category);assertEquals(1,server.requestCount)}
    @Test fun teacherFilterUsesOnlyConfiguredAddress()=runBlocking{profile();enqueue("{}");read(kind=GoogleQueryKind.TEACHER,sender="teacher@example.com");server.takeRequest();assertEquals("from:\"teacher@example.com\"",server.takeRequest().requestUrl!!.queryParameter("q"))}
    @Test fun bearerTokenIsOnlyInHeader()=runBlocking{enqueue("{}");read(GoogleProduct.TASKS);val req=server.takeRequest();assertEquals("Bearer test-access-token",req.getHeader("Authorization"));assertFalse(req.path!!.contains("token"));assertEquals(0L,req.bodySize)}
    @Test fun deniedAuthenticationIsTypedAndRedacted(){failure(401,"AUTHENTICATION")}
    @Test fun disabledAPIOrScopeIsTypedAndRedacted(){failure(403,"AUTHORIZATION")}
    @Test fun quotaFailureDoesNotAutomaticallyRetry(){failure(429,"QUOTA");assertEquals(1,server.requestCount)}
    @Test fun serviceFailureDoesNotAutomaticallyRetry(){failure(503,"SERVICE");assertEquals(1,server.requestCount)}
    @Test fun missingResourceIsNotCalledAnAuthError(){failure(404,"HTTP")}
    @Test fun redirectsAreNotFollowedAndNeverBecomeSuccess(){failure(302,"HTTP");assertEquals(1,server.requestCount)}
    @Test fun malformedJsonIsNotASuccessfulRead()=runBlocking{enqueue("<html>private upstream error</html>");val e=try{read(GoogleProduct.TASKS);error("Expected invalid response")}catch(e:GoogleServiceFailure){e};assertEquals("INVALID_RESPONSE",e.category);assertFalse(e.message!!.contains("upstream"))}
    @Test fun oversizedResponseIsRejected()=runBlocking{enqueue(" ".repeat(2_000_001));val e=try{read(GoogleProduct.TASKS);error("Expected bounded response")}catch(e:GoogleServiceFailure){e};assertEquals("INVALID_RESPONSE",e.category)}
    @Test fun fileDiscoveryRequestsMetadataOnly()=runBlocking{enqueue("""{"files":[{"id":"file-one","name":"Actual plan","mimeType":"application/vnd.google-apps.document","modifiedTime":"Tuesday"}],"nextPageToken":"more"}""");val r=read(GoogleProduct.DRIVE);assertEquals("Actual plan",r.items.single().title);val req=server.takeRequest();assertEquals("nextPageToken,files(id,name,mimeType,modifiedTime)",req.requestUrl!!.queryParameter("fields"));assertTrue(r.summary.contains("Nothing was indexed automatically"));assertTrue(r.summary.contains("More results exist"))}
    @Test fun projectFolderNamesAreNotEvidenceOfCurrentWork()=runBlocking{enqueue("{}");val r=read(GoogleProduct.DRIVE,GoogleQueryKind.PROJECTS);assertTrue(r.summary.contains("do not establish"));assertTrue(server.takeRequest().requestUrl!!.queryParameter("q")!!.contains("application/vnd.google-apps.folder"))}
    @Test fun calendarReadsLocalWindowAndExpandsRecurringEvents()=runBlocking{enqueue("{}");read(GoogleProduct.CALENDAR);val q=server.takeRequest().requestUrl!!;assertEquals("2026-03-08T05:00:00Z",q.queryParameter("timeMin"));assertEquals("2026-03-09T04:00:00Z",q.queryParameter("timeMax"));assertEquals("true",q.queryParameter("singleEvents"))}
    @Test fun taskReadDoesNotIncludeCompletedDeletedTasks()=runBlocking{enqueue("{}");read(GoogleProduct.TASKS);val q=server.takeRequest().requestUrl!!;assertEquals("false",q.queryParameter("showCompleted"));assertEquals("false",q.queryParameter("showDeleted"))}
    @Test fun contactsRequestNamesNotAddressesOrPhoneNumbers()=runBlocking{enqueue("{}");read(GoogleProduct.CONTACTS);assertEquals("names",server.takeRequest().requestUrl!!.queryParameter("personFields"))}
    @Test fun docsRequireExplicitFileSelection()=runBlocking{val e=try{read(GoogleProduct.DOCS);error("Expected explicit file id")}catch(e:GoogleServiceFailure){e};assertEquals("USER_ACTION_REQUIRED",e.category);assertEquals(0,server.requestCount)}
    @Test fun docsReadsOnlySelectedDocumentText()=runBlocking{enqueue("""{"documentId":"approved-id","title":"Actual document","body":{"content":[{"paragraph":{"elements":[{"textRun":{"content":"Document paragraph"}}]}}]}}""");val r=read(GoogleProduct.DOCS,id="approved-id");assertTrue(r.summary.contains("Document paragraph"));assertEquals("/v1/documents/approved-id",server.takeRequest().path)}
    @Test fun sheetsIsBoundedToFirstTwentyRowsAndColumns()=runBlocking{enqueue("""{"spreadsheetId":"sheet-id","properties":{"title":"Budget"}}""");enqueue("""{"values":[["Item","Cost"],["Coffee","3"]]}""");val r=read(GoogleProduct.SHEETS,id="sheet-id");assertTrue(r.summary.contains("Coffee | 3"));server.takeRequest();assertEquals("/v4/spreadsheets/sheet-id/values/A1:T20",server.takeRequest().path)}
    @Test fun apiLessProductsNeverSendCredentials()=runBlocking{val e=try{read(GoogleProduct.KEEP);error("Expected unsupported")}catch(e:GoogleServiceFailure){e};assertEquals("UNSUPPORTED",e.category);assertEquals(0,server.requestCount)}
    @Test fun serviceTokenCannotBeUsedForAnotherProduct()=runBlocking{try{client.read(GoogleAccessSession("a",GoogleProduct.GMAIL,"secret",0),GoogleQuery(GoogleProduct.DRIVE),"owner@example.com");fail("Cross-service token used")}catch(_:IllegalArgumentException){};assertEquals(0,server.requestCount)}
    @Test fun diagnosticSessionDoesNotExposeToken(){assertFalse(GoogleAccessSession("a",GoogleProduct.GMAIL,"secret-token",0).toString().contains("secret-token"))}
    @Test fun callerCancellationStopsTheReadWithoutRetry()=runBlocking{server.enqueue(MockResponse().setBody("{}").setBodyDelay(2,TimeUnit.SECONDS));val job=launch{read(GoogleProduct.TASKS)};withContext(Dispatchers.IO){assertNotNull(server.takeRequest(2,TimeUnit.SECONDS))};job.cancelAndJoin();assertTrue(job.isCancelled);assertEquals(1,server.requestCount)}
    @Test fun productionEndpointOverrideCannotTargetAnExternalHost(){assertThrows(IllegalArgumentException::class.java){GoogleServicesClient(OkHttpClient(),clock,okhttp3.HttpUrl.Builder().scheme("http").host("evil.example").build())}}
}
