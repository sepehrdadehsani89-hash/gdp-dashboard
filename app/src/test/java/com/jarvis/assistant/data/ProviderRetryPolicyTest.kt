package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import org.junit.Assert.*
import org.junit.Test

class ProviderRetryPolicyTest {
    private fun error(status:Int=429,kind:ProviderFailureKind=ProviderFailureKind.RATE_LIMIT,wait:Long?=null)=ProviderException("safe error",kind,status,wait)
    @Test fun missingRetryAfterIsUnknown(){assertNull(ProviderRetryPolicy.retryAfter(null))}
    @Test fun secondsAreConvertedToMilliseconds(){assertEquals(2000L,ProviderRetryPolicy.retryAfter("2"))}
    @Test fun zeroSecondsAllowsImmediateRetry(){assertEquals(0L,ProviderRetryPolicy.retryAfter("0"))}
    @Test fun negativeDelayIsRejected(){assertNull(ProviderRetryPolicy.retryAfter("-1"))}
    @Test fun malformedDelayIsIgnored(){assertNull(ProviderRetryPolicy.retryAfter("private error text"))}
    @Test fun excessiveDelayIsBounded(){assertEquals(86400000L,ProviderRetryPolicy.retryAfter("999999999"))}
    @Test fun rfcDateIsRelativeToActualClock(){assertEquals(2000L,ProviderRetryPolicy.retryAfter("Thu, 01 Jan 1970 00:00:02 GMT",0))}
    @Test fun pastRfcDateCannotProduceNegativeWait(){assertEquals(0L,ProviderRetryPolicy.retryAfter("Thu, 01 Jan 1970 00:00:00 GMT",2000))}
    @Test fun firstSafeReadRetryHasBackoff(){assertEquals(300L,ProviderRetryPolicy.delayForSafeRead(0,error()))}
    @Test fun secondSafeReadRetryIncreasesBackoff(){assertEquals(600L,ProviderRetryPolicy.delayForSafeRead(1,error()))}
    @Test fun readRetriesAreFinite(){assertNull(ProviderRetryPolicy.delayForSafeRead(2,error()))}
    @Test fun invalidAttemptCannotRetry(){assertNull(ProviderRetryPolicy.delayForSafeRead(-1,error()))}
    @Test fun jitterIsBounded(){assertEquals(500L,ProviderRetryPolicy.delayForSafeRead(0,error(),99999))}
    @Test fun serverWaitIsRespected(){assertEquals(1500L,ProviderRetryPolicy.delayForSafeRead(0,error(wait=1500)))}
    @Test fun longServerWaitIsLeftForExplicitUserRetry(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(wait=10000)))}
    @Test fun insufficientCreditsNeverRetry(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(kind=ProviderFailureKind.CREDITS)))}
    @Test fun rejectedKeyNeverRetries(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(401,ProviderFailureKind.AUTHENTICATION)))}
    @Test fun unauthorizedCapabilityNeverRetries(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(403,ProviderFailureKind.AUTHORIZATION)))}
    @Test fun unavailableModelNeverRetries(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(404,ProviderFailureKind.MODEL_UNAVAILABLE)))}
    @Test fun invalidRequestNeverRetries(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(400,ProviderFailureKind.BAD_REQUEST)))}
    @Test fun transientServiceFailureMayRetrySafeRead(){assertEquals(300L,ProviderRetryPolicy.delayForSafeRead(0,error(503,ProviderFailureKind.USAGE_UNAVAILABLE)))}
    @Test fun unknownStatusNeverRetries(){assertNull(ProviderRetryPolicy.delayForSafeRead(0,error(418,ProviderFailureKind.UNKNOWN)))}
}
