package com.jarvis.assistant.brain.auth

import java.io.Closeable
import java.net.Socket
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Mock OAuth server/identity validator with real loopback sockets; no live account proof. */
class OpenAiAuthManagerTest {
    private class MemoryStorage:OpenAiAuthStorage {
        private val entries=mutableMapOf<String,String>()
        @Synchronized override fun read(name:String)=entries[name]
        @Synchronized override fun write(name:String,value:String){entries[name]=value}
        @Synchronized override fun remove(name:String){entries.remove(name)}
        @Synchronized fun clear(){entries.clear()}
    }
    private class Fixture(
        val storage:MemoryStorage=MemoryStorage(),
        val identity:suspend(String,String,String)->ValidatedOpenAiIdentity={_,_,_->ValidatedOpenAiIdentity("subject-1","owner@example.test","Test Owner")},
        timeout:Long=5000,
    ):Closeable {
        val server=MockWebServer().apply{start()}
        val calls=mutableListOf<Triple<String,String,String>>()
        val manager=OpenAiAuthManager(storage,OkHttpClient(),OpenAiOAuthEndpoints(
            token=server.url("/token").toString(),revoke=server.url("/revoke").toString()),{NOW},timeout){token,client,nonce->
            synchronized(calls){calls+=Triple(token,client,nonce)}
            identity(token,client,nonce)
        }
        override fun close(){manager.cancelSignIn();server.close()}
        fun token(body:JSONObject=tokens()) {server.enqueue(MockResponse().setBody(body.toString()))}
        fun start(account:String?=null)=manager.beginSignIn(account).toHttpUrl()
        fun callback(auth:HttpUrl,query:String=validQuery(auth),host:String?=null,path:String="/auth/callback"):String {
            val redirect=URI(auth.queryParameter("redirect_uri")!!)
            return Socket("127.0.0.1",redirect.port).use{socket->
                socket.soTimeout=3000
                socket.getOutputStream().write("GET $path?$query HTTP/1.1\r\nHost: ${host ?: "127.0.0.1:${redirect.port}"}\r\nConnection: close\r\n\r\n".toByteArray())
                socket.getOutputStream().flush()
                socket.getInputStream().readBytes().toString(Charsets.UTF_8)
            }
        }
        suspend fun settled(){withTimeout(5000){while(manager.state.value.signingIn)delay(5)}}
        suspend fun signIn():HttpUrl {token();val auth=start();assertTrue(callback(auth).startsWith("HTTP/1.1 200"));settled();return auth}
    }
    private fun seed(storage:MemoryStorage,scope:Set<String> = PLAN,expires:Long=NOW-1000,refresh:String="old-refresh",nonce:String="original-nonce",gate:Long=0L){
        storage.write("brain_openai_host","urn:uuid:test-installation")
        val account=JSONObject().put("id","account-1").put("client_id",CLIENT).put("subject","subject-1")
            .put("email","owner@example.test").put("name","Test Owner").put("id_token","old-id")
            .put("access_token","old-access").put("refresh_token",refresh).put("scopes",JSONArray(scope.toList()))
            .put("expires_at",expires).put("earliest_refresh_at",gate).put("revision",7L).put("nonce",nonce)
        storage.write("brain_openai_accounts",JSONObject().put("selected","account-1").put("accounts",JSONArray().put(account)).toString())
    }
    private suspend fun failure(block:suspend()->Any?):Throwable {
        val error=runCatching{block()}.exceptionOrNull()
        assertNotNull("Expected authorization failure",error)
        return error!!
    }

