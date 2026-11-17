# JARVIS official OpenAI SIWC contract research

Retrieved 2026-11-17 via available web research tool; local curl fails at proxy CONNECT 403. Evidence JSON files in this directory preserve exact source URLs, tool input, returned text, and crawl date. No provider login, account token, credential, or model inference was used. No repository source edits or builds were performed.

## Eligibility and required configuration

The repository has a GNU GPL v3 LICENSE. Official SIWC quickstart says ChatGPT plan usage is available to all open-source partners and selected private clients. The OSS overview covers open-source and locally hosted applications; paid or remotely hosted products should use the commercial interest form. The docs do not specifically mention Android or certify JARVIS. Therefore the project fits the documented OSS category, while actual user/workspace/region entitlement and Android browser behavior still require real end-to-end verification. Do not label this integration approved, connected, or live-tested before that proof.

The direct OSS registration flow does not require a pre-issued owner client ID, client secret, partner API key, backend, or Firebase. User must sign in to an eligible ChatGPT account, select its workspace, and consent. Quickstart names eligible Plus and Pro users. Any owner deployment outside the OSS/local scope needs separate eligibility review via https://openai.com/form/sign-in-with-chatgpt-interest/.

Sources:
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/request-client-id

## Registration/authorization contract

Before first sign-in, generate one opaque stable host identifier per Android installation/host and persist it. A once-generated UUID v4 with urn:uuid: prefix is documented; JWK thumbprint URI is preferred but optional. This host identifier is not a credential, user ID, or proof of possession.

Initial authorization is a system-browser GET to https://auth.openai.com/api/accounts/authorize with URL-encoded parameters:

- client_id=dynamic_agent_client
- agent_name_hint=J.A.R.V.I.S. (actual app name, consistent across installations; initial registration only)
- ext_agent_host_id=<persisted opaque host URI>
- response_type=code
- redirect_uri=http://127.0.0.1:<bound available port>/auth/callback
- scope=openid profile email offline_access resource.invoke chatgpt.tokens.use.direct
- resource=https://api.openai.com/v1
- state=<fresh cryptographically random per-attempt value>
- nonce=<fresh cryptographically random per-attempt value>
- code_challenge_method=S256
- code_challenge=<base64url(SHA-256(verifier)), no padding>

Choose and bind the loopback listener before opening the browser. Initial registration fixes scheme, host and path; later attempts may vary only the port. Within an attempt token exchange uses the identical redirect URI and port. No localhost, private scheme, App Link, device-code flow, private ChatGPT endpoint, or copied first-party client ID is documented for this OSS route.

Initial callback returns code, state, and an issued client_id. scope may be present but token response is authoritative. Validate pending state before handling any OAuth result. access_denied ends the attempt. Missing issued client ID means incomplete initial registration.

dynamic_agent_client is the official bootstrap entrypoint, not the issued client ID, and must never be used for code exchange or persisted as the registration. Retain the callback-issued ID with the pending registration; validate identity before replacing an active account. Later sign-ins use that saved issued ID and host ID, omit agent_name_hint, and use fresh state/nonce/verifier. If a returning callback contains a different client ID, reject it. An expired retained ID token can be sent as id_token_hint to select the same account; login_hint is optional and must match that registration.

POST form encoded to https://auth.openai.com/api/accounts/oauth/token:
grant_type=authorization_code
client_id=<callback-issued ID, or existing selected registration ID>
code=<validated callback code>
code_verifier=<original secret verifier>
redirect_uri=<identical pending URI>
resource=https://api.openai.com/v1

No client secret. Treat invalid_grant as unusable code; keep registration ID for a fresh attempt.

Source: https://developers.openai.com/siwc/token-sharing-open-source/sign-in

## OIDC identity validation

Actual public discovery document was retrieved:
https://auth.openai.com/.well-known/openid-configuration

Production values:
issuer=https://auth.openai.com
authorization_endpoint=https://auth.openai.com/api/accounts/authorize
token_endpoint=https://auth.openai.com/api/accounts/oauth/token
revocation_endpoint=https://auth.openai.com/api/accounts/oauth/revoke
jwks_uri=https://auth.openai.com/.well-known/jwks.json
userinfo_endpoint=https://auth.openai.com/api/accounts/oauth/userinfo
id_token_signing_alg_values_supported=[RS256]
public token/revocation authentication method=none
response mode=query; grants=authorization_code,refresh_token; PKCE=S256

