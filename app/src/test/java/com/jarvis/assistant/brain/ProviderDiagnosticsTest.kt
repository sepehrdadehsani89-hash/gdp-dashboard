package com.jarvis.assistant.brain

import org.junit.Assert.*
import org.junit.Test

class ProviderDiagnosticsTest {
    private val config=ProviderConfig("openai","gpt-4.1-mini")
    private fun row(status:ProviderConnectionStatus=ProviderConnectionStatus.FAILED,failure:ProviderFailureKind?=null)=ProviderConnection(config,status,failure=failure)
    private fun text(row:ProviderConnection?=null,saved:Boolean=true)=ProviderDiagnostics.describe(config,row,saved,"Unknown",1000)
    @Test fun savedCredentialAloneDoesNotMeanVerified(){assertTrue(text().contains("Connection: NOT_VERIFIED"))}
    @Test fun notConfiguredStateIsExplicit(){assertEquals(AiConnectionState.NOT_CONFIGURED,row(ProviderConnectionStatus.NOT_CONFIGURED).diagnosticState)}
    @Test fun activeTestIsValidating(){assertEquals(AiConnectionState.VALIDATING,row(ProviderConnectionStatus.VERIFYING).diagnosticState)}
    @Test fun completedInferenceIsConnectedVerified(){assertEquals(AiConnectionState.CONNECTED_VERIFIED,row(ProviderConnectionStatus.VERIFIED).diagnosticState)}
    @Test fun historicalProofIsClearlyHistorical(){assertEquals(AiConnectionState.PREVIOUSLY_VERIFIED,row(ProviderConnectionStatus.PREVIOUSLY_VERIFIED).diagnosticState)}
    @Test fun badKeyIsAuthenticationFailed(){assertEquals(AiConnectionState.AUTHENTICATION_FAILED,row(failure=ProviderFailureKind.AUTHENTICATION).diagnosticState)}
    @Test fun creditsFailureIsInsufficientQuota(){assertEquals(AiConnectionState.INSUFFICIENT_QUOTA,row(failure=ProviderFailureKind.CREDITS).diagnosticState)}
    @Test fun throttlingIsRateLimited(){assertEquals(AiConnectionState.RATE_LIMITED,row(failure=ProviderFailureKind.RATE_LIMIT).diagnosticState)}
    @Test fun dnsFailureIsNetworkUnavailable(){assertEquals(AiConnectionState.NETWORK_UNAVAILABLE,row(failure=ProviderFailureKind.NETWORK).diagnosticState)}
    @Test fun timeoutHasDistinctState(){assertEquals(AiConnectionState.TIMEOUT,row(failure=ProviderFailureKind.TIMEOUT).diagnosticState)}
    @Test fun inaccessibleModelHasDistinctState(){assertEquals(AiConnectionState.MODEL_UNAVAILABLE,row(failure=ProviderFailureKind.MODEL_UNAVAILABLE).diagnosticState)}
    @Test fun backendFailureCannotPretendProviderIsVerified(){assertEquals(AiConnectionState.BACKEND_UNAVAILABLE,row(failure=ProviderFailureKind.BACKEND_UNAVAILABLE).diagnosticState)}
    @Test fun malformedResponseIsProviderError(){assertEquals(AiConnectionState.PROVIDER_ERROR,row(failure=ProviderFailureKind.INCOMPLETE).diagnosticState)}
    @Test fun absentKeyIsNotConfigured(){assertTrue(text(saved=false).contains("Authentication: Not configured"))}
    @Test fun rejectedKeyIsShownAsRejected(){assertTrue(text(row(failure=ProviderFailureKind.AUTHENTICATION)).contains("Authentication: Rejected"))}
    @Test fun directApiDoesNotClaimBackendWasDeployed(){assertTrue(text().contains("Backend: Not required"));assertFalse(text().contains("Backend: Online"))}
    @Test fun diagnosticsDoNotReflectProviderMessagesOrConversation(){assertFalse(text(row().copy(message="PRIVATE_SECRET_REPLY")).contains("PRIVATE_SECRET_REPLY"))}
    @Test fun diagnosticsCannotBorrowDifferentModelProof(){assertTrue(text(row(ProviderConnectionStatus.VERIFIED).copy(config=config.copy(model="gpt-other"))).contains("Connection: NOT_VERIFIED"))}
    @Test fun actualRequestMetadataIsVisible(){val t=text(row().copy(httpStatus=429,elapsedMs=321,resolvedModel="gpt-4.1-mini-2025-04-14"));assertTrue(t.contains("HTTP status: 429"));assertTrue(t.contains("321 ms"));assertTrue(t.contains("Resolved model: gpt-4.1-mini-2025-04-14"))}
    @Test fun retryDelayIsActionable(){val t=text(row().copy(retryAt=2000));assertTrue(t.contains("Retry: Wait until"));assertTrue(text(row().copy(retryAt=1000)).contains("Retry: Available"))}
}