    @Test fun publicRegistrationUsesOfficialAuthorityAndPKCE(){Fixture().use{f->
        val auth=f.start()
        assertEquals("https",auth.scheme);assertEquals("auth.openai.com",auth.host)
        assertEquals("/api/accounts/authorize",auth.encodedPath)
        assertEquals("dynamic_agent_client",auth.queryParameter("client_id"))
        assertEquals("code",auth.queryParameter("response_type"));assertEquals("S256",auth.queryParameter("code_challenge_method"))
        assertEquals("J.A.R.V.I.S.",auth.queryParameter("agent_name_hint"))
        assertEquals("https://api.openai.com/v1",auth.queryParameter("resource"))
        assertTrue(auth.queryParameter("scope")!!.split(' ').containsAll(PLAN))
        assertNull(auth.queryParameter("client_secret"))
    }}
    @Test fun callbackIsBoundBeforeBrowserOnNumericLoopbackOnly(){Fixture().use{f->
        val auth=f.start();val uri=URI(auth.queryParameter("redirect_uri")!!)
        assertEquals("http",uri.scheme);assertEquals("127.0.0.1",uri.host);assertEquals("/auth/callback",uri.path)
        assertTrue(uri.port>0);assertTrue(f.callback(auth,"state=wrong").startsWith("HTTP/1.1 400"))
    }}
    @Test fun everyAttemptUsesFreshStateNonceAndChallenge(){Fixture().use{f->
        val a=f.start();val b=f.start()
        listOf("state","nonce","code_challenge").forEach{name->assertTrue(a.queryParameter(name)!!.length>=43);assertNotEquals(a.queryParameter(name),b.queryParameter(name))}
    }}
    @Test fun installationHostSurvivesNewAttempts(){Fixture().use{f->assertEquals(f.start().queryParameter("ext_agent_host_id"),f.start().queryParameter("ext_agent_host_id"))}}
    @Test fun validCallbackUsesIssuedClientAndExactRedirectInCodeExchange()=runBlocking<Unit>{Fixture().use{f->
        val auth=f.signIn();val req=f.server.takeRequest(2,TimeUnit.SECONDS)!!;val form=form(req)
        assertEquals("authorization_code",form.queryParameter("grant_type"));assertEquals(CLIENT,form.queryParameter("client_id"))
        assertEquals(auth.queryParameter("redirect_uri"),form.queryParameter("redirect_uri"));assertEquals("one-time-code",form.queryParameter("code"))
        assertTrue(form.queryParameter("code_verifier")!!.length>=43);assertNull(form.queryParameter("client_secret"))
        assertEquals("https://api.openai.com/v1",form.queryParameter("resource"));assertFalse(req.body.toString().contains("dynamic_agent_client"))
    }}
    @Test fun callbackValidatesIDWithIssuedClientAndTransactionNonce()=runBlocking<Unit>{Fixture().use{f->
        val auth=f.signIn();assertEquals(Triple("signed-id",CLIENT,auth.queryParameter("nonce")!!),f.calls.single())
        assertTrue(f.manager.state.value.accounts.single().planAuthorized)
        assertEquals("actual-access",f.manager.bearerToken())
    }}
    @Test fun callbackHTMLNeverReflectsCodeTokensOrState()=runBlocking<Unit>{Fixture().use{f->
        f.token();val auth=f.start();val html=f.callback(auth);f.settled()
        listOf("one-time-code","actual-access","refresh-secret",auth.queryParameter("state")!!).forEach{assertFalse(html.contains(it))}
        assertTrue(html.contains("Cache-Control: no-store"));assertTrue(html.contains("default-src 'none'"));assertFalse(html.contains("<script"))
    }}
    @Test fun wrongStateDoesNotExchangeOrEndPendingTransaction()=runBlocking<Unit>{Fixture().use{f->
        val auth=f.start();assertTrue(f.callback(auth,"state=incorrect&code=stolen&client_id=$CLIENT").startsWith("HTTP/1.1 400"))
        assertTrue(f.manager.state.value.signingIn);assertEquals(0,f.server.requestCount)
        f.token();f.callback(auth);f.settled();assertEquals(1,f.manager.state.value.accounts.size)
    }}
    @Test fun duplicateQueryParametersAreRejectedWithoutTokenRequest(){Fixture().use{f->
        val auth=f.start();assertTrue(f.callback(auth,validQuery(auth)+"&code=duplicate").startsWith("HTTP/1.1 400"));assertEquals(0,f.server.requestCount)
    }}
    @Test fun nonLoopbackHostHeaderIsRejected(){Fixture().use{f->val a=f.start();assertTrue(f.callback(a,host="attacker.test").startsWith("HTTP/1.1 400"));assertEquals(0,f.server.requestCount)}}
    @Test fun anotherCallbackPathIsRejected(){Fixture().use{f->val a=f.start();assertTrue(f.callback(a,path="/untrusted").startsWith("HTTP/1.1 400"));assertEquals(0,f.server.requestCount)}}
    @Test fun userDenialIsCancellationWithoutCredentials()=runBlocking<Unit>{Fixture().use{f->
        val a=f.start();f.callback(a,"state=${a.queryParameter("state")}&error=access_denied");f.settled()
        assertTrue(f.manager.state.value.accounts.isEmpty());assertTrue(f.manager.state.value.message!!.contains("canceled"));assertEquals(0,f.server.requestCount)
    }}
    @Test fun newRegistrationCannotExchangeWithoutIssuedClientID()=runBlocking<Unit>{Fixture().use{f->
        val a=f.start();f.callback(a,"state=${a.queryParameter("state")}&code=code");f.settled();assertEquals(0,f.server.requestCount);assertTrue(f.manager.state.value.accounts.isEmpty())
    }}
    @Test fun bootstrapClientCanNeverBeUsedForTokenExchange()=runBlocking<Unit>{Fixture().use{f->
        val a=f.start();f.callback(a,"state=${a.queryParameter("state")}&code=code&client_id=dynamic_agent_client");f.settled();assertEquals(0,f.server.requestCount)
    }}
    @Test fun returningRegistrationUsesIssuedClientWithoutNewAgentHint(){val s=MemoryStorage();seed(s);Fixture(s).use{f->
        val a=f.start("account-1");assertEquals(CLIENT,a.queryParameter("client_id"));assertNull(a.queryParameter("agent_name_hint"));assertEquals("old-id",a.queryParameter("id_token_hint"))
    }}
    @Test fun returningRegistrationRejectsDifferentIssuedClientID()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        val a=f.start("account-1");f.callback(a,"state=${a.queryParameter("state")}&code=code&client_id=oaiapp_other123");f.settled();assertEquals(0,f.server.requestCount)
    }}
    @Test fun returningRegistrationRejectsDifferentSubject()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s,identity={_,_,_->ValidatedOpenAiIdentity("different-subject")}).use{f->
        f.token();val a=f.start("account-1");f.callback(a);f.settled();assertEquals("subject-1",f.manager.credentialDescriptor()!!.subject);assertTrue(f.manager.state.value.message!!.contains("does not match"))
    }}
    @Test fun signedIdentityAloneDoesNotAuthorizeInference()=runBlocking<Unit>{Fixture().use{f->
        f.token(tokens("openid profile email"));val a=f.start();f.callback(a);f.settled()
        assertTrue(f.manager.state.value.accounts.single().signedIn);assertFalse(f.manager.state.value.accounts.single().planAuthorized)
        assertNull(f.manager.credentialDescriptor());failure{f.manager.bearerToken()};assertEquals(1,f.server.requestCount)
    }}
    @Test fun cancelClosesListenerAndDoesNotSaveCredentials(){Fixture().use{f->
        val a=f.start();f.manager.cancelSignIn();assertFalse(f.manager.state.value.signingIn)
        assertTrue(runCatching{f.callback(a)}.isFailure);assertTrue(f.manager.state.value.accounts.isEmpty())
    }}
    @Test fun processRestartDoesNotRestorePendingState(){val s=MemoryStorage();Fixture(s).use{a->
        a.start();Fixture(s).use{b->assertFalse(b.manager.state.value.signingIn);assertTrue(b.manager.state.value.accounts.isEmpty())}
    }}
    @Test fun transactionDeadlineEndsWithoutFabricatedAccount()=runBlocking<Unit>{Fixture(timeout=50).use{f->f.start();f.settled();assertTrue(f.manager.state.value.accounts.isEmpty());assertTrue(f.manager.state.value.message!!.contains("timed out"))}}
    @Test fun healthyRefreshRotatesTokensWithoutInvalidatingProofRevision()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.token(tokens().apply{remove("id_token")});assertEquals("actual-access",f.manager.bearerToken())
        assertEquals(7L,f.manager.credentialDescriptor()!!.revision);val req=f.server.takeRequest(2,TimeUnit.SECONDS)!!;val body=form(req)
        assertEquals("refresh_token",body.queryParameter("grant_type"));assertEquals("old-refresh",body.queryParameter("refresh_token"));assertNull(body.queryParameter("scope"))
        assertTrue(s.read("brain_openai_accounts")!!.contains("refresh-secret"))
    }}
    @Test fun refreshWithoutScopeOrNewRefreshRetainsExistingGrant()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.token(tokens().apply{remove("id_token");remove("scope");remove("refresh_token")});assertEquals("actual-access",f.manager.bearerToken())
        assertEquals(PLAN,f.manager.credentialDescriptor()!!.scopes);assertEquals(7L,f.manager.credentialDescriptor()!!.revision)
        assertTrue(s.read("brain_openai_accounts")!!.contains("old-refresh"))
    }}
    @Test fun reducedScopesInvalidateRevisionAndDenyPlanAccess()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.token(tokens("openid profile").apply{remove("id_token")});failure{f.manager.bearerToken()}
        assertFalse(f.manager.state.value.accounts.single().planAuthorized);assertTrue(f.manager.state.value.accounts.single().revision>7)
    }}
    @Test fun newRefreshIDIsValidatedAgainstOriginalNonceAndSubject()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.token();assertEquals("actual-access",f.manager.bearerToken());assertEquals(Triple("signed-id",CLIENT,"original-nonce"),f.calls.single());assertEquals(7L,f.manager.credentialDescriptor()!!.revision)
    }}
    @Test fun refreshedSubjectSubstitutionCannotReplaceSavedCredentials()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s,identity={_,_,_->ValidatedOpenAiIdentity("different")}).use{f->
        f.token();failure{f.manager.bearerToken()};assertTrue(s.read("brain_openai_accounts")!!.contains("old-access"));assertFalse(s.read("brain_openai_accounts")!!.contains("actual-access"))
    }}
    @Test fun invalidRefreshIDSignatureCannotReplaceSavedCredentials()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s,identity={_,_,_->throw OpenAiIdTokenValidationException()}).use{f->
        f.token();failure{f.manager.bearerToken()};assertTrue(s.read("brain_openai_accounts")!!.contains("old-access"))
    }}
    @Test fun expiredTokenWithoutRefreshFailsWithoutNetwork()=runBlocking<Unit>{val s=MemoryStorage();seed(s,refresh="");Fixture(s).use{f->failure{f.manager.bearerToken()};assertEquals(0,f.server.requestCount)}}
    @Test fun terminalRefreshErrorClearsCredentialsButPreservesRegistration()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""));failure{f.manager.bearerToken()}
        assertFalse(f.manager.state.value.accounts.single().signedIn);assertFalse(s.read("brain_openai_accounts")!!.contains("old-access"));assertTrue(s.read("brain_openai_accounts")!!.contains(CLIENT))
    }}
    @Test fun transientRefreshFailurePreservesSavedCredentialForExplicitRetry()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"temporarily_unavailable"}"""));failure{f.manager.bearerToken()}
        assertTrue(s.read("brain_openai_accounts")!!.contains("old-refresh"));assertEquals(7L,f.manager.credentialDescriptor()!!.revision);assertEquals(1,f.server.requestCount)
    }}
    @Test fun successfulRevocationClearsLocalSecretsAndRetainsAccountRegistration()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.server.enqueue(MockResponse().setResponseCode(200));val result=f.manager.disconnect();assertTrue(result.remoteRevocationConfirmed)
        val req=f.server.takeRequest(2,TimeUnit.SECONDS)!!;assertEquals("/revoke",req.path);val body=form(req)
        assertEquals("old-refresh",body.queryParameter("token"));assertEquals(CLIENT,body.queryParameter("client_id"));assertEquals("refresh_token",body.queryParameter("token_type_hint"))
        assertFalse(f.manager.state.value.accounts.single().signedIn);assertNull(f.manager.credentialDescriptor());assertTrue(s.read("brain_openai_accounts")!!.contains(CLIENT));assertFalse(s.read("brain_openai_accounts")!!.contains("old-refresh"))
    }}
    @Test fun failedRemoteRevocationIsReportedHonestlyAfterLocalDisconnect()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.server.enqueue(MockResponse().setResponseCode(503));val result=f.manager.disconnect();assertFalse(result.remoteRevocationConfirmed)
        assertTrue(result.message.contains("not confirmed"));assertEquals(result.message,f.manager.state.value.message);assertFalse(f.manager.state.value.accounts.single().signedIn)
    }}
    @Test fun tokenEndpointRedirectIsNotFollowed()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->MockWebServer().use{sink->
        f.server.enqueue(MockResponse().setResponseCode(307).setHeader("Location",sink.url("/steal")))
        failure{f.manager.bearerToken()};assertEquals(0,sink.requestCount);assertEquals(1,f.server.requestCount)
    }}}
    @Test fun revokeRedirectIsNotFollowed()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->MockWebServer().use{sink->
        f.server.enqueue(MockResponse().setResponseCode(307).setHeader("Location",sink.url("/steal")))
        assertFalse(f.manager.disconnect().remoteRevocationConfirmed);assertEquals(0,sink.requestCount)
    }}}
    @Test fun wipeDuringRefreshCannotRestoreRemovedCredentials()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        f.server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {started.countDown();release.await(3,TimeUnit.SECONDS);return MockResponse().setBody(tokens().apply{remove("id_token")}.toString())}}
        val pending=async(Dispatchers.IO){runCatching{f.manager.bearerToken()}}
        assertTrue(started.await(3,TimeUnit.SECONDS));s.clear();release.countDown();assertTrue(pending.await().isFailure)
        assertNull(s.read("brain_openai_accounts"));assertNull(f.manager.credentialDescriptor())
    }}
    @Test fun healthyTokenDoesNotRequestRefreshBeforeNeeded()=runBlocking<Unit>{val s=MemoryStorage();seed(s,expires=NOW+3600000);Fixture(s).use{f->assertEquals("old-access",f.manager.bearerToken());assertEquals(0,f.server.requestCount)}}
    @Test fun undocumentedLegacyRefreshTimestampCannotFreezeRefresh()=runBlocking<Unit>{val s=MemoryStorage();seed(s,expires=NOW-1000,gate=Long.MAX_VALUE);Fixture(s).use{f->
        f.token(tokens().apply{remove("id_token")});assertEquals("actual-access",f.manager.bearerToken());assertEquals(1,f.server.requestCount)
    }}
    @Test fun unknownRefreshFieldShapeCannotInvalidateHealthyToken()=runBlocking<Unit>{Fixture().use{f->
        f.token(tokens().put("earliest_refresh_at",JSONObject().put("future_policy","not-a-time")))
        val a=f.start();f.callback(a);f.settled();assertEquals("actual-access",f.manager.bearerToken());assertEquals(1,f.server.requestCount)
    }}
    @Test fun numericUndocumentedRefreshFieldDoesNotOverrideExpiresIn()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.token(tokens().apply{remove("id_token");put("earliest_refresh_at",Long.MAX_VALUE)})
        assertEquals("actual-access",f.manager.bearerToken());assertEquals("actual-access",f.manager.bearerToken());assertEquals(1,f.server.requestCount)
    }}
    @Test fun malformedRefreshEligibilityResponseFailsOnceWithoutDeletingSession()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"refresh_not_yet_eligible","earliest_refresh_at":{"unknown":true}}"""))
        failure{f.manager.bearerToken()};assertEquals(1,f.server.requestCount);assertTrue(s.read("brain_openai_accounts")!!.contains("old-refresh"))
    }}
    @Test fun malformedTokenLifetimeCannotCreateAnAuthorizedSession()=runBlocking<Unit>{Fixture().use{f->
        f.token(tokens().put("expires_in","3600"));val a=f.start();f.callback(a);f.settled()
        assertTrue(f.manager.state.value.accounts.isEmpty());assertNull(f.manager.credentialDescriptor())
    }}
    @Test fun fractionalTokenLifetimeCannotCreateAnAuthorizedSession()=runBlocking<Unit>{Fixture().use{f->
        f.token(tokens().put("expires_in",3599.5));val a=f.start();f.callback(a);f.settled()
        assertTrue(f.manager.state.value.accounts.isEmpty());assertNull(f.manager.credentialDescriptor())
    }}
    @Test fun disconnectedAccountCanBeReauthorizedWithItsOwnRegistration()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        f.server.enqueue(MockResponse().setResponseCode(200));f.manager.disconnect();val a=f.start("account-1")
        assertEquals(CLIENT,a.queryParameter("client_id"));assertNull(a.queryParameter("id_token_hint"));assertNull(f.manager.credentialDescriptor())
    }}
    @Test fun publicStateAndDescriptorNeverExposeBearerOrIDTokens()=runBlocking<Unit>{Fixture().use{f->
        f.signIn();val display=f.manager.state.value.toString()+f.manager.credentialDescriptor().toString()
        listOf("actual-access","refresh-secret","signed-id").forEach{assertFalse(display.contains(it))}
    }}
    @Test fun disconnectDuringRefreshCannotResurrectTheSession()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s).use{f->
        val started=CountDownLatch(1);val release=CountDownLatch(1)
        f.server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            if(request.path=="/revoke")return MockResponse().setResponseCode(200)
            started.countDown();release.await(3,TimeUnit.SECONDS)
            return MockResponse().setBody(tokens().apply{remove("id_token")}.toString())
        }}
        val pending=async(Dispatchers.IO){runCatching{f.manager.bearerToken()}}
        assertTrue(started.await(3,TimeUnit.SECONDS));assertTrue(f.manager.disconnect().remoteRevocationConfirmed)
        release.countDown();assertTrue(pending.await().isFailure);assertNull(f.manager.credentialDescriptor())
        assertFalse(s.read("brain_openai_accounts")!!.contains("actual-access"));assertFalse(f.manager.state.value.accounts.single().signedIn)
    }}

    @Test fun failedCodeExchangeRetainsIssuedRegistrationForFreshPKCERetry()=runBlocking<Unit>{Fixture().use{f->
        f.server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))
        val first=f.start();f.callback(first);f.settled()
        val registration=JSONObject(f.storage.read("brain_openai_pending_registration")!!)
        assertEquals(setOf("host_id","client_id"),registration.keys().asSequence().toSet())
        assertEquals(CLIENT,registration.getString("client_id"));assertTrue(f.manager.state.value.accounts.isEmpty())
        val retry=f.start();assertEquals(CLIENT,retry.queryParameter("client_id"));assertNull(retry.queryParameter("agent_name_hint"))
        assertNotEquals(first.queryParameter("state"),retry.queryParameter("state"));assertNotEquals(first.queryParameter("code_challenge"),retry.queryParameter("code_challenge"))
    }}
    @Test fun invalidStateCannotPersistAnIssuedRegistration(){Fixture().use{f->
        val a=f.start();f.callback(a,"state=attacker&code=untrusted&client_id=$CLIENT")
        assertNull(f.storage.read("brain_openai_pending_registration"));assertEquals(0,f.server.requestCount)
    }}
    @Test fun failedAdditionalAccountCannotReplaceTheActiveValidatedIdentity()=runBlocking<Unit>{val s=MemoryStorage();seed(s);Fixture(s,identity={_,_,_->throw OpenAiIdTokenValidationException()}).use{f->
        val previous=f.manager.credentialDescriptor();f.token();val a=f.start()
        f.callback(a,"state=${a.queryParameter("state")}&code=one-time-code&client_id=oaiapp_additional123");f.settled()
        assertEquals(previous,f.manager.credentialDescriptor());assertEquals(1,f.manager.state.value.accounts.size)
        assertTrue(s.read("brain_openai_accounts")!!.contains("old-access"))
        assertEquals("oaiapp_additional123",f.start().queryParameter("client_id"))
    }}
    @Test fun validatedIdentityRemovesPendingRegistration()=runBlocking<Unit>{Fixture().use{f->
        f.signIn();assertNull(f.storage.read("brain_openai_pending_registration"))
    }}
    @Test fun retainedRegistrationIsBoundToItsInstallationHost(){val s=MemoryStorage()
        s.write("brain_openai_host","urn:uuid:current-installation")
        s.write("brain_openai_pending_registration",JSONObject().put("host_id","urn:uuid:other-installation").put("client_id",CLIENT).toString())
        Fixture(s).use{f->assertEquals("dynamic_agent_client",f.start().queryParameter("client_id"))}
    }

    companion object {
        private const val NOW=1_800_000_000_000L
        private const val CLIENT="oaiapp_testclient123"
        private val PLAN=setOf("openid","profile","email","offline_access","resource.invoke",CHATGPT_PLAN_SCOPE)
        private fun tokens(scope:String=PLAN.joinToString(" "))=JSONObject().put("id_token","signed-id").put("access_token","actual-access")
            .put("refresh_token","refresh-secret").put("token_type","Bearer").put("expires_in",3600).put("scope",scope)
        private fun validQuery(auth:HttpUrl)="state=${auth.queryParameter("state")}&code=one-time-code&client_id=$CLIENT"
        private fun form(request:RecordedRequest)=("http://localhost/?"+request.body.readUtf8()).toHttpUrl()
    }
}
