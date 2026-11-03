package com.jarvis.assistant.omnisystem

import com.jarvis.assistant.data.BrainProtocol
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OmniDataPolicyTest {
    private fun vector(index:Int)=List(16){if(it==index)1f else 0f}
    private fun profile(id:String,index:Int,owner:Boolean=false)=SpeakerProfile(id,"Fixture $id",owner,vector(index),1,1000,SpeakerProfiles.SHA)
    @Test fun speakerSimilarityIsMeasuredAndAmbiguityDoesNotSelectAnIdentity(){
        val a=profile("a",0,true);val b=profile("b",1)
        assertEquals("a",SpeakerProfilePolicy.suggest(vector(0).toFloatArray(),listOf(a,b)).profile?.id)
        assertNull(SpeakerProfilePolicy.suggest(vector(2).toFloatArray(),listOf(a,b)).profile)
        val same=b.copy(vector=a.vector)
        val ambiguous=SpeakerProfilePolicy.suggest(vector(0).toFloatArray(),listOf(a,same))
        assertNull(ambiguous.profile);assertTrue(ambiguous.ambiguous)
    }
    @Test fun incompatibleModelTemplatesAreNotMatched(){
        assertNull(SpeakerProfilePolicy.suggest(vector(0).toFloatArray(),listOf(profile("old",0).copy(modelSha="0".repeat(64)))).profile)
    }
    @Test fun speakerStorageEnforcesSixProfilesOneOwnerFiveGuestsAndConsentMetadata(){
        val six=listOf(profile("owner",0,true))+(1..5).map{profile("guest$it",it)}
        assertEquals(six,SpeakerProfilePolicy.parse(SpeakerProfilePolicy.encode(six)))
        assertThrows(IllegalArgumentException::class.java){SpeakerProfilePolicy.parse(SpeakerProfilePolicy.encode(six+profile("extra",7)))}
        assertThrows(IllegalArgumentException::class.java){SpeakerProfilePolicy.parse(SpeakerProfilePolicy.encode(six.map{it.copy(owner=true)}))}
        assertThrows(IllegalArgumentException::class.java){SpeakerProfilePolicy.parse(SpeakerProfilePolicy.encode(listOf(profile("x",0).copy(consentAt=0))))}
    }
    @Test fun invalidOrZeroEmbeddingsCannotBecomeTemplates(){
        assertThrows(IllegalArgumentException::class.java){SpeakerProfilePolicy.normalized(FloatArray(16))}
        assertThrows(IllegalArgumentException::class.java){SpeakerProfilePolicy.normalized(FloatArray(16){Float.NaN})}
        assertThrows(IllegalArgumentException::class.java){SpeakerProfilePolicy.normalized(FloatArray(8){1f})}
    }
    @Test fun sampleBufferIsBoundedChronologicalAndDisabledBuffersAreDiscarded(){
        val first=ByteArray(32000){if(it%2==0)0 else 20}
        val last=ByteArray(32000){if(it%2==0)0 else 40}
        val buffer=SpeakerSampleBuffer(32000)
        buffer.feed(first,first.size,true);buffer.feed(last,last.size,true)
        assertArrayEquals(last,buffer.take()!!.pcm);assertNull(buffer.take())
        buffer.feed(first,first.size,true);buffer.feed(last,last.size,false);assertNull(buffer.take())
    }
    @Test fun silenceAndShortBuffersDoNotProduceProfileSamples(){
        val buffer=SpeakerSampleBuffer(64000)
        buffer.feed(ByteArray(16000),16000,true);assertNull(buffer.take())
        buffer.feed(ByteArray(64000),64000,true);assertNull(buffer.take())
    }
    @Test fun mapUsesActualCoordinatesAndRejectsUnsupportedOrUntrustedInputs(){
        val url=LocationMapPolicy.embed(-.18,-78.48,120f)
        assertTrue(url.startsWith("https://www.openstreetmap.org/export/embed.html?"));assertTrue(url.contains("marker=-0.180000,-78.480000"))
        assertTrue(LocationMapPolicy.permittedResource(url));assertTrue(LocationMapPolicy.permittedResource("https://tile.openstreetmap.org/1/0/0.png"))
        assertFalse(LocationMapPolicy.permittedResource("https://tile.openstreetmap.org.attacker.example/1/0/0.png"))
        assertFalse(LocationMapPolicy.permittedResource("https://user@www.openstreetmap.org/"))
        assertThrows(IllegalArgumentException::class.java){LocationMapPolicy.embed(Double.NaN,0.0)}
        assertThrows(IllegalArgumentException::class.java){LocationMapPolicy.embed(90.0,0.0)}
    }
    @Test fun imagePartsAreActuallySentToEachSupportedProviderWithoutAddedHistory(){
        val images=listOf(byteArrayOf(1,2,3),byteArrayOf(4,5,6))
        for(provider in listOf("gemini","openai","anthropic")){
            val body=JSONObject(BrainProtocol.requestBody(provider,"fixture-model","fixed system","compare","",emptyList(),pictures=images))
            val parts=if(provider=="gemini")body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts") else body.getJSONArray("messages").getJSONObject(if(provider=="openai")1 else 0).getJSONArray("content")
            assertEquals(3,parts.length())
            if(provider=="gemini")assertEquals("AQID",parts.getJSONObject(1).getJSONObject("inlineData").getString("data"))
            else if(provider=="openai")assertEquals("data:image/jpeg;base64,AQID",parts.getJSONObject(1).getJSONObject("image_url").getString("url"))
            else assertEquals("AQID",parts.getJSONObject(1).getJSONObject("source").getString("data"))
        }
    }
    @Test fun imageRequestsAreBoundedAndPlainTextProtocolStaysPlainText(){
        assertThrows(IllegalArgumentException::class.java){BrainProtocol.requestBody("gemini","fixture","system","text","",emptyList(),pictures=List(3){byteArrayOf(1)})}
        assertThrows(IllegalArgumentException::class.java){BrainProtocol.requestBody("openai","fixture","system","text","",emptyList(),pictures=listOf(ByteArray(1_500_001)))}
        val body=JSONObject(BrainProtocol.requestBody("openai","fixture","system","text","",emptyList()))
        assertEquals("text",body.getJSONArray("messages").getJSONObject(1).getString("content"))
    }
}
