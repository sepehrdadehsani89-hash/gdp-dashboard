# JARVIS — MASTER AI CONNECTIVITY REPAIR PROMPT
## Production-Grade OpenAI Integration, Real Model Discovery, Verified Authentication, Secure Backend, Voice AI, and End-to-End Reliability

### YOUR ROLE

You are a principal Android engineer, AI infrastructure architect, Kotlin specialist, backend engineer, security engineer, and senior quality assurance engineer.

You have full access to my existing Jarvis Android project.

Your mission is to **repair the actual AI functionality of Jarvis**, not simply improve its interface.

The newest APK installs successfully on my physical Xiaomi Redmi 9. The holographic core animation is now working correctly. However, the AI provider integration is broken.

**CURRENT CRITICAL PROBLEMS:**

1. OpenAI does not connect successfully.
2. Jarvis has no usable AI model.
3. The assistant cannot reliably answer normal questions.
4. Provider connection indicators may report success without proving that the provider works.
5. Model discovery, authentication, or backend communication may be broken.
6. The interface appears operational, but the intelligence behind it is not.

The objective is to make Jarvis a **real, reliable, voice-first AI assistant powered by working AI models**.

Do not replace the real AI engine with mock data, hardcoded responses, simulated network requests, or fake provider connections.

**A green Connected badge must mean a real authenticated API request succeeded.**

---

# PHASE 1 — COMPLETE EXISTING PROJECT AUDIT

Before modifying any code, inspect the entire repository.

Identify:

- Android application module and package name.
- Current Kotlin and Java source files.
- Jetpack Compose or XML UI architecture.
- AI provider interfaces and implementations.
- OpenAI integration.
- HTTP clients, Retrofit, OkHttp, and interceptors.
- Authentication and API key management.
- Existing backend configuration.
- Model discovery logic.
- Chat request pipeline.
- Streaming response pipeline.
- Voice recognition and text-to-speech integration.
- Conversation state and persistence.
- Provider settings screens.
- Connection test functionality.
- Error handling.
- Network permissions and Android security configuration.
- Existing tests and build configuration.

Trace the complete request lifecycle:

User speaks or types → Jarvis receives input → provider selection → credential loading → network request → provider authentication → model execution → response parsing → conversation update → voice output.

Find the exact point where the pipeline fails.

Inspect actual code and runtime logs. Do not guess.

Produce a short root-cause report identifying the failing classes, functions, and relevant code paths.

**Preserve all currently working features**, especially the repaired holographic core animation.

---

# PHASE 2 — REBUILD OPENAI CONNECTIVITY CORRECTLY

Implement a real OpenAI connection using the official, currently supported API contract.

Use the appropriate endpoint for the selected API architecture, such as the OpenAI Responses API, and verify the current request and response formats against official documentation.

Do not rely on outdated endpoint assumptions.

The implementation must correctly support:

- HTTPS communication.
- Authentication.
- Model selection.
- User messages.
- System/developer instructions where supported.
- Conversation context.
- Structured provider errors.
- Request timeouts.
- Network cancellation.
- Streaming when supported.
- Response extraction.
- Retry policies.
- Rate-limit handling.
- Credential validation.

Do not invent API model identifiers.

Do not assume that a model is accessible merely because its name appears in a dropdown.

Use actual provider capabilities and permissions to determine which models are available.

## Authentication

Support the officially authorized authentication method for each integration.

For OpenAI API access, use a valid OpenAI API key.

Never confuse:

- A ChatGPT subscription.
- An OpenAI API key.
- An OAuth session.
- A third-party gateway credential.
- A local application token.

These are not interchangeable.

If the account lacks API credits or access, show a clear explanation.

Do not attempt to bypass provider billing, rate limits, account restrictions, or authentication requirements.

## API key security

Never hardcode API keys into the Android source code.

Never commit credentials into GitHub.

Never include secrets in APK resources, build logs, analytics, or crash reports.

Preferred architecture:

Android Jarvis application → authenticated secure backend → OpenAI API.

The backend must securely store provider credentials in server-side environment variables.

If the project already has a backend, repair and reuse it where practical.

If the application supports direct user-supplied API keys, document the security tradeoffs and use Android Keystore-backed protection where appropriate. Do not treat client-side encryption as equivalent to keeping a key on a server.

---

# PHASE 3 — IMPLEMENT REAL CONNECTION VERIFICATION

This is the most important requirement.

**Never display Connected unless a real provider operation succeeds.**

Create an explicit provider connection state machine:

1. NOT_CONFIGURED
2. VALIDATING
3. CONNECTED_VERIFIED
4. AUTHENTICATION_FAILED
5. INSUFFICIENT_QUOTA
6. RATE_LIMITED
7. NETWORK_UNAVAILABLE
8. TIMEOUT
9. MODEL_UNAVAILABLE
10. BACKEND_UNAVAILABLE
11. PROVIDER_ERROR

