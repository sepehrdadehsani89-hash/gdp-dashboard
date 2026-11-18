# J.A.R.V.I.S. 4.1 audit before implementation

Baseline: `62d3e8e86146157b6f35d648977acb971d0985d7`, clean `jarvis-next`, same fetched GitHub branch. Recovery branch: `checkpoint/jarvis-4.1-start-62d3e8e`. The latest directive is preserved verbatim in `JARVIS_4_1_DIRECTIVE.md`. No prior application, reference, asset or release file is removed.

## Architecture and environment

Native Kotlin/Java Android app with Compose screens, process-scoped AssistantRuntime, deterministic native commands, BrainProviderManager/LlmClient, encrypted FeatureVault/JarvisStore credentials, Room explicit memories, optional local JNI inference, foreground microphone and screen-capture services, and a Python HTTPS desktop companion. Package `com.jarvis.next.phoenix` in the retained debug preview; namespace `com.jarvis.assistant`; production ID `com.jarvis.next` is a different signing identity and is not substituted. Current release code 31, version 2.11.9-ai.1-debug. Existing JDK21/Gradle8.9/AGP8.7.3/Kotlin2.0.21, compile/target35, min29 and ARM64 remain compatible with Android12/API31 runtime APIs. Platform35 is installed. Platform31 is not needed to compile these APIs; no emulator or redundant SDK/AndroidX installation is assumed necessary merely to target an Android12 device. Disk approximately 50GiB free, memory limit 8GiB.

Existing Core/Lifecycle/Activity/Compose/Room/WorkManager/CameraX/OkHttp, speech recognition/TTS, Android Keystore, notification, media, Bluetooth/Wi-Fi APIs already exist. Permission declarations cover camera, audio, nearby devices, notifications, exact alarms, calendar, contacts, location and appropriate foreground services. Device admin is force-lock only. The existing accessibility and notification services are explicit optional Android special access. No root, protected system writes, shell execution or optional third-party automation app is needed for core controls.

MainActivity and AssistantRuntime own recognition → native routing / provider inference → validated final response → TTS, with generation fences and half-duplex microphone ownership. The repaired core is drawn by the existing UI/wallpaper paths, with reduced-motion preferences. These are preserved. Ephemeral conversation remains RAM-only; explicit memories use the unchanged Room v1 database. Do not convert private history into persistent or cloud data automatically.

## Confirmed gaps and root causes

- OpenAI Responses, strict terminal validation, honest current-session verification, encrypted API key/config persistence and official ChatGPT renewable authorization already exist. However, the selected ChatGPT plan path rejects an ordinary real question when the local proof is older than one hour, even if a saved refreshable grant is valid. That proof is inference evidence, not authorization to renew or answer a user request. Separate restoration/reconnection from billable model verification. Do not renew a badge with background inference or replay ambiguous token POSTs.
- App discovery uses visible launcher apps and exact/fuzzy label matching, but AppMatching.commandTarget does not strip the common “hey Jarvis” prefix and canonical English names can fail against localized launcher labels. Existing exact matches already dispatch directly. Preserve ambiguity review and background notification handoff, repair prefix/known-alias handling, disabled apps and missing launch-intent diagnostics. No particular handset launch error was supplied; these are source defects, not proof of an observed MIUI rejection.
- DeviceControl has 16 named controls, partial direct operations and a Control Center. Many natural command variants are not recognized; global operations dispatch that screen rather than a shared structured permission/action/result contract. Expand the existing engine through a typed AndroidControlManager, with actual readback or truthful user-action/unsupported results.
- Reminder commands currently open a calendar draft and explicitly do not save a reminder. Separate scheduled voice check-ins already persist/restore alarms, and WorkManager exists. Implement real owned reminder persistence and cancellation without replacing check-ins or changing the memory database.
- NotificationListenerService reads actual active notifications with sensitive-data filtering, but lacks a per-source allowlist and many requested message-summary intents. A disconnected listener can appear as an empty result. Preserve lock-screen protection; distinguish unavailable access from no readable notifications.
- Gmail/Google account integration is explicitly absent (RemovedMailIntegration). Owner confirms no Google Cloud Android OAuth client yet. Implement official incremental authorization/setup and scoped API clients; live access is USER ACTION REQUIRED until project, API/consent and package/signer configuration are complete. Never use ChatGPT/Google passwords, browser cookies or fabricated mailbox data.
- Perplexity is absent. Current official documentation uses the Agent API rather than the retired Sonar Chat Completions contract. Verify its current catalog and request/stream schemas before adding an API-key adapter.
- Farewell recognition covers a narrow completion set, lacks the requested catalog/context, and finishConversation calls normal speech completion which resumes wake handling. A genuine farewell must close active capture after speech, including failure/Stop, with independently explicit wake-standby behavior. Existing owner voice enrollment and secure Android unlock flows remain separate.
- The companion has authenticated QR pairing, certificate pins, signed command receipts and a dedicated Chrome profile, but Android stores a single pair. Only Chrome opening/search are currently allowed. Preserve the existing protocol and identity, extend target storage up to20 without overwriting existing pairs, and report unsupported desktop operations accurately. No Windows/remote host is available here for native execution verification.

