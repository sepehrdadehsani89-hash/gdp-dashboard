J.A.R.V.I.S. — UNIVERSAL AI BRAIN MANAGER

MASTER IMPLEMENTATION PROMPT

Real ChatGPT Authentication • Claude Integration • Removable Local Brain • Firebase Identity • Production Android Release

You are the principal software architect, Android engineer, authentication specialist, AI integration engineer, security engineer, and release engineer responsible for upgrading my existing J.A.R.V.I.S. Android application.

This is a real implementation task.

Do not produce a mockup.

Do not create fake login screens.

Do not create decorative connection buttons.

Do not report success without actual authenticated requests.

Your objective is to build an exceptionally polished AI Brain Manager that allows the user to choose between:

1. ChatGPT by OpenAI.
2. Claude by Anthropic.
3. A removable and reinstallable Local Brain.

The user should be able to change AI providers through a beautiful, intuitive settings interface.

The primary goal is a REAL ChatGPT connection using officially supported authentication.

Claude must also be integrated through legitimate provider-supported methods.

The Local Brain must become optional.

Preserve all existing working J.A.R.V.I.S. functionality.

⸻

1. PROJECT IDENTIFICATION

Repository:

sepehrdadehsani89-hash/gdp-dashboard

Target branch:

jarvis-next

Application:

J.A.R.V.I.S.

Primary device:

Xiaomi Redmi 9.

Technology:

Android, Kotlin, and the existing application architecture.

Before making modifications:

* Inspect the actual repository.
* Identify the current AI backend.
* Locate the Local Brain implementation.
* Identify installed model files.
* Inspect the existing settings interface.
* Identify Firebase dependencies, if any.
* Inspect authentication and account storage.
* Check the current microphone-to-AI-to-TTS pipeline.
* Inspect existing release workflows.
* Establish a recoverable Git checkpoint.

Do not delete anything until its purpose and dependencies are understood.

⸻

2. NEW AI BRAIN MANAGER

Create a dedicated section:

Settings → Artificial Intelligence → AI Brain Manager

The interface should feel like a premium futuristic operating system.

Display three provider cards.

CARD A — CHATGPT

Provider: OpenAI.

Logo: Official OpenAI branding used according to applicable guidelines.

Description:

“Connect your ChatGPT account to power J.A.R.V.I.S. with supported OpenAI intelligence.”

Buttons:

* Connect ChatGPT
* Sign in with ChatGPT, when officially available
* Continue with Google, when offered by the official sign-in flow
* Test Connection
* Set as Primary Brain
* Connection Details
* Disconnect

Status indicators:

* Not Connected
* Signing In
* Authorization Required
* Connected — Identity Only
* Connected — AI Access Authorized
* Usage Unavailable
* Session Expired
* Connection Failed

Never display “AI Connected” after identity authentication alone.

CARD B — CLAUDE

Provider: Anthropic.

Description:

“Connect a supported Claude integration to use Anthropic intelligence.”

Buttons:

* Connect Claude
* Sign In, if an officially supported third-party authorization flow exists
* Configure API Access, if required
* Test Connection
* Set as Primary Brain
* Connection Details
* Disconnect

Do not claim that signing into claude.ai automatically authorizes API requests.

CARD C — LOCAL BRAIN

Provider: On-device AI.

Description:

“Optional offline intelligence stored on your device.”

Buttons:

* Install Local Brain
* Remove Local Brain
* View Storage Usage
* Check Model Integrity
* Repair Installation
* Configure Offline Mode

Status indicators:

* Not Installed
* Downloading
* Installing
* Installed
* Removing
* Failed
* Repair Required

The Local Brain must not be mandatory for using ChatGPT or Claude.

⸻

3. REAL CHATGPT AUTHENTICATION

This is the highest-priority feature.

The user wants to select ChatGPT, tap Connect, choose their account, grant permission, and have J.A.R.V.I.S. genuinely communicate with an authorized OpenAI model.

Investigate the CURRENT official OpenAI documentation before implementing authentication.

Relevant official documentation:

https://developers.openai.com/siwc/quickstart

https://developers.openai.com/

