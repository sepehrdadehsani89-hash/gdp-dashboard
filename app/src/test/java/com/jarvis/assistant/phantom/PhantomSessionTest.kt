package com.jarvis.assistant.phantom

import org.junit.Assert.*
import org.junit.Test

class PhantomSessionTest {
    @Test fun terminalStagesCannotLeaveProcessingForever(){
        var time=100L;val s=PhantomSession{time};s.submitted("private test",false)
        s.stage(ExecutionState.GENERATING_RESPONSE);time+=45_001;assertTrue(s.timedOut())
        s.finish(ExecutionState.FAILED);assertFalse(s.timedOut());assertEquals(ExecutionState.FAILED,s.state.value.execution)
        s.submitted("confirmation required",false);s.finish(ExecutionState.WAITING_USER)
        assertFalse(TraceEvent.COMPLETED in s.state.value.trace!!.events);assertTrue(TraceEvent.WAITING_USER in s.state.value.trace!!.events);assertFalse(s.timedOut())
    }
    @Test fun activeCaptureDoesNotInheritInferenceTimeLimit(){
        var time=0L;val s=PhantomSession{time};s.begin(true);s.event(TraceEvent.CAPTURE_STARTED)
        time=300_000;s.partial("still speaking");assertFalse(s.timedOut());assertEquals(ExecutionState.TRANSCRIBING,s.state.value.execution)
    }
    @Test fun realProgressExtendsInferenceButHasABoundedTotal(){
        var time=0L;val s=PhantomSession{time};s.submitted("question",false);s.stage(ExecutionState.GENERATING_RESPONSE)
        time=40_000;s.event(TraceEvent.FIRST_TOKEN);time=80_000;assertFalse(s.timedOut())
        time=241_000;s.event(TraceEvent.FIRST_TOKEN);assertTrue(s.timedOut())
    }
    @Test fun staleAudioCannotOverwriteTheNewRequest(){
        var time=1L;val s=PhantomSession{time};val first=s.submitted("old",false);s.finish(ExecutionState.CANCELLED)
        time=20;val second=s.submitted("new",false);assertNotEquals(first,second)
        s.event(TraceEvent.FIRST_AUDIO,first);s.stage(ExecutionState.SPEAKING,first)
        assertFalse(TraceEvent.FIRST_AUDIO in s.state.value.trace!!.events);assertEquals(ExecutionState.UNDERSTANDING,s.state.value.execution)
    }
    @Test fun finalTextReplacesPartialAndTraceHasNoTranscript(){
        var time=10L;val s=PhantomSession{time};s.begin(true);s.partial("open spot");time=50;s.submitted("open Spotify",true)
        assertEquals("open Spotify",s.state.value.caption.user)
        val trace=s.traces().single();assertEquals(40L,trace.events.getValue(TraceEvent.FINAL_TRANSCRIPT)-trace.started)
        assertFalse(trace.toString().contains("Spotify"));assertFalse(trace.toString().contains("open spot"))
    }
    @Test fun completedCaptionsExpireAndCancellationClearsPrivateText(){
        var time=0L;val s=PhantomSession{time};s.submitted("question",false);s.response("answer","answer");s.finish()
        time=7500;assertEquals(.5f,s.state.value.caption.alpha(time,false),.01f);assertEquals(1f,s.state.value.caption.alpha(time,true),0f)
        time=8000;assertFalse(s.state.value.caption.visible(time));s.finish(ExecutionState.CANCELLED);assertEquals(LiveCaption(),s.state.value.caption)
    }
    @Test fun tracesStayBoundedAndOnlyFirstMilestonesAreStored(){
        var time=1L;val s=PhantomSession{time}
        repeat(50){s.submitted("secret-$it",false);s.event(TraceEvent.FIRST_TOKEN);time++;s.event(TraceEvent.FIRST_TOKEN);s.finish()}
        assertEquals(24,s.traces().size);assertTrue(s.traces().all{it.events.size<=TraceEvent.entries.size})
        assertEquals(s.traces().last().started,s.traces().last().events.getValue(TraceEvent.FIRST_TOKEN))
    }
    @Test fun presentationIsHiddenForEveryLockCredentialAndStopCombination(){
        for(lock in listOf(false,true))for(credential in listOf(false,true))for(stop in listOf(false,true))for(app in listOf(false,true))for(wallpaper in listOf(false,true))for(enabled in listOf(false,true))for(permission in listOf(false,true)){
            val p=PhantomPolicy.presentation(lock,credential,stop,app,wallpaper,enabled,permission)
            when{lock || credential || stop->assertEquals(Presentation.HIDDEN,p);app->assertEquals(Presentation.APP,p);wallpaper->assertEquals(Presentation.WALLPAPER,p);enabled && permission->assertEquals(Presentation.OVERLAY,p);else->assertEquals(Presentation.HIDDEN,p)}
        }
    }
    @Test fun fastNativePathsDoNotHijackCompoundOrContextualRequests(){
        assertEquals("battery",PhantomPolicy.fastTool("What's my battery?"));assertEquals("what time is it",PhantomPolicy.fastTool("qué hora es"))
        listOf("what is its battery","battery then open maps","what time is it in Tokyo","remember my battery is bad","open battery settings","battery forecast tomorrow").forEach{assertNull(it,PhantomPolicy.fastTool(it))}
        assertTrue(PhantomPolicy.expand("show me that"));assertFalse(PhantomPolicy.expand("don't show me that"))
    }
    @Test fun persistedPositionsClampAfterRotationOrSmallerDisplay(){
        assertEquals(0,PhantomPolicy.clampPosition(-2f,400));assertEquals(400,PhantomPolicy.clampPosition(2f,400));assertEquals(0,PhantomPolicy.clampPosition(.7f,-40));assertEquals(100,PhantomPolicy.clampPosition(.25f,400))
    }
    @Test fun speakerAffinityRejectsCompetingSegmentsAndExpiresWithoutGuessingIdentity(){
        var time=0L;val focus=SpeakerAffinity{time}
        assertTrue(focus.accepts(null,false));assertFalse(focus.accepts(null,true))
        assertTrue(focus.accepts("owner",false));time=5000
        assertFalse(focus.accepts("guest",false));assertFalse(focus.accepts(null,false));assertFalse(focus.accepts("owner",true))
        assertTrue(focus.accepts("owner",false));time=34_999;assertFalse(focus.accepts("guest",false))
        time=35_000;assertTrue(focus.accepts("guest",false));focus.clear();assertTrue(focus.accepts(null,false))
    }
    @Test fun spokenRepairRequiresAnExplicitNewAppCommand(){
        val p=com.jarvis.assistant.voice.ConversationInterruptPolicy
        assertEquals("open spotify",p.replacement("Wait, open Spotify instead"))
        assertEquals("abre mapas",p.replacement("Espera, abre Mapas en su lugar"))
        assertNull(p.replacement("don't open Spotify"));assertNull(p.replacement("what does wait open Spotify mean"))
    }

}
