package com.jarvis.assistant.voice

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class FirstSpeakerFocusTest {
    private fun voice(n:Int)=FloatArray(32).also{it[n]=1f}
    private fun pcm(n:Int)=ByteArray(32000).also{b->for(i in b.indices step 2){b[i]=n.toByte();b[i+1]=8}}
    private suspend fun next(g:SpeakerAudioFocus)=withTimeout(3000){var result:List<SpeakerAudioFocus.Packet>;do{delay(10);result=g.packets(ByteArray(0),0)}while(result.isEmpty());result.single()}
    @Test fun anyFirstSpeakerCanStartWithoutAnEnrolledIdentity(){val p=FirstSpeakerFocus{0};assertTrue(p.decide(voice(3),p.generation()));assertTrue(p.state.value.locked)}
    @Test fun anotherVoiceIsIgnoredWhileTheFirstContinues(){val p=FirstSpeakerFocus{0};val g=p.generation();assertTrue(p.decide(voice(0),g));assertFalse(p.decide(voice(1),g));assertTrue(p.decide(voice(0),g))}
    @Test fun referenceExpiresAndIsNotAPermanentOwnerRestriction(){var now=0L;val p=FirstSpeakerFocus{now};p.decide(voice(0),p.generation());now=45001;assertTrue(p.decide(voice(1),p.generation()))}
    @Test fun newWakeOrStopRevokesOldReferenceAndStaleInference(){val p=FirstSpeakerFocus{0};val old=p.generation();p.decide(voice(0),old);p.reset();assertFalse(p.decide(voice(0),old));assertFalse(p.state.value.locked);assertTrue(p.decide(voice(2),p.generation()))}
    @Test fun invalidEmbeddingsNeverBecomeSpeakerEvidence(){val p=FirstSpeakerFocus{0};assertThrows(IllegalArgumentException::class.java){p.decide(FloatArray(0),p.generation())};assertThrows(IllegalArgumentException::class.java){p.decide(FloatArray(32),p.generation())};assertThrows(IllegalArgumentException::class.java){p.decide(voice(0).also{it[2]=Float.NaN},p.generation())}}
    @Test fun pcmIsQuarantinedAndOtherSpeakerBecomesSilenceBeforeAsr()=runBlocking{
        val p=FirstSpeakerFocus{0};val allow=CompletableDeferred<Unit>();val g=SpeakerAudioFocus(true,p){audio->allow.await();if(audio.pcm[0]==1.toByte())voice(0)else voice(1)}
        try{val initial=g.packets(pcm(1),32000);assertTrue(initial.isEmpty());allow.complete(Unit);val a=next(g);assertTrue(a.pcm.any{it!=0.toByte()});a.pcm.fill(0);val pending=g.packets(pcm(2),32000);val b=pending.singleOrNull() ?: next(g);assertTrue(b.pcm.all{it==0.toByte()});b.pcm.fill(0)}finally{g.close()}
    }
    @Test fun silenceDoesNotLockOntoBackgroundOrInventRecognizedSpeech()=runBlocking{
        val p=FirstSpeakerFocus{0};var called=false;val g=SpeakerAudioFocus(true,p){called=true;voice(0)}
        try{val pending=g.packets(ByteArray(32000),32000);val frame=pending.singleOrNull() ?: next(g);assertFalse(called);assertFalse(p.state.value.locked);assertTrue(frame.pcm.all{it==0.toByte()})}finally{g.close()}
    }
    @Test fun filteredBackgroundDoesNotKeepAcceptedUtteranceListeningForever(){var now=0L;val turn=UtteranceAssembler({now});turn.segment("what time is it");now=1000;repeat(20){turn.energy(0f)};assertTrue(turn.ready());assertEquals("what time is it",turn.acceptedText())}
    @Test fun disabledOrWakeOnlyCaptureKeepsOriginalAsrAudioAndDoesNotClassify()=runBlocking{
        val p=FirstSpeakerFocus{0};var called=false;val g=SpeakerAudioFocus(true,p){called=true;voice(0)}
        try{val b=pcm(1);val f=g.packets(b,b.size,false).single();assertSame(b,f.pcm);assertFalse(f.owned);assertFalse(called)}finally{g.close()}
    }
    @Test fun obsoleteCaptureCannotLockTheNextConversation()=runBlocking{
        val p=FirstSpeakerFocus{0};val started=CompletableDeferred<Unit>();val resume=CompletableDeferred<Unit>();val g=SpeakerAudioFocus(true,p){started.complete(Unit);resume.await();voice(0)}
        try{g.packets(pcm(1),32000);withTimeout(3000){started.await()};g.resetCapture();p.reset();resume.complete(Unit);delay(80);assertTrue(g.packets(ByteArray(0),0).isEmpty());assertFalse(p.state.value.locked)}finally{g.close()}
    }
}
