package com.jarvis.assistant.omnisystem

import kotlinx.coroutines.*
import okhttp3.Request
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ModuleHttpTest {
    @Test fun boundedResponsesRejectDeclaredAndChunkedOversizeWithoutEchoingErrors()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            val client=ModuleHttp();val request=Request.Builder().url(server.url("/private")).build()
            server.enqueue(MockResponse().setBody("secret provider diagnostic").setResponseCode(401))
            val error=runCatching{client.bytes(request,10)}.exceptionOrNull()!!
            assertTrue(error is ModuleServiceFailure);assertFalse(error.message!!.contains("secret"))
            server.enqueue(MockResponse().setBody("x".repeat(11)))
            assertTrue(runCatching{client.bytes(request,10)}.isFailure)
            server.enqueue(MockResponse().setChunkedBody("x".repeat(11),3))
            assertTrue(runCatching{client.bytes(request,10)}.isFailure)
            server.enqueue(MockResponse().setBody("actual"));assertEquals("actual",client.bytes(request,10).toString(Charsets.UTF_8))
        }finally{server.shutdown()}
    }
    @Test fun cancellationDisposesCallWithoutDeliveringPrivateResponse()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val job=launch{ModuleHttp().bytes(Request.Builder().url(server.url("/cancel")).build(),100);fail("Cancelled response delivered")}
            withContext(Dispatchers.IO){assertNotNull(server.takeRequest(5,TimeUnit.SECONDS))}
            withTimeout(3000){job.cancelAndJoin()};assertTrue(job.isCancelled)
        }finally{server.shutdown()}
    }
    @Test fun redirectsNeverForwardAuthorizationToAnotherHost()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location","https://untrusted.invalid/"))
            val error=runCatching{ModuleHttp().bytes(Request.Builder().url(server.url("/token")).header("Authorization","fixture-not-secret").build(),100)}.exceptionOrNull()
            assertTrue(error is ModuleServiceFailure);assertEquals(1,server.requestCount)
        }finally{server.shutdown()}
    }
}
