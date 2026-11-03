package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class CustomVoicePolicyTest {
    @Test fun privateOrDisallowedRepliesNeverUseCustomCloudVoice(){
        for(enabled in listOf(false,true))for(allowed in listOf(false,true))for(privateTurn in listOf(false,true))
            assertEquals(enabled && allowed && !privateTurn,CustomVoicePolicy.eligible(enabled,allowed,privateTurn))
    }
    @Test fun messagePackUsesActualReferenceIdAndUtf8Lengths(){
        val bytes=CustomVoicePolicy.payload("سلام",CustomVoicePolicy.REQUESTED_ID)
        assertEquals(0x83,bytes[0].toInt() and 255)
        val text=bytes.toString(Charsets.UTF_8)
        assertTrue(text.contains("reference_id"));assertTrue(text.contains(CustomVoicePolicy.REQUESTED_ID));assertTrue(text.contains("سلام"));assertTrue(text.endsWith("mp3"))
        assertEquals(0xa8,bytes[6].toInt() and 255) // eight UTF-8 bytes, not four characters
        assertTrue(CustomVoicePolicy.payload("x".repeat(300),"new_id").any{(it.toInt() and 255)==0xda})
    }
    @Test fun rejectsPathInjectionAndUnboundedText(){
        for(id in listOf("../secret","id?key=secret","x\nHeader",""))assertFalse(CustomVoicePolicy.validId(id))
        assertTrue(CustomVoicePolicy.validId("editable_voice-2"))
        assertThrows(IllegalArgumentException::class.java){CustomVoicePolicy.payload("x".repeat(3501),"valid")}
    }
}