https://platform.openai.com/docs

Determine whether this project qualifies for the official Sign in with ChatGPT open-source client flow or another supported integration.

3.1 Preferred authentication

Where supported, implement official Sign in with ChatGPT.

Use:

* Official OpenAI authorization endpoints.
* OAuth 2.0 / OpenID Connect.
* Authorization Code Flow.
* PKCE.
* Secure redirect URI handling.
* State validation.
* Nonce validation where required.
* Token validation.
* Secure token storage.
* Proper refresh and expiration handling.
* Official documented AI authorization scopes.

Use the system browser or Android Custom Tabs.

Do not collect the user’s Google password.

Do not build a fake Google login page.

Do not intercept credentials.

Do not scrape ChatGPT browser cookies.

Do not extract session tokens from the official ChatGPT app.

Do not reverse-engineer private ChatGPT endpoints.

Do not bypass provider subscription or authorization restrictions.

3.2 Google account selection

When the user taps:

Connect ChatGPT

Launch the official OpenAI authorization experience.

If OpenAI offers “Continue with Google,” allow the user to choose it within that official flow.

The user should be able to:

1. Select an existing Google account.
2. Add another Google account.
3. Complete the official OpenAI sign-in.
4. Review requested permissions.
5. Approve the connection.
6. Return securely to J.A.R.V.I.S.

The application must not falsely assume every Google account is already an OpenAI account.

If the user has an existing ChatGPT account, the official provider flow must determine the correct account identity.

3.3 Firebase integration

Firebase Authentication may be used for J.A.R.V.I.S.’s own account system.

However, Firebase and OpenAI authorization are separate systems.

Implement this distinction correctly.

Firebase may:

* Identify the J.A.R.V.I.S. user.
* Manage application sessions.
* Support Google sign-in.
* Store non-sensitive user preferences.
* Associate authorized provider connections with the application’s user identity.

Firebase must NOT be treated as an OpenAI access token.

A Firebase ID token does not automatically authorize OpenAI model inference.

Only official OpenAI authorization can grant OpenAI access.

If Firebase is not needed for the selected architecture, do not add it unnecessarily.

If Firebase is required, document the project configuration and use secure, environment-specific settings.

Never invent a Firebase project ID, OAuth client ID, or secret.

3.4 AI access authorization

After OpenAI identity sign-in, determine whether the user has separately authorized AI usage.

For official Sign in with ChatGPT plan-based usage, follow the documented scopes, eligibility, token handling, request contract, and usage limits.

If plan-based inference is unavailable to this application, explain that clearly and offer the officially supported API route.

Do not silently switch to paid API usage.

Do not claim that ChatGPT Plus automatically provides unrestricted API access.

Do not assume that identity sign-in gives access to ChatGPT conversations, memory, files, or private account data.

The user’s request is to use an AI brain, not to import their personal ChatGPT history.

3.5 Prove the connection works

After authentication, perform an actual authorized model request.

Example diagnostic request:

“Respond with the word READY.”

The response must come from the provider’s real supported AI endpoint.

Validate:

* Authentication success.
* Required authorization scopes.
* Token validity.
* Actual inference permission.
* Successful model response.
* Network timeout handling.
* Rate-limit handling.
* Permission-denied handling.
* Expired-session handling.

Only then display:

ChatGPT — AI Connected

If identity works but inference does not, display:

ChatGPT — Signed In, AI Access Unavailable

Do not disguise this failure.

⸻

4. REAL CLAUDE AUTHENTICATION

Implement Anthropic as a separate provider.

Use current official Anthropic documentation.

Official resources:

https://docs.anthropic.com/

https://console.anthropic.com/

Investigate whether Anthropic provides a supported third-party user authorization flow appropriate for this Android application.

If such a flow exists and the application is eligible, implement it correctly.

If not, use an officially supported API connection.

Never fabricate a Claude OAuth authorization endpoint.

Never reuse Claude Code login tokens in an unsupported Android integration.

Never scrape Claude browser sessions.

Never claim that Claude Pro automatically provides unrestricted API inference.

4.1 Claude connection workflow

