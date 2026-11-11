# J.A.R.V.I.S. — FINAL POLISH MASTER PROMPT
## Four Advanced Intelligence Engines + Video 6158 Repair | Preserve Existing Application

**ROLE:** You are a principal Android engineer, audio and speech engineer, on-device AI engineer, machine-learning evaluation specialist, desktop companion architect, security engineer, QA lead, and motion designer. Your job is to **finish and repair the existing J.A.R.V.I.S. app**, not to start over. Treat this document as a source-code implementation brief, not an invitation to generate only a concept, mockup, or list of suggestions.

**SOURCE MATERIAL:** Inspect the entire current application repository and the previously provided `IMG_6158.mp4` video, plus the accompanying `JARVIS_VIDEO_6158_REPAIR_MASTER_PROMPT.md`. If the original recording is available, analyze its full audio and frames; maintain timestamps for every demonstrable issue. The earlier video repair document observed a circular cyan HUD with a central battery display reading 100%, changing colors, repeated wake attempts, incomplete recognition, failed actions, and insufficiently reactive ring/particle animation. Treat the video observations as *reproduction targets*, not proof that any particular root cause has been established. If an utterance cannot be transcribed reliably, mark it uncertain and use runtime logs or a guided reproduction, not a guessed quotation.

**SCOPE:** Implement exactly these **four new premium capabilities** and incorporate all requirements from the Video 6158 repair brief:

1. J.A.R.V.I.S. **Auto-Repair Intelligence** — real diagnostics, bounded safe repair, meaningful explanations, verifiable results.
2. J.A.R.V.I.S. **Neural Evolution Engine** — opt-in correction learning, custom vocabulary, editable personal commands, measurable improvements.
3. J.A.R.V.I.S. **Predictive Intelligence** — user-configurable routine suggestions and useful just-in-time assistance, never intrusive or presumptuous.
4. J.A.R.V.I.S. **Universal Mission Control** — secure Android-as-microphone-and-speaker control of authorized Windows, macOS, and Linux desktop companions, with monitored execution.

**IMPORTANT EXCLUSION:** Do **not** add a fifth separate product or module called “Living Intelligence Core.” The existing core must nevertheless get the **specific animation repairs and state synchronization already required by the Video 6158 repair document**. This is repairing an existing UI, not proposing the removed fifth idea.

**ABSOLUTE NON-DESTRUCTIVE REQUIREMENTS:** Preserve the current recognizable wallpaper, J.A.R.V.I.S./SENTINEL circular core, battery percentage in its center, cyan futuristic HUD, time/weather/telemetry panels, existing settings and permissions, trusted contact workflows, installed-app opening, local brain, wake phrases, optional cloud gateway, animation assets, offline capability, prior user data, and any working device pairing. Do not switch framework or replace the project without proving an unavoidable technical necessity and receiving user approval. Make migrations additive and reversible. Never delete the training datasets, settings, profiles, or offline model assets as a convenience. The user is at the **finishing-touches stage**: reliability, polish, production quality, and measurable correctness outweigh adding a thousand fake buttons.

---

# CHAPTER 0 — FIRST AUDIT, SOURCE OF TRUTH, BASELINE AND FREEZE

Before modifying source code, create the following artifacts inside the repository (adapt names if the codebase has an existing structure):

- `docs/finish-polish/Baseline.md`: current app architecture, Gradle/build versions, supported Android versions, foreground services, AI gateway configuration, local model status, permissions, actual voice pipeline, rendering mechanism, desktop companion status, pre-existing features, known defects, existing tests, exact reproduction conditions from the video.
- `docs/finish-polish/Video6158_Timeline.md`: time-coded observations and any successfully verified spoken instructions; clearly label unresolved speech.
- `docs/finish-polish/Feature_Matrix.md`: each requested function with planned/implemented/verified/blocked classifications, device/platform limitations and acceptance tests.
- `docs/finish-polish/Performance_Baseline.md`: wake detection latency, speech-to-text accuracy, time-to-first-local-answer, foreground/background behavior, memory, thermal and battery impact, UI frame quality, ANR/crash rate. Do not invent numbers; include "not measured" when necessary.
- `docs/finish-polish/Rollback.md`: version control checkpoints and database migration reversal strategy.

Freeze the original theme and HUD through reference screenshots. Add automated visual regression tests where feasible. Avoid changing its iconography, branding, core position, typography hierarchy, layout organization, app name, package name, or existing public APIs except where an actual defect requires a compatible change.

### Global definition of success
A task counts as successful only when its actual API, OS or device postcondition is verified. A speech response that *sounds* successful is not proof. A running visual animation that merely pretends to show progress is not evidence of execution. Local model answers must not invent current facts; the Google/account/cloud integrations must not claim permissions that do not exist.

Use this rule for every subsystem: **OBSERVE → UNDERSTAND → PLAN → AUTHORIZE → EXECUTE → VERIFY → REPORT → LEARN (OPT-IN)**.

---

# CHAPTER 1 — VIDEO 6158: FIX THE REAL VOICE PIPELINE FIRST

## 1.1 One utterance is enough

