package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Error fixtures never call a provider or count as live account verification. */
class ProviderHttpErrorsTest {
    private fun body(code:String)=JSONObject().put("error",JSONObject().put("code",code).put("message","SECRET-CREDENTIAL Bearer PRIVATE-SESSION")).toString()
    private fun error(status:Int=400,code:String="",provider:String="openai")=httpFailure(status,provider,body(code),AppLanguage.EN)
    @Test fun invalidSessionCodesRequireReconnect(){
        listOf("invalid_user","invalid_token","token_expired").forEach{assertEquals(ProviderFailureKind.AUTHENTICATION,error(code=it).kind)}
    }
    @Test fun modelAccessCodesDoNotMasqueradeAsIdentityFailure(){
        listOf("model_not_found","model_unavailable").forEach{assertEquals(ProviderFailureKind.MODEL_UNAVAILABLE,error(code=it).kind)}
    }
    @Test fun apiCreditCodesAreDistinctFromPlanQuota(){
        listOf("insufficient_quota","billing_hard_limit_reached","credit_balance_too_low").forEach{
            assertEquals(ProviderFailureKind.CREDITS,error(code=it).kind)
        }
    }
    @Test fun planUsageUnavailableDoesNotSuggestPaidFallback(){
        val failure=error(code="subscription_sharing_usage_unavailable")
        assertEquals(ProviderFailureKind.USAGE_UNAVAILABLE,failure.kind)
        assertTrue(failure.message.orEmpty().contains("no alternate paid route was used"))
    }
    @Test fun planUsageLimitMapsToRateLimit(){assertEquals(ProviderFailureKind.RATE_LIMIT,error(code="subscription_sharing_usage_limit_exceeded").kind)}
    @Test fun eligibilityAndUnsupportedRouteRequireAuthorization(){
        listOf("subscription_sharing_user_not_eligible","route_not_supported","unsupported_capability").forEach{
            assertEquals(ProviderFailureKind.AUTHORIZATION,error(code=it).kind)
        }
    }
    @Test fun missingStructuredCodeFallsBackToHttp401(){assertEquals(ProviderFailureKind.AUTHENTICATION,error(status=401).kind)}
    @Test fun forbiddenHttpStatusDoesNotClaimIdentitySignedOut(){assertEquals(ProviderFailureKind.AUTHORIZATION,error(status=403).kind)}
    @Test fun http404ReportsUnavailableModel(){assertEquals(ProviderFailureKind.MODEL_UNAVAILABLE,error(status=404).kind)}
    @Test fun http429ReportsUsageLimit(){assertEquals(ProviderFailureKind.RATE_LIMIT,error(status=429).kind)}
    @Test fun httpTimeoutStatusesAreTyped(){listOf(408,504).forEach{assertEquals(ProviderFailureKind.TIMEOUT,error(status=it).kind)}}
    @Test fun serverFailuresRemainTemporaryProviderUnavailability(){listOf(500,502,503,599).forEach{assertEquals(ProviderFailureKind.USAGE_UNAVAILABLE,error(status=it).kind)}}
    @Test fun documentedCodeTakesPrecedenceOverGenericStatus(){assertEquals(ProviderFailureKind.CREDITS,error(status=429,code="insufficient_quota").kind)}
    @Test fun anthropicErrorTypeIsAcceptedWhenCodeIsMissing(){
        val failure=httpFailure(401,"anthropic","""{"error":{"type":"invalid_token","message":"PRIVATE"}}""",AppLanguage.EN)
        assertEquals(ProviderFailureKind.AUTHENTICATION,failure.kind)
    }
    @Test fun diagnosticsNeverIncludeRawErrorMessagesOrCredentialStrings(){
        listOf("invalid_token","model_unavailable","insufficient_quota","subscription_sharing_usage_unavailable","unsupported_capability","unknown-secret-code").forEach{code->
            val message=error(code=code).message.orEmpty()
            assertFalse(message.contains("SECRET-CREDENTIAL"));assertFalse(message.contains("PRIVATE-SESSION"));assertFalse(message.contains("unknown-secret-code"))
        }
    }
    @Test fun malformedErrorBodyStillProducesSafeTypedHttpFailure(){
        val failure=httpFailure(401,"openai","<html>SECRET-CREDENTIAL</html>",AppLanguage.EN)
        assertEquals(ProviderFailureKind.AUTHENTICATION,failure.kind);assertFalse(failure.message.orEmpty().contains("SECRET-CREDENTIAL"))
    }
    @Test fun oversizedErrorBodyCannotReflectSensitiveTail(){
        val failure=httpFailure(503,"anthropic"," ".repeat(40000)+"PRIVATE-TAIL",AppLanguage.EN)
        assertEquals(ProviderFailureKind.USAGE_UNAVAILABLE,failure.kind);assertFalse(failure.message.orEmpty().contains("PRIVATE-TAIL"))
    }
    @Test fun exceptionsRetainHttpStatusForHonestDiagnostics(){assertEquals(429,error(status=429).httpStatus)}
    @Test fun geminiRestrictionGuidanceRemainsAvailableWithoutSecretReflection(){
        val failure=httpFailure(403,"gemini","""{"error":{"status":"PERMISSION_DENIED","message":"PRIVATE-KEY","details":[{"reason":"API_KEY_ANDROID_APP_BLOCKED"}]}}""",AppLanguage.EN)
        assertTrue(failure.message.orEmpty().contains("restrictions"));assertFalse(failure.message.orEmpty().contains("PRIVATE-KEY"))
    }
    @Test fun unknownHttpFailureDoesNotFabricateAuthorizationResult(){assertEquals(ProviderFailureKind.UNKNOWN,error(status=418).kind)}
    @Test fun anthropicLowCreditDiagnosticIsCanonicalAndRedacted(){
        val failure=httpFailure(400,"anthropic","""{"error":{"type":"invalid_request_error","message":"Your credit balance is too low to access the Anthropic API. PRIVATE-KEY"}}""",AppLanguage.EN)
        assertEquals(ProviderFailureKind.CREDITS,failure.kind)
        assertFalse(failure.message.orEmpty().contains("PRIVATE-KEY"))
    }

}
