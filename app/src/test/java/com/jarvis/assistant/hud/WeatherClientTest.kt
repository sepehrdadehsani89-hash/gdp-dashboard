package com.jarvis.assistant.hud

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class WeatherClientTest {
    private val forecast="""{"current":{"time":"2026-11-02T14:00","temperature_2m":20}}"""
    @Test fun cityAndForecastRequestsCarryOnlyNeededFields()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            server.enqueue(MockResponse().setBody("""{"results":[{"name":"Quito","country":"Ecuador","latitude":-0.22,"longitude":-78.51}]}"""));server.enqueue(MockResponse().setBody(forecast))
            val result=WeatherClient(server.url("/geo").toString(),server.url("/weather").toString()).fetch("Quito",null,null)
            assertEquals("Quito, Ecuador",result.place)
            assertEquals("Quito",server.takeRequest().requestUrl!!.queryParameter("name"))
            val request=server.takeRequest();assertEquals("-0.22",request.requestUrl!!.queryParameter("latitude"));assertTrue(request.requestUrl!!.queryParameter("current")!!.contains("relative_humidity_2m"))
            assertEquals(20.0,WeatherRepository.parse(result.json,result.place,100).temperatureC!!,.001)
        }finally{server.shutdown()}
    }
    @Test fun approximateChoiceSkipsGeocoding()=runBlocking {
        val server=MockWebServer();server.start()
        try {server.enqueue(MockResponse().setBody(forecast));val result=WeatherClient(server.url("/geo").toString(),server.url("/weather").toString()).fetch("",10.12,20.34);assertEquals("Approximate location",result.place);assertEquals("/weather",server.takeRequest().requestUrl!!.encodedPath);assertEquals(1,server.requestCount)}finally{server.shutdown()}
    }
    @Test fun httpErrorsUnknownCitiesAndOversizedResponsesAreExplicit()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            val client=WeatherClient(server.url("/geo").toString(),server.url("/weather").toString())
            server.enqueue(MockResponse().setResponseCode(429));assertTrue(runCatching{client.fetch("Test",null,null)}.exceptionOrNull()!!.message!!.contains("429"))
            server.enqueue(MockResponse().setBody("""{"results":[]}"""));assertTrue(runCatching{client.fetch("Unknown",null,null)}.exceptionOrNull()!!.message!!.contains("City not found"))
            server.enqueue(MockResponse().setBody(" ".repeat(262145)));assertTrue(runCatching{client.fetch("",1.0,2.0)}.exceptionOrNull()!!.message!!.contains("too large"))
        }finally{server.shutdown()}
    }
    @Test fun cancelledRequestsFinishWithoutPublishingWeather()=runBlocking {
        val server=MockWebServer();server.start()
        try{
            server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
            val job=launch{WeatherClient(server.url("/geo").toString(),server.url("/weather").toString()).fetch("Test",null,null);fail("Cancelled request must not deliver weather")}
            withContext(Dispatchers.IO){server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)}
            withTimeout(3000){job.cancelAndJoin()};assertTrue(job.isCancelled)
        }finally{server.shutdown()}
    }
}