Repair the entire path: microphone/audio capture → low-power wake detection → post-wake ASR → normalized transcript → contextual intent and target resolution → deterministic action or local conversational answer → actual result verification → natural TTS → follow-up window → wake-ready.

Required phrases: `Hey Jarvis`, `Jarvis, are you up?`, and a user-configurable optional short phrase. If someone says `Hey Jarvis, what time is it?` in one breath, **do not interrupt or discard the remainder**. If someone says only `Hey Jarvis`, acknowledge once and immediately start listening for the next command without requiring another wake phrase. Debounce the detector; prevent duplicate greetings and ghost detections of J.A.R.V.I.S.'s own spoken voice.

Implement one authoritative microphone session owner and one observable voice state machine with interruption/error transitions:

`OFF → WAKE_READY → WAKE_DETECTED → LISTENING → TRANSCRIBING → INTERPRETING → PLANNING → EXECUTING|ANSWERING → SPEAKING → FOLLOW_UP → WAKE_READY`

Also include `MUTED`, `PERMISSION_REQUIRED`, `MODEL_UNAVAILABLE`, `AUDIO_INTERRUPTED`, `CANCELLED`, and `ERROR`. Every transition has an initiating event, start/end timestamp, cleanup behavior and a user-readable cause if it fails. Restore wake detection after normal interaction; do not run multiple speech recognition sessions simultaneously. Manage audio focus, mic selection, Bluetooth switching, speech endpoints, TTS callbacks and actual completion.

### Example sessions

- `Hey Jarvis.` → `Always ready, sir. How may I assist you?` → J.A.R.V.I.S. listens → `What time is it?` → speaks the *real* local time.
- `Jarvis, are you up?` → `Always, sir. What can I help with?` → `What's your name?` → `I'm J.A.R.V.I.S., your personal AI assistant.` (no HTTPS gateway needed).
- `Hey Jarvis, open Chrome.` → launch via supported Android package/deep-link flow, check result, respond accordingly.
- `Hey Jarvis, tell me why stars shine.` → answer from genuinely available local knowledge/model if possible; if online sources are needed, state this accurately without pretending the words were inaudible.
- `Open YouTube and search for astronomy.` → use the target application's official intent/deep-link when available, else a supported browser search alternative; do not fabricate a successful in-app search.

## 1.2 Never use the wrong error message

Track independent stages and separate these failures: wake phrase not recognized; microphone busy or denied; intelligible words not recognized; transcript obtained but user intent ambiguous; command unsupported; selected application absent; local model missing or out of RAM; backend unavailable; target desktop offline; OS permission denied; action dispatched but not verified; TTS unavailable; animation render paused. Use `I didn't catch that` **only** for genuine ASR failure, never for command routing or server errors. Use a brief natural explanation and an actionable next step for other errors.

## 1.3 Real local answer routing

Time, date, time zone, battery, charging status, app launching, supported local alarms, offline arithmetic, known app identity, saved preferences, and verified device telemetry use deterministic handlers. Do not send simple commands to a language model by default. For open-ended questions, route to authorized on-device model if installed. Cloud HTTPS inference is optional, not required for basic competence; preserve advanced gateway settings under an advanced menu rather than demanding tokens for trivial queries. An optional local model, including an approximately 2 GB compressed package if devices can actually support it, must have a smaller low-memory option, license checks, size checks, checksum validation, download opt-in, measured RAM and thermal thresholds, and honest offline limitations. Never promise an answer to every fact without evidence.

## 1.4 Supported background and lock-screen behavior

When the user deliberately enables persistent voice mode, use the supported Android microphone foreground service architecture and an ongoing notification, pause/stop controls, appropriate user-visible permissions and privacy indicators. The service must start through an Android-permitted route, normally when the app is visible. Android 14+ has additional while-in-use microphone restrictions; no unauthorized reboot or background restart, force-stop evasion, hidden recording or lock-screen bypass. When Android blocks a wake-up or Activity launch, disclose the limitation and provide an explicit tap-to-continue route. A voiceprint is never a replacement for strong device authentication. Do not attempt PIN entry, bypass fingerprint verification, or secretly inspect other applications.

## 1.5 Respectful response personality

Default: professional, concise, polite and adaptable. Examples (variants, not fixed scripts): `Always ready, sir.` `At your service.` `I'm listening.` `Opening Chrome, sir.` `Your battery is at 64%.` `I heard your request, but the target app is unavailable.` Avoid excessive repeated greetings, excessive use of `sir`, or blocking the command behind an unskippable TTS introduction. Confirmation speech must follow actual results, not precede them. Provide quiet mode and TTS rate/volume options.

---

# CHAPTER 2 — VIDEO 6158: PERFECT THE EXISTING HUD AND BATTERY SIGNALS

**Do not rebuild or replace the wallpaper.** Keep the user-approved dark-black/cyan HUD, original circular orbital design, existing outer rings, side telemetry panels, battery percentage in the middle, and current branding. Change the rendering behavior, not the identity.

A wallpaper-only PNG cannot animate. Inspect whether the current graphic lives in an Activity, Compose/Canvas view, Android WallpaperService, animated renderer or other platform component. Implement the appropriate real renderer and map it to an immutable/observable `AssistantUiState`. Support a live wallpaper only if that is how the existing app is designed; keep wallpaper/app rendering separated from microphone and AI service lifetimes.