The user taps:

Connect Claude

Then the app presents the supported connection method.

If provider account sign-in is officially available:

Open the official authorization page.

Complete account selection and permission approval.

Return to J.A.R.V.I.S.

If API access is required:

Explain that API access is separate.

Provide a secure configuration workflow.

Never embed a shared Anthropic API key in the Android APK.

Prefer a secure backend or another documented credential architecture.

4.2 Claude verification

Perform a real inference request.

Verify:

* Authentication or credential validity.
* Model availability.
* Permission to use the model.
* Actual response content.
* Usage restrictions.
* Error handling.

Only display:

Claude — AI Connected

after successful verification.

⸻

5. REMOVE AND REINSTALL LOCAL BRAIN

The user wants complete control over local AI storage.

Implement a real model-management system.

5.1 Remove Local Brain

When the user taps:

Remove Local Brain

Display a confirmation dialog.

Example:

“Remove the downloaded Local Brain models from this device? You can download them again later. Your J.A.R.V.I.S. settings and connected AI accounts will remain available.”

Buttons:

* Cancel
* Remove Downloaded Models

After confirmation:

1. Stop active local inference.
2. Release loaded model resources.
3. Close open model file handles.
4. Identify managed model files.
5. Delete only verified application-owned model files.
6. Preserve account credentials.
7. Preserve user settings.
8. Preserve the voice interface.
9. Preserve the holographic core.
10. Preserve cloud-provider connections.
11. Update available storage information.
12. Update the UI.

Do not delete unrelated files.

Do not delete application databases indiscriminately.

Do not delete shared system speech recognition components.

If part of the Local Brain is bundled inside the APK, explain that those bytes cannot be removed independently without changing the app package.

Make future downloadable model assets independently removable wherever feasible.

5.2 Reinstall Local Brain

When the user taps:

Install Local Brain

Show:

* Available model.
* Download size.
* Required free storage.
* Expected device compatibility.
* Offline capabilities.
* Installation status.

Implement:

* Official or otherwise authorized model download source.
* Download progress.
* Pause/cancel where supported.
* Resume where supported.
* File integrity verification.
* Safe installation.
* Failure recovery.
* Storage checks.
* Model initialization.
* Real inference verification.

Do not mark the model installed until it has been verified.

5.3 No mandatory Local Brain

When the user chooses ChatGPT or Claude, J.A.R.V.I.S. must work without requiring the local LLM.

However, preserve any small on-device components needed for legitimate microphone capture, wake-word processing, endpoint detection, and speech playback.

These components are not necessarily the same as the downloadable Local Brain.

Do not break microphone functionality by deleting required audio libraries.

⸻

6. PRIMARY BRAIN SELECTION

Create an elegant provider selector.

Options:

* ChatGPT
* Claude
* Local Brain

The user may select a provider only when it is actually available.

When selecting ChatGPT:

Verify OpenAI authorization and inference availability.

When selecting Claude:

Verify Anthropic authorization and inference availability.

When selecting Local Brain:

Verify the local model is installed and operational.

Do not mark a provider active if its required connection is unavailable.

6.1 Provider switching

When changing providers:

1. Finish or cancel the current request safely.
2. Update the selected provider.
3. Initialize the appropriate adapter.
4. Check connection status.
5. Update the interface.
6. Preserve user preferences.
7. Return to the normal assistant experience.

Do not restart the entire application unnecessarily.

6.2 Unified AI architecture

Build a provider abstraction such as:

AIProvider

AIProviderManager

OpenAIProvider

AnthropicProvider

LocalModelProvider

AuthenticationManager

ProviderConnectionValidator

ProviderSettingsRepository

Use the existing architecture where possible.

Do not introduce a second incompatible AI system.

The same voice pipeline should work regardless of provider:

Microphone → Speech Recognition → AI Provider → Response → Text-to-Speech.

⸻

7. PREMIUM FUTURISTIC USER INTERFACE

Design a polished AI Brain Manager.

Visual inspiration:

Advanced holographic operating systems.

Maintain the existing J.A.R.V.I.S. visual identity.

7.1 Provider cards

