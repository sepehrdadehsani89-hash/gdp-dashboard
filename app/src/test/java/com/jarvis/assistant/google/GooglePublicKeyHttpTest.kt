package com.jarvis.assistant.google

import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class GooglePublicKeyHttpTest {
    private val server=MockWebServer();private val key="AIza"+"T".repeat(35)
    private fun client()=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).readTimeout(250,TimeUnit.MILLISECONDS).callTimeout(2,TimeUnit.SECONDS).build()
    private fun reader()=GooglePublicKeyTest(client(),server.url("/youtube/v3/i18nLanguages"))
    private fun body(items:String="""[{"id":"en","snippet":{"name":"English"}}]""")="""{"kind":"youtube#i18nLanguageListResponse","items":$items}"""
    private fun enqueue(text:String=body(),code:Int=200){server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type","application/json").setBody(text))}
    private fun rejected(category:String,action:()->Unit={reader().read(key)}){val e=assertThrows(GoogleKeyFailure::class.java,action);assertEquals(category,e.category);assertFalse(e.message.orEmpty().contains(key))}
    @Before fun start(){server.start()}
    @After fun stop(){server.shutdown()}
    @Test fun realFixtureRequestParsesMetadata(){enqueue();assertEquals(1,reader().read(key).languages)}
    @Test fun realRequestUsesOnlyPublicLanguageEndpoint(){enqueue();reader().read(key);val r=server.takeRequest();assertEquals("/youtube/v3/i18nLanguages",r.requestUrl!!.encodedPath);assertEquals("snippet",r.requestUrl!!.queryParameter("part"))}
    @Test fun apiKeyNeverBecomesABearerToken(){enqueue();reader().read(key);val r=server.takeRequest();assertNull(r.getHeader("Authorization"));assertEquals(key,r.requestUrl!!.queryParameter("key"))}
    @Test fun androidRestrictionIdentityUsesActualHeaderFields(){enqueue();reader().read(key,"com.jarvis.next.phoenix","A".repeat(40));val r=server.takeRequest();assertEquals("com.jarvis.next.phoenix",r.getHeader("X-Android-Package"));assertEquals("A".repeat(40),r.getHeader("X-Android-Cert"))}
    @Test fun invalidAndroidHeaderNeverLeavesDevice(){rejected("ANDROID_IDENTITY_FORMAT"){reader().read(key,"com.jarvis.next.phoenix",null)};assertEquals(0,server.requestCount)}
    @Test fun oauthTokenIsNotAcceptedAsApiKey(){rejected("KEY_FORMAT"){reader().read("ya29.invalid")};assertEquals(0,server.requestCount)}
    @Test fun blankKeyNeverSendsNetworkRequest(){rejected("KEY_FORMAT"){reader().read("")};assertEquals(0,server.requestCount)}
    @Test fun authenticationFailureIsNotAConnection(){enqueue(code=401);rejected("AUTHENTICATION")}
    @Test fun apiConfigurationFailureIsNotAConnection(){enqueue(code=403);rejected("API_KEY_OR_API_CONFIGURATION")}
    @Test fun quotaFailureIsNotRetried(){enqueue(code=429);rejected("QUOTA");assertEquals(1,server.requestCount)}
    @Test fun serverFailureIsNotRetried(){enqueue(code=500);rejected("HTTP_500");assertEquals(1,server.requestCount)}
    @Test fun redirectsDoNotForwardTheKey(){server.enqueue(MockResponse().setResponseCode(302).setHeader("Location",server.url("/other")));rejected("REDIRECT_REJECTED");assertEquals(1,server.requestCount)}
    @Test fun invalidJsonIsRejected(){enqueue("not json");rejected("INVALID_RESPONSE")}
    @Test fun missingItemsIsRejected(){enqueue("{}");rejected("INVALID_RESPONSE")}
    @Test fun emptyMetadataDoesNotBecomeVerified(){enqueue(body("[]"));rejected("INVALID_RESPONSE")}
    @Test fun missingRecordIdIsRejected(){enqueue(body("""[{"snippet":{"name":"English"}}]"""));rejected("INVALID_RESPONSE")}
    @Test fun missingRecordNameIsRejected(){enqueue(body("""[{"id":"en","snippet":{}}]"""));rejected("INVALID_RESPONSE")}
    @Test fun wrongRecordTypeIsRejected(){enqueue(body("[1]"));rejected("INVALID_RESPONSE")}
    @Test fun unrelatedJsonCannotLookConnected(){enqueue("""{"kind":"different","items":[{"id":"en","snippet":{"name":"English"}}]}""");rejected("INVALID_RESPONSE")}
    @Test fun htmlIsNotAnApiProof(){server.enqueue(MockResponse().setHeader("Content-Type","text/html").setBody(body()));rejected("INVALID_RESPONSE")}
    @Test fun boundedBodyIsEnforced(){enqueue("x".repeat(128*1024+1));rejected("RESPONSE_TOO_LARGE")}
    @Test fun timeoutIsReportedWithoutAutomaticRetry(){server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));rejected("TIMEOUT");assertEquals(1,server.requestCount)}
    @Test fun maliciousInjectedEndpointIsRejected(){assertThrows(IllegalArgumentException::class.java){GooglePublicKeyTest(client(),"https://example.com/".toHttpUrl())}}
    @Test fun unsafeRedirectingClientIsRejected(){assertThrows(IllegalArgumentException::class.java){GooglePublicKeyTest(OkHttpClient(),server.url("/"))}}
}