Visual state contract:

- **CYAN — standby/idle:** low-power slow orbital rotation, faint breath of light, subtle particles. If wake service is stopped, accurately display offline/paused rather than faking active listening.
- **PURPLE — wake/listening:** brief smooth activation sweep, violet arcs, audio-level/rms-driven wavelets from **actual microphone energy**, no harsh strobing. Listening effects end promptly when the mic closes.
- **CYAN-BLUE — reasoning/action:** controlled energy tracing, meaningful phase label (`INTERPRETING`, `RUNNING`, `VERIFYING`) backed by the real current state, not a fictional loading spinner. Important: this is a repair to the existing video animation, not a new fifth feature.
- **ELECTRIC BLUE — speaking:** synchronized visual response to real TTS/audio playback (validated available playback envelope), with animation that starts, pauses and stops with the speech engine. No fake TTS playback state.
- **GREEN SECONDARY CHARGING PARTICLES:** small green energy dots orbiting outside the central reactor when battery status indicates genuine charging; preserve the primary listening/speaking color. Unplugged or plugged-but-not-charging states must be distinguished.
- **AMBER — genuine error:** a restrained short alert, then return to the correct actual state; avoid repeated flashes and health-anxiety messaging.

Keep the center legible: e.g. `66%` as the **actual** battery, not a hardcoded texture or demo value. Get battery updates from Android `ACTION_BATTERY_CHANGED` fields `EXTRA_LEVEL`, `EXTRA_SCALE`, `EXTRA_STATUS`, `EXTRA_PLUGGED`, validate input, compute battery percentage, and correct charging state semantics. A recording showing `100%` is not proof the phone is truly full; verify it on real hardware. If fully charged *and still connected to power*, allow a single-per-charge configurable short notice; respect silence/quiet hours and avoid infinite repeats. Recheck real state when returning to the app.

Motion engineering: smoothed microphone RMS and noise gating; measured TTS output level if available; variable particle count based on hardware; hardware-accelerated rendering where suitable; proper canvas invalidation and service lifecycle; consistent easing; no unnecessary GPU animation while app surface/wallpaper isn't visible; battery saver and reduced motion; debug FPS; target up to smooth 60 fps on capable devices without promising it everywhere. No new navigation buttons are required; preserve nonfunctional wallpaper as display-only and put real tappable actions only in the actual app layer.

---

# CHAPTER 3 — PREMIUM ENGINE 1: J.A.R.V.I.S. AUTO-REPAIR INTELLIGENCE

## 3.1 Product promise
The assistant can answer `Hey Jarvis, why aren't you understanding my voice?`, actually check the diagnostic signals it can access, explain likely causes, and safely resolve only problems it is authorized and able to fix. It must never claim to repair Android OS restrictions, missing hardware, revoked user permissions, broken networks, or third-party apps without evidence.

## 3.2 Observability and health probes
Design a typed **HealthProbe registry**. Each probe has ID, owner, required permission, supported platform, measurement/certainty, last sampled timestamp, privacy level, failure category, remediation suggestions and evidence references. Include, as applicable:

- microphone permission, capture session ownership, audio input available, input level/clipping, audio focus conflict, wake engine initialized, wake false positives, available local recognition language/model;
- speech recognition start/result/end callbacks, empty transcripts, noise/environment indicators, wake confidence, language mismatch, mic-to-TTS overlap, playback echo, Bluetooth route changes;
- TTS engine initialization, selected voice availability, speaking callback completion, audio playback routes, volume settings;
- local model installed and cryptographically verified, model memory fit, load success, token throughput, knowledge retrieval results, context size, fallback routing, cloud gateway authentication/transport when enabled;
- local battery true percent and charging state, service restrictions, foreground service notification status, wake service availability;
- app package/deep-link availability, action registry compatibility, denied permissions, postcondition failures;
- desktop pairing health, certificate identity, device availability, command queue state, OS agent version mismatch, timeouts;
- animation state stream validity, actual rendering, low frame rate, view not attached, wallpaper visibility, dropped frames, excessive GPU or CPU activity;
- app crashes/ANRs, device RAM pressure, thermal status and storage space.

## 3.3 Severity, diagnosis and explanations
Classify issue severity as `INFO`, `DEGRADED`, `ACTION_NEEDED`, `CRITICAL` (reserve CRITICAL for real app/service failures, not decorative alerts). Include symptoms, verified evidence, confidence, impact, safe choices, and status. Show `Unknown—not enough information` instead of inventing a root cause. Correlate related symptoms into one clear issue: for example, `Voice recognizer started before TTS completion` rather than three redundant alerts. Avoid saying `microphone broken` if the recognizer merely ran into a backend error.

For the spoken command `Why didn't you hear me?`, generate a concise explanation backed by health probes: `Your microphone permission is enabled, but speech recognition ended before you finished speaking. I can change the endpoint setting and run a test.` Show an optional diagnostic detail panel.

