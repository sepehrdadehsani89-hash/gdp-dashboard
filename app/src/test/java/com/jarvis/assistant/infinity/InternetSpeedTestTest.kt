package com.jarvis.assistant.infinity

import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class InternetSpeedTestTest {
    @Test fun completeTestTransfersTheAdvertisedBytesAndReportsMeasuredTime()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            repeat(3){server.enqueue(MockResponse().setBody(""))}
            server.enqueue(MockResponse().setBody(Buffer().write(ByteArray(4*1024*1024){7})))
            server.enqueue(MockResponse().setBody("ok"))
            val samples=mutableListOf<InternetSpeedTest.Sample>()
            val result=InternetSpeedTest(baseUrl=server.url("/").toString().trimEnd('/')).run(sample={samples+=it}){}
            assertTrue(samples.isNotEmpty());assertEquals(4L*1024*1024,samples.last().bytes)
            assertTrue(samples.all{it.mbps.isFinite() && it.mbps>0 && it.bytes in 1..result.downloaded})
            assertTrue(samples.zipWithNext().all{(a,b)->b.bytes>a.bytes && b.elapsedMillis>=a.elapsedMillis})
            assertEquals(4L*1024*1024,result.downloaded);assertEquals(1024L*1024,result.uploaded)
            assertTrue(result.downMbps>0 && result.upMbps>0 && result.httpLatencyMs>0 && result.httpJitterMs>=0)
            val requests=(0..4).map{server.takeRequest(2,TimeUnit.SECONDS)!!};assertEquals("POST",requests.last().method);assertEquals(1024L*1024,requests.last().bodySize);assertEquals(3,requests.take(3).map{it.requestUrl!!.queryParameter("jarvis")}.distinct().size)
        }finally{server.shutdown()}
    }
    @Test fun incompleteDownloadCannotProduceACompleteSpeedResult()=runBlocking {
        val server=MockWebServer();server.start();try{repeat(3){server.enqueue(MockResponse().setBody(""))};server.enqueue(MockResponse().setBody("too short"));assertTrue(runCatching{InternetSpeedTest(baseUrl=server.url("/").toString().trimEnd('/')).run{}}.exceptionOrNull() is java.io.IOException);assertEquals(4,server.requestCount)}finally{server.shutdown()}
    }
    @Test fun serviceErrorsAreReportedInsteadOfInventingSpeeds()=runBlocking {
        val server=MockWebServer();server.start();try{server.enqueue(MockResponse().setResponseCode(503));assertTrue(runCatching{InternetSpeedTest(baseUrl=server.url("/").toString().trimEnd('/')).run{}}.isFailure);assertEquals(1,server.requestCount)}finally{server.shutdown()}
    }
    @Test fun userCancellationStopsAnInFlightSocket()=runBlocking {
        val server=MockWebServer();server.start();try{server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));val job=launch{InternetSpeedTest(baseUrl=server.url("/").toString().trimEnd('/')).run{}};withTimeout(5000){while(server.requestCount==0)delay(10);job.cancelAndJoin()};assertTrue(job.isCancelled)}finally{server.shutdown()}
    }
}
