package com.jarvis.assistant.google

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Clock
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class GoogleFileIdentityTest(private val product:GoogleProduct,private val scenario:String){
    @Test fun explicitFileConsentCannotVerifyAnEmptyOrDifferentFile()=runBlocking<Unit>{MockWebServer().use{server->
        val origin=server.url("/")
        val client=GoogleServicesClient(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(3,TimeUnit.SECONDS).build(),Clock.systemUTC(),origin)
        val field=when(product){GoogleProduct.DOCS->"documentId";GoogleProduct.SHEETS->"spreadsheetId";GoogleProduct.SLIDES->"presentationId";else->"formId"}
        val id=when(scenario){"non_string"->"12345";"invalid_input"->"../different?token=secret";else->"approved-id"}
        val root=JSONObject().put("title","Actual file").put("properties",JSONObject().put("title","Actual sheet")).put("info",JSONObject().put("title","Actual form"))
        when(scenario){"matching"->root.put(field,id);"different"->root.put(field,"another-file");"non_string"->root.put(field,12345)}
        if(scenario!="invalid_input"){server.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody(root.toString()));if(product==GoogleProduct.SHEETS && scenario=="matching")server.enqueue(MockResponse().setBody("{\"values\":[]}"))}
        val session=GoogleAccessSession("selected-account",product,"fixture-access-token",1)
        when(scenario){
            "matching"->{val result=client.read(session,GoogleQuery(product,GoogleQueryKind.DETAIL,id),"owner@example.com");assertEquals(id,result.items.single().id);assertEquals(if(product==GoogleProduct.SHEETS)2 else 1,server.requestCount)}
            "invalid_input"->{try{client.read(session,GoogleQuery(product,GoogleQueryKind.DETAIL,id),"owner@example.com");fail("Invalid resource accepted")}catch(_:IllegalArgumentException){};assertEquals(0,server.requestCount)}
            else->{val failure=try{client.read(session,GoogleQuery(product,GoogleQueryKind.DETAIL,id),"owner@example.com");error("Missing/mismatched file accepted")}catch(e:GoogleServiceFailure){e};assertEquals("INVALID_RESPONSE",failure.category);assertEquals(1,server.requestCount)}
        }
    }}
    companion object{@JvmStatic @Parameterized.Parameters(name="{0} {1}")fun cases()=listOf(GoogleProduct.DOCS,GoogleProduct.SHEETS,GoogleProduct.SLIDES,GoogleProduct.FORMS).flatMap{product->listOf("matching","missing","different","non_string","invalid_input").map{scenario->arrayOf(product,scenario)}}}
}