## 3.4 Repair tiers and authorization
- `T0: Read-only`: collect health signals, run self-test, explain settings, no mutation.
- `T1: Safe app-local fix`: clear invalid in-memory session state, release a stale microphone resource, rebind TTS, reset a bounded retry timer, refresh visible animation observers; may run automatically only when side effects are trivial, reversible and disclosed in settings.
- `T2: User-approved app setting fix`: adjust sensitivity, swap available installed model, reconnect paired desktop, restore speech voice or preferred mic, rebuild a local index; show action preview and ask first.
- `T3: Operating-system action`: open Android microphone permission settings, battery settings or relevant system panels; never silently grant OS permissions or circumvent restrictions.
- `T4: External or destructive`: wipe model data, delete files, reinstall packages, send messages, alter remote configurations: require explicit targeted confirmation, backup plan where available and result verification. No unrestricted shell or arbitrary self-modifying generated code.

Every repair has `preconditions`, `risk`, `expected outcome`, `rollback/cancel path`, `verification method`, `timeout`, `max retries`, and `lastKnownGoodState`. Prevent repair loops through a cooldown, circuit breaker and retry budget. On failure, return to a known-safe state and report that it failed; never repeat the same repair forever.

## 3.5 Interface, interaction, voice and animation
Add a **small, integrated, expandable System Doctor panel** consistent with existing cyan HUD, not a replacement home screen. Its design contains `Systems checked`, `Issue found`, `Recommended fix`, `Fixing`, `Verifying`, `Resolved/Needs help`. Prefer small crisp labels and minimal motion. Use a blue-cyan scan accent around the original core during an actual test; a short amber cue on confirmed failure, blue/cyan on confirmed success. Do not create a separate Living Core feature. No invented CPU, memory or connection percentages. Allow keyboard/touch accessibility and disable animation in reduced-motion mode.

Spoken examples:
- `Sir, I found a microphone timing issue. I can run a safe reset and retest. Shall I proceed?`
- `The offline model isn't installed yet. Local commands are still available.`
- `Your computer is paired, but its companion is offline. Please open the desktop agent.`
- `The repair is complete. I verified that wake detection is working in this test.` (only if verified)

## 3.6 Automated regression coverage
Reproduce `IMG_6158.mp4` scenarios and deliberately inject `RECORD_AUDIO` revoked, microphone busy, empty transcript, TTS failure, app missing, model OOM, cloud offline, desktop unavailable, corrupted local index, force-stop, animation surface paused, and battery charging transitions. Each must produce a correct diagnostic, not a generic phrase. Make diagnostic logs structured, short-lived and redacted by default.

---

# CHAPTER 4 — PREMIUM ENGINE 2: J.A.R.V.I.S. NEURAL EVOLUTION ENGINE

## 4.1 Product promise
J.A.R.V.I.S. becomes better at understanding the **specific user's chosen phrases and preferences** over time, without claiming to update the foundation model's weights automatically. The experience is editable, reversible and consent-based. No invisible audio collection, no sharing user corrections with other accounts, no permanent training from private messages without express opt-in.

## 4.2 Teach-by-example command learning
Example: `Jarvis, when I say start my coding workspace, open Chrome, VS Code, and my programming folder on the paired laptop.` Convert this utterance into a structured proposed recipe: trigger phrases, entities, target device, allowed action IDs, prerequisites, permission checks, sequencing, parallel safe steps, expected postconditions and error recovery. Show the proposed recipe for review. Execute only after it is enabled. Do not infer permission to install software, run arbitrary scripts or access unrelated private files.

Support multiple synonyms, aliases and user-preferred app names. Resolve `Dad` and other saved contact nicknames only within the trusted address book and check ambiguity before dialing. For sensitive commands, a trained voice profile is a convenience signal, not strong authentication.

## 4.3 Learning sources and settings
- explicit correction: `No, I meant Calendar, not Calculator`;
- approved preferred vocabulary: names, multilingual project titles, contact nicknames, app names, and code terms;
- explicit tone preference: shorter answers, explain in Spanish, speak only when not in class;
- accepted vs rejected suggestions (with permission and retention limits);
- verified action outcomes and typed reasons for failures (avoid learning from falsely reported successes);
- optional imported, vetted **2,000-question / 6,000-answer evaluation library** as evaluation content, not proof of neural training or license to emit hallucinations.

Use an offline/local personal preference store. Separate `explicitMemory`, `derivedPreference`, `temporaryConversationContext`, `speechVocabulary`, `userRecipe`, and `evaluationAggregate`. Tag source, last revision, confidence, expiry and user approval. Temporary/hypothetical statements must not become persistent profile facts. Add `Forget this preference` and a global export/delete function. Do not auto-train on background ambient conversations.

## 4.4 Personalization engine
Use explicit semantic intent mapping and versioned command patterns; if available use embeddings or a compact local intent classifier. Avoid keyword-only matching. Separate entity resolution (`which app, file, person, device`) from intention (`open`, `search`, `call`, `draft`, `send`, `delete`). Preserve distinctions like `Draft an email` vs `Send an email` and `Open Chrome` vs `Search inside Chrome`. When a trigger collides with an existing system command, show a conflict and let the user resolve it.

Present the minimal needed clarification: `Do you mean your laptop or this phone?` rather than asking five questions. Maintain a low confidence threshold policy: uncertain sensitive actions require direct confirmation; low-risk local queries may retry or offer options. Voiceprint similarity must not authorize unlocking the device, bypassing credentials, sending money or exposing secrets.