Discovery lists standard identity/offline scopes only; the dedicated OSS guide additionally documents resource.invoke and chatgpt.tokens.use.direct. Do not remove the latter merely because discovery scopes_supported is narrower.

Verify ID JWT signature using published JWKS before trusting claims. Enforce exact issuer, aud containing the issued client ID, exp, original nonce, nonempty sub; official website example requires sub/exp/iat with 5-second skew. Validate optional sid shape when retained. Cache JWKS responsibly and refresh on unknown kid. Never accept alg=none or trust an arbitrary JWT-provided jku/x5u URL. Maintained JWT/OIDC library is recommended.

Account identity is validated issuer+issued client ID+sub. Email is display information, not the identity or workspace key; even equal email/sub can occur across registrations/workspaces. Retain registrations separately. No subscription tier can be inferred from an email or an opaque auth claim.

A valid ID token proves identity only. Require the token response's actual granted chatgpt.tokens.use.direct permission before attempting plan inference. Store the complete returned scope set. An ID-only result should be shown as signed in with AI usage disabled, with a deliberate option to authorize plan use or configure a distinct supported API billing route.

Sources:
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/website (signature-validation example explicitly referenced by OSS guide)
- https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery
- https://auth.openai.com/.well-known/openid-configuration

## Tokens, refresh, logout

Code exchange returns id_token/access_token/refresh_token/token_type/expires_in/scope/earliest_refresh_at. Access lifetime is one hour; refresh lifetime is 30 days and each rotation starts another 30-day period. The reference lists earliest_refresh_at but does not document its type/semantics; do not invent a required arithmetic interpretation.

Refresh near expiry with form POST to the same token endpoint:
grant_type=refresh_token
client_id=<issued ID associated with this token set>
refresh_token=<latest saved refresh token>
resource=https://api.openai.com/v1
Omit scope to retain grant. Serialize each account/session's refreshes and atomically persist replacement access token, scope, expiry, and rotating refresh token. Retained ID token supports future id_token_hint. Terminal invalid refresh codes require reauthorization; temporary infrastructure/network failure must not erase tokens.

Access token aud is https://api.openai.com/v1; ID token audience is issued client ID. Treat nested https://api.openai.com/auth metadata as opaque. Do not manufacture plan tier claims.

Logout stops requests, attempts form POST revocation using the refresh token, token_type_hint=refresh_token, and issued client_id, then clears local tokens. HTTP200/empty succeeds even for already-invalid token. If remote revocation cannot be confirmed, clear local secrets and truthfully report it; offer ChatGPT Settings disconnect. Retain host ID and account/client mapping for future same registration sign-in.

Android recommendation: Keystore-generated AES-GCM key protecting app-private atomic credential records, backups disabled, no URL/log/analytics leakage. No Firebase is necessary.

Sources:
- https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions
- https://developers.openai.com/siwc/token-sharing-open-source/token-reference

## Inference and real proof

Use OAuth access token as Authorization: Bearer. GET https://api.openai.com/v1/models returns a models array for this route (distinct from ordinary API-key catalog data shape). Preserve server order and include visibility=="list" entries; show display_name and send slug as model.

POST https://api.openai.com/v1/responses
Authorization: Bearer <selected account's OAuth access token>
Content-Type: application/json

Example minimal body:
{"model":"<account-visible slug>","instructions":"Be concise.","input":[{"role":"user","content":"Respond with the word READY."}],"store":false,"stream":true}

Use public Responses, not /chat/completions or ChatGPT backend-api. This contract does not require Codex account headers, OpenAI API project/organization headers, partner key, or Codex app-server. Consume SSE text deltas and terminal events. Treat only response.completed plus expected nonempty test content as successful AI verification. response.failed/response.incomplete/stream cutoff are failures, even after text deltas.

Required preview changes: store=false, stream=true, input array with supplied history. Use instructions/developer messages; explicit system-message items are rejected. Omit previous_response_id over HTTP. Unsupported fields include max_output_tokens, temperature, top_p, background, conversation, metadata and other fields enumerated in source. Audio/video inputs, file upload/transcription APIs and several hosted tools are not covered by this flow; existing Android STT/TTS must remain separate.

Sources:
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations

## Entitlement and truthful errors

