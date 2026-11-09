package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class PlaybackInterruptPolicyTest {
    @Test fun narrowEnglishAndSpanishControlsStopOnlySpeech(){
        assertEquals("wait wait",PlaybackInterruptPolicy.command("wait, wait","The capital is Quito."))
        assertEquals("stop talking",PlaybackInterruptPolicy.command("Jarvis, stop speaking","Here is the answer."))
        assertEquals("wait wait",PlaybackInterruptPolicy.command("espera espera","La capital es Quito."))
        assertEquals("stop talking",PlaybackInterruptPolicy.command("deja de hablar","Aquí está la respuesta."))
    }
    @Test fun ordinaryCommandsAndBackgroundSentencesCannotRunDuringReplyPlayback(){
        listOf("unlock my phone","lock my phone","call Alex","open Spotify","wait wait and unlock my phone","I said stop talking yesterday","stop").forEach{
            assertNull(it,PlaybackInterruptPolicy.command(it,"Here is an explanation."))
        }
    }
    @Test fun aReplyQuotingItsOwnControlPhraseCannotCancelItself(){
        assertNull(PlaybackInterruptPolicy.command("stop talking","You can say stop talking to interrupt."))
        assertNull(PlaybackInterruptPolicy.command("wait wait","The phrase is: wait, wait."))
        assertNull(PlaybackInterruptPolicy.command("espera espera","Puedes decir espera, espera."))
    }
    @Test fun playbackControlsRequireAnAlreadyEnabledWakeServiceAndVoiceSession(){
        assertTrue(PlaybackInterruptPolicy.mayArm(true,true,true,false))
        assertFalse(PlaybackInterruptPolicy.mayArm(false,true,true,false))
        assertFalse(PlaybackInterruptPolicy.mayArm(true,false,true,false))
        assertFalse(PlaybackInterruptPolicy.mayArm(true,true,false,false))
        assertFalse(PlaybackInterruptPolicy.mayArm(true,true,true,true))
    }
    @Test fun obsoleteOrCompletedPlaybackCannotInterruptANewRequest(){
        assertTrue(PlaybackInterruptPolicy.current(5,5,true))
        assertFalse(PlaybackInterruptPolicy.current(5,6,true))
        assertFalse(PlaybackInterruptPolicy.current(5,5,false))
    }
}
