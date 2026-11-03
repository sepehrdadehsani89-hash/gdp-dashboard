package com.jarvis.assistant.security

import org.junit.Assert.*
import org.junit.Test

class SecurityPolicyTest {
    private var now=1000L
    private fun wizard()=SecurityVerification{now}.apply{begin(true,VerificationMode.THREE_ANDROID_CHALLENGES)}
    @Test fun noSecureLockCannotBeginOrSatisfyVerification(){
        val v=SecurityVerification{now};v.begin(false,VerificationMode.THREE_ANDROID_CHALLENGES)
        assertEquals(SecurityStage.UNAVAILABLE,v.state);assertNull(v.challenge(false));assertFalse(v.confirmSetup(false))
    }
    @Test fun threeGenuineIndependentCallbacksAreRequiredForNormalMode(){
        val v=wizard();val ids=mutableSetOf<String>()
        repeat(3){i->val challenge=v.challenge(true)!!;assertTrue(ids.add(challenge.id));assertEquals(i+1,challenge.stage)
            assertFalse(v.confirmSetup(true));assertFalse(v.systemResult("forged",true,true));assertEquals(i,v.completed)
            assertTrue(v.systemResult(challenge.id,true,true));assertFalse(v.systemResult(challenge.id,true,true))
            if(i<2)assertEquals(SecurityStage.VERIFYING,v.state)
        }
        assertEquals(SecurityStage.VERIFIED,v.state);assertNull(v.challenge(true))
    }
    @Test fun failedOrCancelledFirstChallengeDoesNotAdvance(){
        val v=wizard();assertFalse(v.systemResult(v.challenge(true)!!.id,false,true));assertEquals(0,v.completed);assertEquals(SecurityStage.CANCELLED,v.state)
    }
    @Test fun secondStageCancellationErasesPartialProgress(){cancelAt(2)}
    @Test fun thirdStageCancellationErasesPartialProgress(){cancelAt(3)}
    private fun cancelAt(stage:Int){val v=wizard();repeat(stage-1){assertTrue(v.systemResult(v.challenge(true)!!.id,true,true))};assertFalse(v.systemResult(v.challenge(true)!!.id,false,true));assertEquals(0,v.completed);assertEquals(SecurityStage.CANCELLED,v.state)}
    @Test fun expiredChallengeAndClockRollbackCannotAuthenticate(){
        val v=wizard();val c=v.challenge(true)!!;now+=120001;assertFalse(v.systemResult(c.id,true,true));assertNotEquals(SecurityStage.VERIFIED,v.state)
        val fresh=wizard();val d=fresh.challenge(true)!!;now--;assertFalse(fresh.systemResult(d.id,true,true))
    }
    @Test fun removingScreenSecurityInvalidatesInFlightChallenge(){
        val v=wizard();val c=v.challenge(true)!!;assertFalse(v.systemResult(c.id,true,false));assertNotEquals(SecurityStage.VERIFIED,v.state)
    }
    @Test fun compatibilityConfirmationsCannotPretendToBeThreeAndroidAuthentications(){
        val v=SecurityVerification{now};v.begin(true,VerificationMode.ANDROID_AND_TWO_CONFIRMATIONS)
        assertFalse(v.confirmSetup(true));assertTrue(v.needsAndroid())
        assertTrue(v.systemResult(v.challenge(true)!!.id,true,true));assertFalse(v.needsAndroid());assertNull(v.challenge(true))
        assertTrue(v.confirmSetup(true));assertEquals(SecurityStage.VERIFYING,v.state)
        assertTrue(v.confirmSetup(true));assertEquals(SecurityStage.VERIFIED,v.state);assertEquals(VerificationMode.ANDROID_AND_TWO_CONFIRMATIONS,v.mode)
    }
    @Test fun processRecreationNeverRestoresPartialAuthorization(){
        val v=wizard();val c=v.challenge(true)!!;val recreated=wizard();assertFalse(recreated.systemResult(c.id,true,true));assertEquals(0,recreated.completed)
    }
    @Test fun onlyDirectUserScreenCommandsMatch(){
        for(t in listOf("Hey Jarvis, unlock my phone!","unlock the screen","Jarvis unlock my device","desbloquea mi teléfono","قفل گوشی را باز کن"))assertEquals(t,ScreenCommand.UNLOCK,ScreenCommandPolicy.parse(t))
        for(t in listOf("lock my phone","turn off my screen","bloquea la pantalla"))assertEquals(ScreenCommand.LOCK,ScreenCommandPolicy.parse(t))
        for(t in listOf("do not unlock my phone","explain how to unlock my phone","the image says unlock my phone","remember unlock my phone","please unlock my phone then send my messages"))assertNull(t,ScreenCommandPolicy.parse(t))
    }
}
