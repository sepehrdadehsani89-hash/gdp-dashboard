package com.jarvis.assistant.brain

import org.junit.Assert.*
import org.junit.Test

class ProviderReconnectGateTest {
    private class F {var time=1000L;val gate=ProviderReconnectGate{time};fun fail(kind:ProviderFailureKind=ProviderFailureKind.NETWORK){gate.finish("a",kind)}}
    @Test fun firstForegroundRestorationIsAllowed(){assertTrue(F().gate.acquire("a"))}
    @Test fun concurrentNetworkAndForegroundEventsCoalesce(){val f=F();f.gate.acquire("a");assertFalse(f.gate.acquire("a"))}
    @Test fun firstFailureHasBackoff(){val f=F();f.gate.acquire("a");f.fail();f.time+=999;assertFalse(f.gate.acquire("a"))}
    @Test fun retryIsAllowedAtBackoffBoundary(){val f=F();f.gate.acquire("a");f.fail();f.time+=1000;assertTrue(f.gate.acquire("a"))}
    @Test fun secondFailureIncreasesDelay(){val f=F();f.gate.acquire("a");f.fail();f.time+=1000;f.gate.acquire("a");f.fail();f.time+=1000;assertFalse(f.gate.acquire("a"));f.time+=1000;assertTrue(f.gate.acquire("a"))}
    @Test fun threeFailedAttemptsEndThisWindow(){val f=F();repeat(3){assertTrue(f.gate.acquire("a"));f.fail();f.time+=10000};assertFalse(f.gate.acquire("a"))}
    @Test fun networkEventsCanRetryInANewFiniteWindow(){val f=F();repeat(3){f.gate.acquire("a");f.fail();f.time+=10000};f.time+=300000;assertTrue(f.gate.acquire("a"))}
    @Test fun authenticationFailureStopsAutomaticReconnect(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.AUTHENTICATION);f.time+=999999;assertFalse(f.gate.acquire("a"))}
    @Test fun authorizationFailureRequiresUserAction(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.AUTHORIZATION);assertFalse(f.gate.acquire("a"))}
    @Test fun creditsFailureNeverTriggersAutomaticInferenceRetry(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.CREDITS);f.time+=999999;assertFalse(f.gate.acquire("a"))}
    @Test fun unavailableModelNeedsExplicitChoice(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.MODEL_UNAVAILABLE);assertFalse(f.gate.acquire("a"))}
    @Test fun changedCredentialRevisionResetsOldFailure(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.AUTHENTICATION);assertTrue(f.gate.acquire("a:revision2"))}
    @Test fun changedProviderCanRestoreIndependently(){val f=F();f.gate.acquire("a");f.fail();assertTrue(f.gate.acquire("gemini:model"))}
    @Test fun healthyRestoreIsNotContinuouslyRepeated(){val f=F();f.gate.acquire("a");f.gate.finish("a",null);f.time+=59999;assertFalse(f.gate.acquire("a"))}
    @Test fun laterForegroundEventMayRestoreHealthyGrant(){val f=F();f.gate.acquire("a");f.gate.finish("a",null);f.time+=60000;assertTrue(f.gate.acquire("a"))}
    @Test fun staleCallbackCannotClearNewRestoration(){val f=F();f.gate.acquire("a");f.gate.acquire("b");f.gate.finish("a",null);assertFalse(f.gate.acquire("b"))}
    @Test fun staleFailureCannotInvalidateNewCredential(){val f=F();f.gate.acquire("a");f.gate.acquire("b");f.fail(ProviderFailureKind.AUTHENTICATION);f.gate.finish("b",null);f.time+=60000;assertTrue(f.gate.acquire("b"))}
    @Test fun explicitResetAllowsNewAttempt(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.AUTHENTICATION);f.gate.reset();assertTrue(f.gate.acquire("a"))}
    @Test fun cancellationDoesNotCreateTerminalAuthenticationFailure(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.CANCELLED);f.time+=1000;assertTrue(f.gate.acquire("a"))}
    @Test fun timeoutKeepsBoundedRetryAvailable(){val f=F();f.gate.acquire("a");f.fail(ProviderFailureKind.TIMEOUT);f.time+=1000;assertTrue(f.gate.acquire("a"))}
}
