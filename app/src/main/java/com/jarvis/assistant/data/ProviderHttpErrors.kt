package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import org.json.JSONObject

/** Inspect documented codes, never reflect provider bodies or credentials into diagnostics. */
internal fun httpFailure(status:Int,provider:String,body:String,language:AppLanguage):ProviderException {
    val error=runCatching{JSONObject(body.take(32768)).optJSONObject("error")}.getOrNull()
    val code=error?.optString("code").orEmpty().ifBlank{error?.optString("type").orEmpty()}
    val kind=when {
        code in setOf("invalid_user","invalid_token","token_expired","invalid_api_key","authentication_error")->ProviderFailureKind.AUTHENTICATION
        code=="permission_error"->ProviderFailureKind.AUTHORIZATION
        code=="rate_limit_error"->ProviderFailureKind.RATE_LIMIT
        code in setOf("server_error","overloaded_error","api_error")->ProviderFailureKind.USAGE_UNAVAILABLE
        code in setOf("model_not_found","model_unavailable")->ProviderFailureKind.MODEL_UNAVAILABLE
        code in setOf("insufficient_quota","billing_hard_limit_reached","credit_balance_too_low")->ProviderFailureKind.CREDITS
        provider=="anthropic" && status==400 && error?.optString("message").orEmpty().contains("credit balance is too low",ignoreCase=true)->ProviderFailureKind.CREDITS
        code=="subscription_sharing_usage_unavailable"->ProviderFailureKind.USAGE_UNAVAILABLE
        code=="subscription_sharing_usage_limit_exceeded"->ProviderFailureKind.RATE_LIMIT
        code in setOf("subscription_sharing_user_not_eligible","route_not_supported","unsupported_capability")->ProviderFailureKind.AUTHORIZATION
        code in setOf("invalid_request_error","unsupported_parameter","unsupported_value","invalid_url") || status==400->ProviderFailureKind.BAD_REQUEST
        status==401->ProviderFailureKind.AUTHENTICATION
        status==403->ProviderFailureKind.AUTHORIZATION
        status==404->ProviderFailureKind.MODEL_UNAVAILABLE
        status==429->ProviderFailureKind.RATE_LIMIT
        status in setOf(408,504)->ProviderFailureKind.TIMEOUT
        status in 500..599->ProviderFailureKind.USAGE_UNAVAILABLE
        else->ProviderFailureKind.UNKNOWN
    }
    val message=when(kind){
        ProviderFailureKind.BAD_REQUEST->"The provider rejected the request parameters or endpoint. Check the selected model and API compatibility; no response was verified."
        ProviderFailureKind.CREDITS->"The provider API project has insufficient credits. Review its API billing; no alternate paid route was used."
        ProviderFailureKind.USAGE_UNAVAILABLE->"Provider usage is temporarily unavailable. Try again later; no alternate paid route was used."
        ProviderFailureKind.RATE_LIMIT->"The provider's rate or authorized usage limit was reached. Wait or review the account's usage."
        ProviderFailureKind.AUTHENTICATION->"The provider rejected this credential or session. Reconnect or update API access."
        ProviderFailureKind.AUTHORIZATION->"This account has not authorized the requested AI capability. Review permissions and the selected connection method."
        ProviderFailureKind.MODEL_UNAVAILABLE->"The selected AI model is unavailable to this account. Refresh the model list and test another model."
        ProviderFailureKind.TIMEOUT->"The provider request timed out. Please retry."
        else->BrainProtocol.providerHttpError(status,provider,body,language)
    }
    return ProviderException(if(provider=="gemini")BrainProtocol.providerHttpError(status,provider,body,language) else message,kind,status)
}