Plan permission does not prove current usage availability. Handle subscription_sharing_user_not_eligible (403), subscription_sharing_usage_limit_exceeded (429), subscription_sharing_usage_unavailable (503), unsupported_capability (400), route_not_supported (403), invalid_user (401), authorization-context errors, plus admission failures whose JSON may contain detail instead of error. Failure can arrive after streaming begins. Preserve request ID/error code with sensitive data redacted. Never silently move a failed plan request to paid API billing.

Missing plan grant -> identity only. Valid grant but unavailable inference -> usage unavailable / authorization status with test failure. Mark AI Connected only following real completed inference. UI should show Using ChatGPT plan and link https://chatgpt.com/settings/usage. Plus five-hour usage is shared across apps; Pro is documented without that five-hour limit. Do not invent reset times or unlimited usage.

Sources:
- https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery
- https://developers.openai.com/siwc/ui-ux-guidelines
- https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions

## Android lifecycle/security implications (engineering recommendations, not an Android-specific OpenAI endorsement)

Current project minSdk29/targetSdk35, INTERNET permission and usesCleartextTraffic=false. SIWC requires an inbound loopback HTTP callback; keep all provider transport HTTPS and do not enable unrestricted cleartext. The callback lives on the same device as browser/app; using a companion machine's 127.0.0.1 would address the wrong host.

Prefer actual Custom Tabs with its browser binding/session support; fall back to the external default browser. Android docs describe Custom Tabs sharing browser state and maintaining launcher process importance. Lifecycle transitions still occur; ordinary external browser launch can stop the app Activity. Do not tie the pending sign-in socket to Compose disposal, onPause, onStop, or a short-lived activity coroutine. Keep a bounded application-owned attempt and observable result; rotations/UI recreation should not silently cancel it. Redmi/MIUI memory pressure and user cancellation must be tested on device.

Process death destroys a loopback listener; a saved ViewModel alone cannot preserve it. Fail safely on restart: discard any unrecoverable pending attempt, show interrupted sign-in, and require fresh state/nonce/PKCE rather than trusting a loose callback. Never persist credentials from a callback lacking its original pending transaction. Server can return a plain completion page telling user to return to app; do not rely on launching an Activity from a background socket thread, and never reflect code/tokens into HTML/links. App observes completed results when resumed. Browser return alone is not auth cancellation or success.

Bind only 127.0.0.1 on an available ephemeral port before opening authorization; exact /auth/callback, strict GET parsing, bounded request/line/query size, finite timeout, one pending attempt, reject ambiguous duplicate code/state/client_id parameters, no open redirect or general web server. Validate state before consuming valid callback; token exchange off main thread. Close listener on successful/denied verified callback, explicit cancellation, timeout, or fatal failure. Avoid arbitrary wrong-state network traffic terminating a legitimate attempt. Respond with cache-control:no-store and no-referrer and no scripts/resources exposing auth data.

RFC8252 supports native-app loopback redirects where platform allows them, notes local app interception risks, and requires PKCE; listen only loopback, only during the attempt. This does not override OpenAI's stricter IPv4/redirect contract. Browser and app should be in the same Android user/work profile; future cross-profile loopback restrictions must be surfaced as failure, not bypassed. No platform-specific Android SIWC allowlist/SDK or browser entitlement guarantee was found.

Official platform sources:
- https://developer.android.com/develop/ui/views/layout/webapps/overview-of-android-custom-tabs
- https://developer.android.com/guide/components/activities/activity-lifecycle
- https://developer.android.com/guide/components/activities/process-lifecycle
- https://datatracker.ietf.org/doc/html/rfc8252

## Claude

See anthropic-android-auth-findings-2026-11-17.md and matching official retrieval JSON. Current Anthropic policy explicitly prohibits third-party Claude.ai login or consumer subscription-token routing. Use Console API key or supported cloud provider, preferably owner-controlled authenticated HTTPS backend/companion; personal explicitly configured BYOK can use secure Android storage. Never bundle a shared key. Current documented recommended header is Authorization: Bearer <API_KEY>; legacy x-api-key remains supported. Explicit anthropic-version:2026-10-21 avoids a discrepancy between overview requirement and generated reference optional marking. Workspace header is needed for keys not scoped to a single workspace. WIF is organization/workload federation, not subscriber sign-in; App Attest is Apple only. Some current paid plans include claimable Console API credits, but that does not grant third-party Claude login permission.