Each provider should display:

* Official provider name.
* Provider icon.
* Connection status.
* Selected model, when available.
* Primary Brain status.
* Connection action.
* Diagnostic action.
* Disconnect action.

Use elegant motion and clear status transitions.

7.2 Animation states

Disconnected:

Soft neutral appearance.

Connecting:

Subtle animated progress ring.

Authorization required:

Clearly labeled waiting state.

Connected:

Green confirmation accent.

Connection failed:

Controlled warning animation.

Primary Brain:

Distinctive active state.

Avoid excessive animation on low-power devices.

7.3 Account screen

Show only information that the provider legitimately makes available.

Examples:

* Display name.
* Email address.
* Provider connection status.
* Granted permissions.
* Last successful connection test.

Do not fabricate subscription tier information.

Do not expose access tokens.

Do not automatically import private conversations.

7.4 Buttons must work

Every button needs:

* A real action.
* A loading state.
* Success handling.
* Failure handling.
* Appropriate accessibility labels.

No decorative buttons.

No fake green checkmarks.

⸻

8. VOICE ASSISTANT INTEGRATION

The selected AI Brain must power the actual J.A.R.V.I.S. voice experience.

Example:

User: “Hey Jarvis, explain black holes.”

The microphone captures the request.

Speech recognition produces text.

The selected provider receives the request through its supported integration.

The provider generates a response.

Jarvis speaks the response.

The existing holographic core reacts to the correct voice state.

The wallpaper displays assistant captions according to the existing caption requirements.

Do not create an AI connection that works only inside a test screen while the actual voice assistant still uses an old or disconnected backend.

8.1 Preserve existing improvements

Do not regress:

* Adaptive end-of-speech detection.
* Background noise handling.
* Microphone lifecycle.
* Text-to-speech.
* Assistant speech captions.
* Animated holographic core.
* Live wallpaper.
* Charging particles.
* Device controls.
* Existing supported voice commands.

Verify the integration with these systems.

⸻

9. SECURITY ARCHITECTURE

Treat authentication as a security-critical subsystem.

Requirements:

* Official OAuth/OIDC flows only.
* PKCE for supported public-client flows.
* Secure redirect handling.
* State and nonce verification.
* Token expiration handling.
* Secure Android credential storage.
* Encrypted transport.
* No hardcoded secrets.
* No provider passwords collected by Jarvis.
* No embedded shared API keys.
* No unauthorized session reuse.
* No scraping private provider endpoints.
* No silent purchases or billing activation.
* No unauthorized background inference.
* No automatic access to conversations or files.
* Clear account disconnect and credential revocation behavior.

Use a secure backend only where technically required.

Do not create unnecessary infrastructure.

Never display an account as connected merely because its email address matches a Firebase user.

⸻

10. ERROR HANDLING

Implement real, understandable error messages.

Examples:

“ChatGPT sign-in was canceled.”

“Your OpenAI account is signed in, but AI access has not been authorized.”

“This integration does not support ChatGPT plan-based usage.”

“Your OpenAI API project has insufficient credits.”

“Claude API access is not configured.”

“The selected AI model is unavailable.”

“The Local Brain is not installed.”

“Connection timed out. Please try again.”

“Your session expired. Sign in again.”

Do not respond to every failure with a generic “Something went wrong.”

Do not claim that switching providers fixes account permissions.

⸻

11. IMPLEMENTATION ORDER

Execute in the following order.

Stage 1 — Audit

Inspect the existing repository.

Identify the current AI integration architecture.

Locate the Local Brain.

Locate existing authentication code.

Document findings.

Stage 2 — Provider architecture

Implement the unified AI provider interface.

Preserve the existing assistant pipeline.

Stage 3 — OpenAI authentication

Investigate and implement the official supported ChatGPT authorization flow.

Prioritize real AI inference authorization.

Verify actual model responses.

Stage 4 — Claude integration

Implement the official supported Anthropic connection method.

Verify actual model responses.

Stage 5 — Local Brain management

Implement removal, reinstall, storage verification, and recovery.

Stage 6 — Settings interface