## Existing feature boundary

| Area | Baseline status / scope |
| --- | --- |
| Core, wallpaper, reduced motion, owner voice, half-duplex/TTS | IMPLEMENTED; prior software tests; physical behavior unverified here |
| OpenAI / ChatGPT plan / Claude / Gemini / Local | IMPLEMENTED; mocked tests; no live runtime credentials in cloud |
| Perplexity | Not implemented; official current API must be integrated |
| Lock / secure unlock | IMPLEMENTED via lock-only administrator / Android authentication; no voice bypass |
| Torch / media volume | IMPLEMENTED with Android operations/readback; command coverage and errors need expansion |
| Wi-Fi / Bluetooth / hotspot / airplane / DND / battery saver | Partial official consent/settings workflows; never unrestricted global toggles |
| Screen recording | Screenshot MediaProjection exists; persistent video recording is absent |
| Orientation/theme/reading/cast/share/focus/bedtime/wallet | Mixed existing settings links and gaps; each must have an honest capability classification |
| App launching | IMPLEMENTED; wake-prefix/localized-name/diagnostic gaps; background launches require Android interaction |
| Reminders | Calendar draft only; owned alarm/check-in and work infrastructure retained |
| Notifications | Active notification read exists; per-app permission selection and accurate availability need repair |
| Gmail / Drive project discovery / Google service wizard | Absent; owner setup required for real authorization |
| Farewell catalog / context / mic shutdown | Incomplete; closure currently may resume wake |
|20 computers | Single pair currently; no fabricated desktop controls |
| Production Play release | Original production signer unavailable, target/native Play readiness constraints remain |

Baseline published evidence: 1,516 JVM cases, 1,515 passed, one optional private-export fixture skip; lint zero errors/55 warnings;23 static APK checks. These results belong to the baseline and must not be reused as results of this upgrade. No physical Android device, emulator, Google project grant or live provider credential is connected to this task. Tests of the new code must distinguish mock contracts, actual controlled local integration and unavailable handset/cloud checks.

## Implementation sequence

Repair current provider restoration and app routing first; integrate typed Android control handlers and permission-aware dashboard/test lab; then scoped Google/notification/reminder features, farewell closure and multi-target companion storage. Reuse existing stores, security/permission ownership, screen services and UI styling. Add only a justified compatible Google authorization library if needed; no general dependency upgrade. Run meaningful contract/lifecycle tests (20 or more relevant cases for each feature group), the existing regression suite, lint, actual APK/signing/asset inspection and available companion tests. Mark platform restrictions and unavailable live tests explicitly. Publishing still requires a concrete checkpoint and the owner's confirmation under the saved recovery directive.