## 4.5 Interactive refinement UI
Introduce a subtle **Teach J.A.R.V.I.S.** page under existing Settings, or an expandable existing memory panel. Provide `What I heard`, `What you meant`, `Remember this phrase?`, `Try it`, `Save`, `Undo`, `Delete`, `Export`. Preserve the cyan visual language and gentle ring pulse for a successfully *saved* approved correction. Never show `Trained!` for a correction that did not persist. Avoid gamified or intrusive quality scores; show factual metrics only.

## 4.6 Metrics and model evaluation
Measure intent accuracy, false execution, correct entity selection, wake detection, successful commands and accepted/rejected suggestions on a user-consented anonymized test set. Compare before/after corrections and permit rollback to a previous recognition profile. No blanket claim of “learning everything” or “perfect memory.” Keep all improvement features functional offline where feasible.

---

# CHAPTER 5 — PREMIUM ENGINE 3: J.A.R.V.I.S. PREDICTIVE INTELLIGENCE

## 5.1 Product promise
J.A.R.V.I.S. makes useful suggestions at the right moment **only when enabled**. It should anticipate an appropriate next step based on the user's approved calendar, reminders, selected routines, device health or connected services without secretly searching private data, recording conversations, or taking irreversible actions.

Examples:
- `Your class starts in 20 minutes. Want me to open the notes you saved for it?` (only if schedule is connected and event verified).
- `Your phone is at 10%. Would you like battery saver settings?` (actual battery data).
- `Your coding session is on your calendar. Shall I prepare your paired Windows workspace?`
- `You usually review assignments in the evening. Want me to show today's tasks?` (only if user approved pattern learning).
- `You're charging at 100%. You can unplug if you want.` (only when actually connected, cooldown once per charging session).

## 5.2 Suggestion inputs and consent
Supported inputs are explicit user routines, authorized calendar, opted-in device state, manually chosen locations, authorized connected Google services, and verified task status. Every input should state `source`, `consent`, `freshness`, `confidence`, and retention policy. Do not infer school, medical conditions, location, personal relationships or sensitive identity characteristics from unapproved data. Location-triggered reminders require the appropriate OS permission and clear opt-in. Do not claim live external information if network is unavailable.

## 5.3 Prediction versus execution
Categorize suggestions:
1. **Informational**: quiet, passive reminder, no device action.
2. **One-tap preparation**: show files, open apps, start a preapproved focus timer.
3. **Optional automation**: run a saved recipe on a paired authorized device after permission and current-state validation.
4. **Sensitive external action**: sending a message, editing shared files, contacting someone, sharing location, changing external configurations; explicit review/confirmation.

Never silently send communications, buy anything, transmit location or wake someone else's device merely because the system predicts a desire. Offer `Not now`, `Never suggest this`, `Snooze`, `Adjust routine`, `Why am I seeing this?`.

## 5.4 Suggestion relevance and anti-spam
A suggestion score may combine deadline proximity, explicit priority, recent user preferences, verified availability and suppression rules, but it must not fabricate confidence. Respect quiet hours, Focus/DND, battery saver, and user limits. Cap repeated prompts and do not create urgency where none exists. A declined suggestion should decay. After a missed condition, do not spam the user with the same stale alert. Use natural short acknowledgments and keep a preference-driven `voice vs text vs silent` delivery setting.

## 5.5 Predictive dashboard design
Add **a concise, dismissible Future Actions strip** to existing dashboard only when relevant; do not expand the main UI into multiple oversized cards. Dark glass HUD styling, cyan text, small icons, micro-animation for time-sensitive events, clear timestamps and sources. No fake schedules or prefilled imaginary appointments. Battery and weather suggestions must show the actual source and last updated time. When user prefers quiet mode, do not speak unsolicited messages.

## 5.6 Evaluation
Test true/false relevance, timing around time zones and daylight-saving changes, expired calendar grants, no internet, missed events, duplicate notifications, local charging/discharging transitions, denied location, opt-out, re-enabled reminders and conflicting routine settings. Track dismissals and user acceptance *without* creating a compulsive notification loop.

---

# CHAPTER 6 — PREMIUM ENGINE 4: UNIVERSAL MISSION CONTROL

## 6.1 Product promise
The Android device becomes J.A.R.V.I.S.'s **voice microphone and speaker** for a Windows, macOS or Linux computer even if the computer has no microphone or speaker. A small legitimate desktop companion executes **only allowed actions** after pairing. Example:

`Hey Jarvis, open Chrome on my computer and search for ChatGPT.`

Flow: wake speech captured on Android → local recognition → target computer selected → signed typed command sent to desktop companion → app/browser opens and navigates via supported OS/browser mechanisms → companion checks result → result returned to phone → J.A.R.V.I.S. speaks actual status. No hidden spying, no unrestricted arbitrary shell as a default, and no false statement that a program launched when it failed.

## 6.2 Companion architecture
Build or integrate a properly installed desktop companion for the user's supported OS. Prefer reuse of existing pairing logic. If no companion exists, add a maintainable cross-platform agent using an appropriate supported framework, with system-specific adapters and a **capability manifest**. Supported basic actions may include:

- list available approved capabilities, connectivity and app presence;
- launch a named installed app using supported OS launch mechanisms;
- open an authorized URL in the chosen browser;
- perform a safe search through a supported browser URL or app integration;
- show an approved local document or directory within user-authorized folders;
- report battery, volume or selected device telemetry when the OS exposes it;
- present a desktop notification;
- transfer a user-chosen file after explicit confirmation;
- prepare or restore a selected workspace of applications and documents;
- return status, logs and screenshots only with the user's active consent.

Provide a separate `SUPPORTED`, `REQUIRES_CONFIRMATION`, `UNAVAILABLE`, and `PERMISSION_DENIED` status per action and platform. Do not pretend every OS offers identical app accessibility, window management, clipboard, input simulation or media APIs. If a third-party app has no supported search entry point, offer a browser search or ask the user to open the app manually. Screen content and microphone relay must be user-controlled and visibly active.

## 6.3 Secure pairing and transport
Pair using a displayed, short-lived code or authenticated QR handshake with explicit consent on both devices. Generate device-specific credentials; authenticate both endpoints; encrypt traffic in transit and use replay/nonce protection, command IDs, expiry times, per-device permission scopes, rate limiting and audit trails. Permit local network operation; add relay infrastructure only if already authorized and genuinely needed. Do not accept unauthenticated LAN requests, unrestricted network scanning, persistent covert access or arbitrary commands from external sources.

Give each desktop device a friendly name (`My Laptop`, `Windows PC`) and show `Connected`, `Offline`, `Awaiting Approval`, `Permission Needed`, `Running Task`, `Verified`, `Failed`. Handle disconnection and duplicate action recovery. Provide a `Disconnect and revoke` action. Avoid exposing persistent tokens in logs or UI.

## 6.4 Desktop tool protocol
Use typed command objects conceptually like:

- `request_id`, `user_id`, `device_id`, `action_id`, `args`, `expiry`, `confirmation_state`, `idempotency_key`;
- response fields: `accepted`, `executed`, `verified`, `verification_evidence`, `error_code`, `duration_ms`.

Never treat `accepted=true` as an actual success. Execution result and verified postcondition are distinct. Make cancellation propagate when possible. When a tool is unavailable, report that before executing other dependent steps.

## 6.5 Mission planning
Convert natural-language requests to typed plans with dependency order and visible progress. Examples:

- `Start my coding workspace`: select paired desktop, check authorization, verify VS Code/Chrome exists, launch requested apps, open the selected project, report the actual results.
- `Find my project files on the laptop`: only search previously authorized folders, return file titles with links, never enumerate arbitrary sensitive directories.
- `Open Chrome and search for J.A.R.V.I.S. Kotlin tutorials`: launch browser and navigate to supported search URL; verify browser navigation.
- `Open it there`: resolve `it` and `there` from the present conversation and connected-device context, ask one question if ambiguous.
- `Send this file to my phone`: use explicit file selection, present transfer size and target, send through authenticated channel, verify checksum and arrival.

Require review for multi-step plans that contain any sensitive or destructive action. Provide a clear stop button, timeout and safe retry policy.

## 6.6 Audio/screen constraints
Android microphone/TTS works as the main voice endpoint. Desktop voice output may optionally stream to Android, but only from approved capture routes; don't promise capturing protected audio from every application. If the PC lacks speaker/mic, spoken J.A.R.V.I.S. answers come from the phone. When Android is locked, do not bypass security to launch restricted activities; service restrictions apply. If screen capture is offered, show a meaningful user consent flow and clear visual indicator; do not silently watch other applications.

## 6.7 Mission Control UI
Keep the main circular HUD unchanged. Add a compact **Mission Control** overlay/drawer with connected devices, a selected target, active job stages `Requested → Authorized → Sent → Running → Verified`, an optional task history, retry/cancel, and permission diagnostics. A real animated cyan progress tracer may follow actual events; purple listening and blue speaking states remain accurate. Do not create disconnected cosmetic spinners. No new wallpaper-only fake buttons.

## 6.8 Platform acceptance matrix
Test Windows 10/11 where supported, current supported macOS, representative Linux desktops including Wayland/X11 restrictions when relevant. Report which capabilities work on each; provide installer/build instructions only for actually supported target architectures. If cloud signing or a desktop environment is absent, do not claim successful installers; supply verified source and build workflow.

---

# CHAPTER 7 — UNIFIED CROSS-ENGINE BEHAVIOR

These are four integrated engines sharing *one state model*, not four disconnected chatbots. Implement a `VoiceSessionCoordinator`, `IntentRouter`, `CommandRegistry`, `ActionVerifier`, `SystemDoctor`, `LearningStore`, `SuggestionScheduler`, `DeviceRegistry`, `MissionExecutor`, `BatteryTelemetrySource`, and `AssistantUiState` as logical modules or interfaces according to the current architecture. Use explicit well-tested contracts, dependency injection if already used, and bounded lifecycle scopes.

### Representative interaction A — self-repair
`Hey Jarvis, why didn't you hear me?` → wake local → System Doctor checks recognizer callbacks, permissions, mic ownership and noise data → proposes a **permitted** recovery → user accepts if required → bounded fix → verification sample → truthful status → optional approved correction saved.

