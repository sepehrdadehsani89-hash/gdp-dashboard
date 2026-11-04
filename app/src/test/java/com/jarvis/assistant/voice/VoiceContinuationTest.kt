package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceContinuationTest {
    @Test fun lockedWakeGreetingOpensACommandWindowAfterAudioInsteadOfRequiringAnotherWake(){
        var now=0L;val window=ConversationWindow{now};window.enable();assertTrue(window.wake());assertTrue(window.accept())
        // Provider delay, playback and echo cleanup do not consume the command timer.
        now=45_000
        window.resume(VoiceContinuationPolicy.followUp(true,locked=true,allowLockedChat=false,publicPrompt=true))
        assertEquals(ConversationWindow.Mode.WAITING_FOR_CAPTURE,window.snapshot().mode)
        assertFalse(window.acceptsCommand())
        now+=250;assertTrue(window.captureReady());assertEquals(now+12_000,window.snapshot().deadline)
        now+=1000;assertTrue(window.accept())
    }
    @Test fun allowedPublicLockedChatCanContinueAfterEachAnswer(){
        val window=ConversationWindow{1000L};window.enable()
        repeat(5){
            window.resume(VoiceContinuationPolicy.followUp(true,locked=true,allowLockedChat=true))
            assertTrue(window.captureReady());assertTrue(window.accept())
        }
    }
    @Test fun lockAndPrivacyNeverAuthorizePrivateRepliesOrAStoppedTurn(){
        assertFalse(VoiceContinuationPolicy.followUp(true,true,true,privateReply=true))
        assertFalse(VoiceContinuationPolicy.followUp(true,true,true,publicPrompt=true,privateReply=true))
        assertFalse(VoiceContinuationPolicy.followUp(true,false,true,systemPrivacy=true))
        assertFalse(VoiceContinuationPolicy.followUp(false,true,true,publicPrompt=true))
        assertFalse(VoiceContinuationPolicy.followUp(true,true,false))
    }
    @Test fun failedOrMutedGreetingUsesTheSameBoundedFollowUpThenReturnsToWakeOnly(){
        var now=0L;val window=ConversationWindow{now};window.enable();window.wake();window.accept()
        window.resume(VoiceContinuationPolicy.followUp(true,true,false,publicPrompt=true));now=30_000
        assertTrue(window.captureReady());now+=12_000;assertTrue(window.expire())
        assertEquals(ConversationWindow.Mode.WAKE_ONLY,window.snapshot().mode);assertFalse(window.accept())
    }
    @Test fun stopCannotBeUndoneByLateSpeechCompletion(){
        val window=ConversationWindow{0L};window.enable();window.wake();window.accept();window.disable()
        window.resume(VoiceContinuationPolicy.followUp(true,true,false,publicPrompt=true))
        assertFalse(window.captureReady());assertEquals(ConversationWindow.Mode.OFF,window.snapshot().mode)
    }
}
