package com.jarvis.assistant.pickup

import com.jarvis.assistant.core.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class PickupPolicyTest {
    private var at=0L
    private fun settle(p:PickupPolicy,eligible:Boolean=true,far:Boolean=true){repeat(12){at+=200;assertFalse(p.sample(0f,0f,9.81f,eligible,far))}}
    private fun lift(p:PickupPolicy,eligible:Boolean=true,far:Boolean=true):Boolean {
        at+=200;assertFalse(p.sample(0f,3f,9.3f,eligible,far))
        at+=200;assertFalse(p.sample(0f,6f,7.76f,eligible,far))
        at+=200;return p.sample(0f,6f,7.76f,eligible,far)
    }
    @Test fun anUncoveredSettledPhoneThenLiftAndTiltOffersExactlyOneGreeting(){val p=PickupPolicy{at};settle(p);assertTrue(lift(p));at+=200;assertFalse(p.sample(0f,6f,7.76f,true,true))}
    @Test fun aCoveredPocketCannotArmOrTriggerTheGesture(){val p=PickupPolicy{at};settle(p,far=false);assertFalse(lift(p,far=false));assertFalse(lift(p))}
    @Test fun aScreenOffOrBusyIntervalDisarmsAnEarlierCandidate(){val p=PickupPolicy{at};settle(p);at+=200;p.sample(0f,0f,9.81f,false,true);assertFalse(lift(p))}
    @Test fun proximityCoverDuringTheLiftCancelsIt(){val p=PickupPolicy{at};settle(p);assertFalse(lift(p,far=false));assertFalse(lift(p))}
    @Test fun continuousShakingDoesNotPretendThePhoneWasResting(){val p=PickupPolicy{at};repeat(50){at+=200;assertFalse(p.sample(if(it%2==0)4f else -4f,0f,9.81f,true,true))};assertFalse(lift(p))}
    @Test fun staleMovementDoesNotBecomeANewPickup(){val p=PickupPolicy{at};settle(p);at+=200;p.sample(0f,4f,9f,true,true);at+=3000;assertFalse(p.sample(0f,6f,7.76f,true,true));at+=200;assertFalse(p.sample(0f,6f,7.76f,true,true))}
    @Test fun invalidSensorsCannotTriggerAndAStationaryPhoneNeverGreets(){val p=PickupPolicy{at};settle(p);assertFalse(p.sample(Float.NaN,0f,9.81f,true,true));settle(p);repeat(100){at+=200;assertFalse(p.sample(0f,0f,9.81f,true,true))}}
    @Test fun greetingRotationHasSixDistinctPublicPhrasesForEachSupportedLanguage(){
        for(language in AppLanguage.supported){val phrases=(0..5).map{PickupGreetings.text(it,language)};assertEquals(6,phrases.toSet().size);assertEquals(phrases[0],PickupGreetings.text(6,language));assertTrue(phrases.all{it.isNotBlank() && !it.contains("see you",true)})}
    }
}