### Interaction B — custom phrase learning
`Jarvis, when I say coding time, open my project on the laptop` → identify the actual laptop → show proposed action recipe → user authorizes → save in personal profile → later `Hey Jarvis, coding time` invokes it, reports which steps were verified. If desktop offline, suggest retry without claiming it opened.

### Interaction C — prediction
At a configured, authorized calendar milestone J.A.R.V.I.S. gently offers `Your coding session starts soon; prepare your workspace?` → user says `Yes` → Mission Control opens selected apps on desktop → reports. If dismissed, stop. If calendar access expired, do not invent an event.

### Interaction D — video issue
`Hey Jarvis, what time is it?` in one utterance → recognizes and answers accurately with device time and no gateway → cyan → purple mic-reactive animation → blue speech-reactive core → cyan idle. Central battery percent is measured. Charger adds green particles *only* on verified charge state.

### Interaction E — failure
`Hey Jarvis, open Chrome on my laptop` when desktop offline → assistant responds `I understood, but your laptop companion is offline. Open its agent to reconnect.` → System Doctor records connection category → Mission Control shows `Not sent`, not green `Done` → no hallucinated execution.

### Interaction F — off-device mic
PC has no microphone or speakers. User holds Android phone, says `Hey Jarvis, open my notes on Windows.` Voice recognized locally on phone, secure desktop command runs on Windows, result is returned, assistant voice plays on phone. If Windows OS policies forbid a requested window interaction, use a supported fallback.

---

# CHAPTER 8 — PRODUCT DESIGN: PREMIUM FINISHING TOUCHES, NOT A REDESIGN

The current app aesthetic is approved. Do not move or enlarge the existing circular reactor, remove its displayed battery, change its original color foundation, or rebrand. Introduce the following **only within existing UI surfaces** or small accessible drawers/tabs:

- `System Doctor`: status, exact evidence, repair proposal, verification, log export with secrets removed;
- `Teach J.A.R.V.I.S.`: user correction, preview, save/undo/delete, trigger phrase conflicts, vocabulary;
- `Your Next Action`: one current optional suggestion with source and dismiss controls, no fake schedule;
- `Mission Control`: paired device selection, plan, live verified stages, permission and network indicators.

Use thin cyan outlines, readable navy-black contrast, refined micro-motion, honest system labels, accessible tap targets, haptics sparingly, responsive layouts and minimal CPU usage. Animations should convey *real current system states*. Avoid gratuitous bounce, strobe effects and infinite loading animations. Provide silent and reduced-motion options. Wallpapers are display-only; functional buttons must exist in the active app UI, not be painted onto an inaccessible background image.

Create screens only if needed to implement authentic functionality; never add mock command buttons or artificially generated telemetry.

---

# CHAPTER 9 — PRIVACY, AUTHORIZATION, HEALTH, AND RELIABILITY GUARDRAILS

- No covert microphone capture. Persistent listening is opt-in, uses supported foreground service and indicator, pauses/stops upon user request. A 30-second voice enrollment sample (if previously configured) is not unquestionable identity proof, and never bypasses lock authentication.
- No recording/private-screen ingestion without meaningful consent. Raw speech, screen content, personal Google data, email and contacts are **not** training data by default.
- No storing login passwords, OAuth refresh tokens in logs, or embedding provider API secrets in the APK. Optional Google access is scoped per service, consent-based, and revocable. Signing in with Google is not permission to access every Google product.
- No uncontrolled autonomous repair, self-updating executable code, arbitrary shell from remote commands, destructive file operations, automatic messaging, or external purchases. The user can stop operations and revoke devices.
- Proactive suggestions are optional, quiet-hours aware and never infer sensitive personal characteristics. Avoid medical or health interpretations beyond simple comfortable screen-break suggestions.
- No unconditional guarantees of “always listening” after force-stop or system restriction; no guarantee of 100% voice recognition; no promise that an offline model knows everything or answers instantly on all hardware.
- No false release claims: the app must pass tests; an unrun test is `NOT RUN`, not `PASS`.

---

# CHAPTER 10 — QUALITY ENGINEERING, TEST MATRIX, AND CRITERIA

**Critical Video 6158 tests to preserve and extend:**
1. At least 100 attempts of `Hey Jarvis` with measured wake rate, false positives and acknowledgment latency.
2. At least 30 wake-and-follow-up trials: one greeting, immediate command listening, no repeated wake phrase.
3. At least 20 combined `Hey Jarvis, [command]` trials: no lost suffix.
4. Recognized local requests: real time, date, identity, actual battery %, actual charging, installed app launching, offline basic reasoning and follow-up context.
5. Deliberately trigger permission denial, no mic, recognizer timeout, absent app, model not loaded, no internet, unauthorized Google connector and unreachable desktop; verify separate correct error categories.
6. Confirm core cyan idle, purple listening responsive to real mic volume, blue speaking responsive to actual audio playback, green charging particles only when actually charging, soft error state, and no animation when invisible except appropriate wallpaper renderer behavior.
7. Check battery 100% only from true telemetry, plug/unplug states, optional full-charge once-per-charge notice and quiet-hours handling.
8. Test visible app, Home, another app, screen off, lock screen, force-stop, power saver and competing audio session; document system limits honestly.
9. On low-end Android hardware record response latency (mean and p95), FPS, memory and battery/thermal impact; include clear test conditions.
10. Verify all original UI/feature behavior survives with automated regression and manual screenshots.