Create the AI Brain Manager UI.

Connect every button to real functionality.

Stage 7 — Voice integration

Connect the selected provider to the actual microphone and speech-response system.

Stage 8 — Security verification

Test authentication, logout, token expiration, session recovery, and provider isolation.

Stage 9 — Regression testing

Verify the microphone, holographic core, wallpaper, charging effects, captions, and other existing features.

Stage 10 — Release

Build and verify the updated Android APK.

⸻

12. REQUIRED TESTS

Create meaningful tests for:

1. ChatGPT sign-in initialization.
2. OAuth callback validation.
3. Invalid authorization state.
4. Expired credentials.
5. Canceled sign-in.
6. Identity-only authentication.
7. Missing inference authorization.
8. Successful OpenAI inference.
9. Failed OpenAI inference.
10. OpenAI rate limits.
11. Claude credential validation.
12. Successful Claude inference.
13. Claude permission errors.
14. Provider switching.
15. Provider disconnection.
16. Local Brain removal.
17. Local Brain reinstall.
18. Interrupted model download.
19. Insufficient device storage.
20. Model checksum failure.
21. Voice request routing.
22. TTS response playback.
23. Microphone lifecycle.
24. App restart and session restoration.
25. Settings persistence.
26. Secure credential storage.
27. Network loss and recovery.
28. Wallpaper regression.
29. Holographic animation regression.
30. Android device compatibility.

Use additional tests where they meaningfully improve confidence.

Never invent test results.

If external credentials or provider approval are unavailable, clearly identify the blocked integration tests.

⸻

13. ABSOLUTE REQUIREMENTS

Do not simulate successful provider connections.

Do not use a fake OAuth popup.

Do not make Firebase authentication impersonate OpenAI authorization.

Do not pretend a ChatGPT subscription is an API key.

Do not pretend Claude subscription login automatically provides API access.

Do not bypass provider restrictions.

Do not claim that ChatGPT memory or chat history is available without separate authorization.

Do not delete the Local Brain’s required speech dependencies.

Do not damage the existing wallpaper.

Do not freeze the holographic core.

Do not remove working features.

Do not hardcode credentials.

Do not claim a release is complete if it does not build.

If a requested integration is not currently supported, implement the closest legitimate supported alternative and explain the difference.

Do not leave an unsupported connection button pretending to work.

⸻

14. FINAL RELEASE AND APK

After implementing the changes:

1. Compile the Android application.
2. Run available automated tests.
3. Verify authentication integration paths.
4. Verify provider adapters.
5. Verify Local Brain removal and installation.
6. Verify the voice assistant uses the selected provider.
7. Verify signing configuration.
8. Produce the APK.
9. Calculate the APK SHA-256 checksum.
10. Publish the artifact through an authorized release workflow.

Do not fabricate device-test results.

Do not fabricate authentication success.

Do not fabricate a download URL.

REQUIRED FINAL REPORT

Provide:

OPENAI

* Authentication method implemented.
* Whether Sign in with ChatGPT is supported.
* Whether plan-based inference is authorized.
* Whether real AI inference was tested.
* Any outstanding authorization requirements.

CLAUDE

* Authentication method implemented.
* Whether real inference was tested.
* Any API or account requirements.

LOCAL BRAIN

* Whether downloaded models can be removed.
* Whether they can be reinstalled.
* Storage freed.
* Any retained required audio components.

APPLICATION

* Provider switching status.
* Voice assistant integration status.
* UI functionality.
* Test results.
* Known limitations.

RELEASE

* Version.
* Package identifier.
* Build result.
* Signing status.
* SHA-256 checksum.
* Release commit.

FINAL OUTPUT

End your response with:

J.A.R.V.I.S. APK DOWNLOAD LINK:

Provide the actual verified HTTPS URL for the newly generated APK.

If publishing is blocked, state the verified artifact path and explain why a public link is not yet available.

Never invent a link.

BEGIN IMPLEMENTATION NOW. PRIORITIZE REAL CHATGPT CONNECTIVITY, THEN CLAUDE, THEN REMOVABLE LOCAL BRAIN MANAGEMENT.