The state must reflect actual evidence.

A successful TCP connection does not mean OpenAI authentication succeeded.

A successful backend health check does not mean OpenAI works.

A valid-looking API key does not mean the account has model access.

A successful model-list request does not guarantee inference works.

## Required connection test

When the user taps **Test Connection**:

1. Validate local configuration.
2. Verify that the backend is reachable if one is required.
3. Authenticate against the real provider.
4. Verify that the selected model is available to the credential.
5. Send a minimal real inference request to that model.
6. Parse the provider's actual response.
7. Mark the connection verified only after successful inference.
8. Display the verified model identifier and verification time.

Use a minimal request such as:

"Reply with the word READY."

Do not assume the response will match perfectly; validate that the provider returned a successful, nonempty, legitimate model output.

Clearly inform the user that a real connection test may consume a small amount of API usage.

## Absolutely prohibited

- Fake HTTP 200 responses.
- Mocked production provider responses.
- Hardcoded "Connection successful" messages.
- Green status indicators based solely on saved credentials.
- Simulated AI replies when a real request fails.
- Silently falling back to another provider.
- Reporting model availability without verification.
- Claiming the connection was tested when it was not.

Mocking is allowed only in clearly isolated automated tests, never as proof of live connectivity.

---

# PHASE 4 — FIX MODEL DISCOVERY AND SELECTION

Jarvis currently has no usable AI model.

Investigate why.

Potential causes to investigate include:

- Incorrect API endpoint.
- Invalid credentials.
- Unsupported authentication.
- Broken JSON parsing.
- Incorrect model identifiers.
- Missing backend configuration.
- Account permissions.
- Rate limits.
- Network failures.
- Incorrect filtering of available models.
- Incorrect persistence of the selected model.
- UI state synchronization bugs.

Do not assume any one cause until evidence confirms it.

## Model selection interface

Build a reliable model selector displaying:

- Provider.
- Exact model identifier.
- Availability status.
- Verification status.
- Relevant capabilities.
- Whether the model supports the intended request type.

Models must be obtained from a legitimate provider capability source or a maintained, documented compatibility registry.

Do not fabricate a model list.

If discovery fails, show:

"Unable to retrieve available models."

Include the actual error category and a Retry button.

If a provider returns models but none is usable for Jarvis chat, explain that explicitly.

## Default model behavior

If no model is selected:

- Attempt to identify an accessible compatible model.
- Verify it before activating it.
- Save the verified selection.
- Inform the user which model was chosen.

If no compatible model is available, do not invent one.

Show an actionable setup message.

Persist model selection across app restarts.

Handle revoked or discontinued models gracefully.

---

# PHASE 5 — IMPLEMENT A SECURE, WORKING BACKEND

Inspect whether Jarvis currently requires a backend URL.

If a backend is needed, implement or repair the actual backend.

Use the existing infrastructure where possible.

If appropriate, support a secure deployment architecture compatible with Vercel.

Required backend responsibilities:

- Receive authenticated requests from Jarvis.
- Validate input.
- Authenticate the app user appropriately.
- Retrieve server-side provider credentials.
- Call the real OpenAI API.
- Stream or return the actual response.
- Normalize provider errors without hiding important details.
- Enforce sensible usage limits.
- Protect secrets.
- Provide meaningful health diagnostics.

Implement a genuine backend health endpoint.

However, distinguish clearly between:

**Backend online** and **AI provider verified**.

These must be separate indicators.

A backend may be operational even when OpenAI is unavailable.

## Backend URL configuration

Validate:

- HTTPS URL format.
- Correct host.
- Correct API path.
- Reachability.
- Authentication requirements.
- TLS certificate validity.
- Response content type.
- Server-side error handling.

Avoid exposing backend secrets in Android logs.

Do not disable TLS verification or accept arbitrary certificates to make connectivity appear to work.

If the backend URL is missing, show a clear configuration error rather than silently pretending AI is available.

---

# PHASE 6 — BUILD A COMPLETE ERROR DIAGNOSTIC SYSTEM

Handle provider errors accurately.

### HTTP 400 — Bad Request

Investigate invalid request bodies, model identifiers, unsupported parameters, and endpoint mismatches.

Display a meaningful explanation.

### HTTP 401 — Unauthorized

Identify missing, invalid, expired, or revoked credentials.

Never report Connected.

### HTTP 403 — Forbidden

Identify account permissions or access restrictions.

Do not bypass them.

### HTTP 404 — Not Found

Check endpoint paths, model identifiers, and deployment configuration.

### HTTP 429 — Rate Limited or Quota Exhausted

Distinguish between:

- Temporary request-rate limits.
- Usage limits.
- Insufficient account quota.

Respect provider retry guidance.

Do not repeatedly hammer the API.