**Auto-Repair tests:** run each probe, inject one failure for each subsystem, verify accurate diagnosis, appropriate permission-aware repair, bounded retries, verification before success label, safe rollback, no hidden permission changes.

**Neural Evolution tests:** teach 20 custom aliases; test paraphrases, collisions, multilingual names, device targets and sensitive-action confirmation; correct and delete a preference; export/reimport; prove that a correction made under account A never appears in account B; test offline learning where supported.

**Predictive Intelligence tests:** authorized calendar, unavailable calendar, missing consent, quiet hours, repeat suggestions, dismiss/snooze, real battery threshold, charging-only events, local time zones and fully disabled suggestions. No false notifications or unsolicited app execution.

**Mission Control tests:** secure pair and revoke on Windows, macOS and Linux; open an app; open browser search; offline companion; unsafe target; replayed command; duplicate response; session expiration; permission denied; action postcondition absent; local file transfer with approval; prove no remote action without explicit device trust.

**Usability tests:** ensure all essential controls are accessible by screen reader and touch; no wallpaper-only nonfunctional hotspots. Test portrait 9:16 with reduced motion and on small displays. Count any performance regression introduced by UI polish.

Define exit gates:
- `P0`: one wake once, valid recognized command, genuine answer, no fake success, central battery telemetry accurate.
- `P1`: optional microphone foreground service and lock-screen experience behave within Android rules, with transparent error handling.
- `P2`: all four engines use shared action registry and account permissions; core state is real.
- `P3`: tests passing on real hardware/desktop targets, measured performance, no high-severity privacy or security defects.

Do not claim `complete` for an unsupported or unverified action.

---

# CHAPTER 11 — EXECUTION PHASES AND CONCRETE DELIVERABLES

**Stage A — Audit and video reproduction:** read repo and source video, make verified issue list, capture screenshots/measurements, freeze UI baseline, establish tests.

**Stage B — Repair voice, offline brain and actions:** microphone single owner, debouncing, follow-up, error mapping, local deterministic answers, actual action execution and postcondition tests, background permissions as supported.

**Stage C — Repair holographic display:** preserve wallpaper and central battery, synchronize real UI state with cyan/purple/blue/green/amber rules; record state-driven render tests and battery plug/unplug checks.

**Stage D — Auto-Repair Intelligence:** health probes, diagnosis engine, bounded safe repairs, detail panel, logs, repair verification and rollback.

**Stage E — Neural Evolution Engine:** opted-in correction memory, custom phrase teaching, command preview, conflict resolver, user profile data isolation and evaluation.

**Stage F — Predictive Intelligence:** consent-based inputs, relevance and quiet-hour rules, one-action suggestion strip, anti-spam controls and tested thresholds.

**Stage G — Universal Mission Control:** secure desktop companion pairing, typed actions and result verification, Android mic/speaker role, desktop readiness view, tested target OS adapters.

**Stage H — Finish and release validation:** integrated cross-engine tests, low-end performance, permission review, crash/ANR fixes, signed builds where possible, truthful release notes and rollback plan.

**Deliverables required from the coding assistant:** changed source files and diffs; new logical components and data migrations; integration instructions; actual build/test output; before/after screenshots/video; `Video6158_Timeline.md`; `Feature_Matrix.md`; crash and performance reports; desktop capability matrix; permission declarations; security review; truthful list of unsupported/untested behaviors; an APK/AAB or installer **only when actually built and signed**; concise user-facing setup and troubleshooting.

**Maintain a progress board:** `Task | Source issue | Files changed | Test | Verified on device? | Status | Limitation`. For any unsolved problem, give exact blocker, next safe action and non-disruptive fallback. Do not replace working functionality with mock features just to mark a milestone done.

---

# FINAL NON-NEGOTIABLE DEMONSTRATION

The user opens the existing J.A.R.V.I.S. HUD. Its battery center shows a real system reading; the cyan core subtly animates. They say `Hey Jarvis, what time is it?` only once. The core turns purple while speech is truly captured and recognized; J.A.R.V.I.S. answers with real local time without demanding an HTTPS gateway; the core turns blue *while actual speech plays*. When charging, green particles appear around the core while other state colors remain correct. They say `Why didn't you understand my last command?`; System Doctor returns an evidence-based diagnosis and approved correction path. They teach `coding time` as a saved workflow, opt in to an upcoming study reminder, then say `Hey Jarvis, coding time` to execute an authorized, verified action on a paired desktop that has no microphone or speakers, with Android still handling both voice input and voice output. If the desktop is disconnected, J.A.R.V.I.S. explains the real failure rather than saying it succeeded.

**Implement repairs and polish in the current codebase. No replacement project, no removed features, no standalone fifth “Living Core” module, no invented results, no fake machine intelligence, no forced cloud gateway. Begin with the baseline audit and Video 6158 issue reproduction, then deliver source-code changes and measured tests.**