### HTTP 500/502/503/504

Handle temporary provider or backend failures.

Use bounded retries with exponential backoff and jitter for retryable requests.

Avoid automatically retrying operations that could duplicate side effects.

### Network failures

Handle:

- No internet connection.
- DNS errors.
- TLS errors.
- Connection timeouts.
- Read timeouts.
- Interrupted streaming.
- Backend unreachable.
- Android background network restrictions.

Never collapse every failure into the generic message "Something went wrong."

## Diagnostic screen

Create an AI Diagnostics section showing:

- Internet connectivity.
- Backend connectivity.
- Provider authentication status.
- Model availability.
- Last successful inference.
- Last error category.
- Selected provider.
- Selected model.
- Request duration.
- Retry availability.

Show safe, redacted diagnostic details.

Never display complete API keys, authorization headers, or private user conversations in exported logs.

---

# PHASE 7 — RESTORE REAL JARVIS CONVERSATION

After connectivity is repaired, ensure the assistant can actually answer questions.

Required flow:

1. User submits a message.
2. Jarvis displays the user's message.
3. The app sends the request to the verified model.
4. The model generates a real response.
5. Jarvis displays the returned text.
6. Jarvis speaks the response if voice output is enabled.
7. The conversation is saved appropriately.

The assistant must answer ordinary questions using real model inference.

Examples:

"What is artificial intelligence?"

"Explain Python programming."

"Help me plan my day."

"Tell me about the solar system."

The app must not respond with a canned answer pretending to come from OpenAI.

## Conversation memory

Preserve conversation history across navigation and app restarts according to the existing privacy settings.

Ensure conversation context is correctly passed to the model.

Avoid uncontrolled growth of request context.

Use sensible truncation or summarization strategies when necessary.

Never claim to remember information that was not stored.

---

# PHASE 8 — RESTORE VOICE-FIRST AI FUNCTIONALITY

Jarvis is intended to be a voice-first assistant, not merely a chatbot.

Repair the voice-to-AI pipeline:

Speech recognition → recognized text → real model request → model response → text-to-speech.

Requirements:

- Reliable microphone permission handling.
- Accurate speech recognition state.
- Clear listening indicators.
- Proper request cancellation.
- Text-to-speech initialization.
- Voice response playback.
- No duplicate AI requests.
- No endless listening loops.
- No microphone feedback caused by Jarvis hearing its own speech.

Preserve the previously repaired holographic core animation.

Synchronize the core with actual states:

- Idle.
- Listening.
- Processing.
- Speaking.
- Error.

Do not animate Processing indefinitely after a request fails.

If OpenAI is unavailable, Jarvis should say something useful such as:

"I couldn't connect to the AI provider. Please check your connection settings."

It must not claim to have answered a question when no model response was received.

---

# PHASE 9 — PROVIDER SETTINGS REDESIGN

Improve the existing AI provider settings without unnecessarily changing the overall visual design.

The OpenAI provider screen should include:

**Provider:** OpenAI

**Authentication:** Configured / Not configured / Invalid

**Backend:** Online / Offline / Not required

**Model:** Actual selected model identifier

**Connection:** Verified / Not verified / Failed

**Last verified:** Timestamp

**Actions:**

- Test Connection
- Refresh Models
- Select Model
- View Diagnostics
- Retry Connection

Use green only for verified success.

Use clear warning states for configuration issues.

Show the actual error reason where possible.

Do not expose secret credentials in the interface.

## Connection persistence

A previously verified connection must not remain permanently green.

On app restart, distinguish:

- Previously verified.
- Currently checking.
- Currently verified.
- Verification failed.

Revalidate when appropriate.

Do not perform expensive inference tests continuously in the background.

---

# PHASE 10 — ADD REAL END-TO-END TESTS

Implement comprehensive automated tests.

Test:

1. Valid API credentials.
2. Invalid API credentials.
3. Missing API credentials.
4. Missing backend URL.
5. Backend unavailable.
6. Model discovery success.
7. Model discovery failure.
8. Selected model unavailable.
9. Successful inference.
10. Empty model response.
11. Malformed provider response.
12. HTTP 400.
13. HTTP 401.
14. HTTP 403.
15. HTTP 404.
16. HTTP 429.
17. HTTP 500.
18. Network timeout.
19. Streaming interruption.
20. App restart and provider state restoration.
21. Voice recognition to AI response.
22. Text-to-speech playback.
23. Provider switching.
24. Request cancellation.
25. No fake Connected state.
26. No production mock responses.
27. No credential leakage.
28. Preservation of existing user settings.
29. Preservation of holographic core functionality.
30. APK update installation behavior.

Use mocks for unit tests, but clearly label those tests as simulated.

## Mandatory live integration testing

When legitimate test credentials and network access are available, run a real provider integration test.

Verify:

- Actual authentication.
- Actual accessible model.
- Actual inference request.
- Actual returned response.
- Actual backend behavior.

Record the test outcome without exposing secrets.

If live credentials or network access are unavailable, explicitly report:

**LIVE PROVIDER VERIFICATION NOT PERFORMED**

Do not substitute mocked tests and call the provider verified.

---

# PHASE 11 — PHYSICAL REDMI 9 VALIDATION

Target my physical Xiaomi Redmi 9.

The device has already successfully installed the latest Jarvis APK through ADB.

Validate compatibility with the device's actual Android version.

Do not assume the phone is connected to your development environment unless ADB confirms it.

If the device is available, verify:

- Installation and update.
- App launch.
- Provider settings screen.
- Internet connectivity.
- OpenAI connection test.
- Model discovery.
- Model selection.
- Text conversation.
- Voice conversation.
- Core animation during AI processing.
- Background and foreground transitions.
- Recovery after network interruption.
- Persistence after app restart.

Capture relevant Android Logcat diagnostics when necessary.

Do not fabricate device testing.

---

# PHASE 12 — BUILD AND DELIVERY

After implementing the repairs:

1. Run the existing test suite.
2. Run newly added tests.
3. Build the debug APK.
4. Verify APK metadata.
5. Confirm package name and version.
6. Check signing compatibility with the installed version.
7. Avoid unnecessary changes to application identity.
8. Preserve existing user data during updates.
9. Produce the actual APK artifact.
10. Provide its verified filesystem path.

If the existing signing certificate is unavailable, report that clearly.

Do not claim that an APK can update the installed application without verifying signing compatibility.

Do not uninstall the current Jarvis application as a workaround.

If GitHub or deployment publishing is configured and authorized, prepare the appropriate changes and report what was actually published.

Never invent an APK download link.

---

# PHASE 13 — FINAL ACCEPTANCE CRITERIA

The project is not finished merely because it compiles.

It is complete only when the following are demonstrated, or clearly reported as blocked by unavailable credentials or infrastructure:

- [ ] Jarvis launches successfully.
- [ ] Existing holographic core remains functional.
- [ ] OpenAI configuration is valid.
- [ ] Authentication works with a real provider.
- [ ] At least one compatible model is genuinely accessible.
- [ ] Model selection works.
- [ ] A real inference request succeeds.
- [ ] Jarvis displays the actual model response.
- [ ] Jarvis can speak that response.
- [ ] The Connected indicator reflects genuine verification.
- [ ] Invalid credentials produce an accurate error.
- [ ] Insufficient API quota produces an accurate error.
- [ ] No mock AI responses are used in production.
- [ ] No secrets are exposed.
- [ ] Existing app data is preserved.
- [ ] Automated tests pass.
- [ ] A new installable APK is built.
- [ ] Physical-device testing is performed if a device is available.

## REQUIRED FINAL REPORT

At completion, provide:

**1. Root cause**

Exactly why the previous OpenAI integration failed, supported by code or logs.

**2. Files changed**

Every important modified file and its purpose.

**3. Backend status**

Actual backend deployment and connectivity status.

**4. OpenAI status**

Whether real authentication succeeded.

**5. Model status**

Exact verified model identifier, or the reason none could be verified.

**6. Inference proof**

A sanitized record of a real request and successful model response, if performed.

**7. Test results**

Automated, integration, and physical-device results separately.

**8. APK artifact**

Actual generated filename, path, version, and signing status.

**9. Remaining blockers**

Any missing credentials, quota, infrastructure, permissions, or manual actions.

**10. Exact next steps**

Commands or actions required to finish setup.

---

# NON-NEGOTIABLE ENGINEERING RULES

- Do not rewrite the entire app unnecessarily.
- Do not damage the working holographic core.
- Do not remove existing features.
- Do not replace real AI with canned responses.
- Do not create fake provider success states.
- Do not hardcode credentials.
- Do not expose API keys.
- Do not bypass provider restrictions.
- Do not confuse ChatGPT subscriptions with API access.
- Do not invent model identifiers.
- Do not fabricate test results.
- Do not report completion if real inference has not been verified.
- Do not uninstall the existing app or clear its data.
- Do not stop after changing the UI.
- Do not claim that passing unit tests proves real provider connectivity.

# FINAL INSTRUCTION

**START BY INSPECTING THE EXISTING REPOSITORY AND TRACING THE ACTUAL FAILED OPENAI REQUEST.**

Find the root cause, implement the smallest reliable architectural repair, add genuine verification, run the available tests, and build a working Jarvis APK.

I do not want an AI assistant that merely looks connected.

I want Jarvis to connect to a real authorized AI provider, select a real accessible model, receive real generated responses, speak those responses, and explain accurately whenever something fails.

**REAL CONNECTION. REAL MODEL. REAL RESPONSES. NO FAKE SUCCESS.**