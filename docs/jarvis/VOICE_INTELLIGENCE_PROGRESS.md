# Voice intelligence and rendering recovery

## Recoverable baseline

- Repository `sepehrdadehsani89-hash/gdp-dashboard`, branch `jarvis-next`, clean starting HEAD `b2861b3739f8cc2e3bc9fc4423cef18a0270ec5a`.
- Local checkpoint `checkpoint/voice-intelligence-start-b2861b3` preserves the package-isolated Phoenix preview and all earlier work. Existing rollback branches/APKs were retained. No GitHub push has been performed for this directive.
- The new owner directive is preserved verbatim in `VOICE_INTELLIGENCE_DIRECTIVE.md`, SHA-256 `6e942ad5334a1bd650e1a08a01a511662b7218c39ebf41282517cf2e3b447666`.
- Baseline `:app:testDebugUnitTest :app:compileDebugKotlin` completed **BUILD SUCCESSFUL in 24s**, all 26 tasks up-to-date. This checked the existing source/cache state; it did not re-execute the prior 677 passing / one optional skipped tests. Log: `TEST_EVIDENCE/VOICE_INTELLIGENCE/baseline-build.txt`.
- Existing JDK21, Gradle8.9, AGP8.7.3, Kotlin2.0.21, SDK35/build-tools35, NDK26.3 and CMake3.22 were reused. Approximately 51GiB disk available, 8GiB memory limit. No dependency or emulator installation.
- No connected Android runtime or Redmi 9 evidence is available. Cached build success does not establish correct microphone, speaker separation, animation, wallpaper, charging, thermal or installation behavior on a phone.

## Existing implementation inventory

Foreground speech uses `voice/SpeechController.kt`: downloaded Vosk through `OfflineSpeechSession.kt`, otherwise supported Android `SpeechRecognizer` with explicit network preference. `WakeListenerService.kt` owns the existing opt-in foreground wake/command capture. `ManagedMicrophone.kt` and `MicrophoneProcessing.kt` handle capture leasing and supported Android processing effects. Endpoint policies are `UtteranceAssembler.kt`, `SpeechInactivityClock.kt` and `ConversationWindow.kt`.

`AssistantRuntime.kt` owns backend dispatch, session generations and output routing. Existing reply audio is configured Fish Audio with explicit privacy consent, while local TTS helpers remain separate. Captions flow through phantom state and `ui/ConfirmedCaption.kt`. Rendering uses `ui/JarvisHud.kt`, `ui/CoreFramePacer.kt`, `sentinel/JarvisWallpaperService.kt` and `sentinel/WallpaperRenderer.kt`. Real charging telemetry comes from the existing power repositories.

The current APK retains `com.jarvis.next.phoenix` debug identity to prevent the reported signing conflict. Production package remains `com.jarvis.next`; original release signing material remains unavailable. The existing UI and working implementations are the starting point, not a replacement template.

## Verification boundaries

Work proceeds microphone first, then conversation/caption, rendering/wallpaper/charging, settings/diagnostics and release verification. Candidate VAD or speaker processing must have an actual compatible integration before it is labeled available. Single-microphone heuristics cannot guarantee foreground speaker separation. No private raw audio/voiceprints are retained or uploaded by default. New feature test results and blocked device scenarios will be recorded as work completes.

## Implemented source changes

**Microphone endpointing.** The old assembler treated every above-threshold sound as voice. Each changing ASR hypothesis also renewed its stability timer, allowing noise or other voices to extend a turn to120s. Endpoint pause is now configurable650–1800ms (default900); incomplete English/Spanish clauses receive a longer pause. Stable or same-length/oscillating hypotheses have a2.5s safety trigger (4s for incomplete clauses) that requests an actual decoder final. Genuine word growth retains the longer utterance allowance. The absolute turn cap is60s, independent of continuing background energy/text. Continually growing nearby speech can still reach that cap: this is a safe bounded fallback, not proof of active-speaker isolation.

Foreground and wake PCM readers use nonblocking reads, a3s no-PCM limit and a short backoff instead of spinning. Foreground capture has an independent timeout; forced decoder/platform finalization has a2.5s response guard. Android supplied low/invalid confidence is rejected; omitted/-1 confidence is labeled unavailable and existing action/owner/confirmation policies remain. A result is never synthesized from a partial. Empty decoder boundaries preserve accepted words when there is no unfinished tail; an unrecognized new compound tail now requires a retry instead of silently dispatching the accepted prefix.

The bundled sherpa-onnx library exposes VAD APIs, but the app has no validated Silero model or Redmi9 acoustic/performance benchmark. No new ML model/dependency was installed. Current endpoint diagnostics explicitly identify ASR/RMS/text heuristics; they do not present amplitude as a neural speech or speaker probability. Existing optional local speaker-model and consent flows are preserved. No perfect speech/TV/traffic separation is claimed.

**Conversation and captions.** Existing generation/serial gates and single-provider routing are preserved. An old cancelled streaming continuation can no longer stop a newer playback session. Live captions now start only when the configured Fish Audio segment actually starts playback, using that segment's text. User recognition, unplayed generated text and chat-history fallbacks no longer appear as live assistant captions. Completion uses a configurable0–5000ms linger (default1200); errors, cancellation and privacy changes clear immediately. Speech IDs ignore obsolete callbacks. Transcript preview is opt-in in the main app; wallpaper/overlay remain assistant-only. Conversation history remains available separately. Segment onset is supported; reliable word timing is unavailable, so no fabricated word synchronization was added.

**Rendering and charging.** The prior cadence repair remains. Wallpaper recovery now considers successful Canvas posting, covering null Canvas, transient lock errors and failed post even under a static motion policy. Recovery is bounded and visibility-gated. Both renderers use a small reusable field of green particles, traveling from the existing Canvas bottom toward the core, with trails, glow and absorption. Maximum18 particles, at most6 under constrained conditions. Real connected/charging/full readings drive effects; hidden, unplugged, full, disabled, zero-density and reduced-motion states stop incoming transport. Original core geometry, palette, charging indicators and real battery/Android estimate handling remain. Main-HUD transport is inside the existing core Canvas; wallpaper transport uses the surface. No Redmi9 FPS, thermal or battery measurement is claimed.

**Settings and diagnostics.** The existing Settings screen now links directly to Voice Intelligence. Working controls cover local microphone sensitivity/confidence, endpoint pause, supported Android processing requests, output speed, supported stop controls, caption linger and optional transcript preview. Existing English/Spanish choice, response-language preference, speaker consent/enrollment, output-voice configuration and pickup controls remain. Engine availability distinguishes installed Vosk, Android on-device support, system service presence and network-speech permission.

The controlled diagnostic runs the real configured recognizer under the existing exclusive capture lease. It displays actual input activity, endpoint stage, monotonic capture/finalization timings and sanitized errors. It does not submit recognized speech to AI/tools, persist a recording or enable listening automatically. Navigation, lost ownership, permission loss, lock or pending authentication stop the test. Only opted-in transcript preview is held temporarily in memory.

## Test and release preparation

`TEST_EVIDENCE/VOICE_INTELLIGENCE/DEVICE_ACCEPTANCE.csv` defines20 distinct acceptance scenarios covering the owner's traffic/nearby/music/long-question/caption/core/wallpaper/charging/stability reports, English/Spanish, privacy and Redmi9 performance. All20 remain BLOCKED with zero hardware evidence. Four new charging bitmap instrumentation cases are authored; compilation and execution statuses will be recorded separately.

The first integrated build was deliberately stopped during compilation after review found the unfinished-compound and diagnostic transition edge cases. Its log is retained as `review-stopped-build.log` in the working evidence directory. No APK or passing-test claim was made for that stopped run. The next run found one legacy TitanVoiceTest assertion expecting the former2.5s incomplete-clause pause. That test now explicitly selects900ms and checks1799ms/1800ms after the last energy update, matching the new documented policy. The failed XML/log/counts are retained in `first-test-failure/`; the final suite result below follows that correction.

## Completed software verification

- Final build: **BUILD SUCCESSFUL in10m48s**,68 tasks (20 executed/48 up-to-date). Application compilation had already succeeded in the preceding run and was correctly reused; the changed test compiled and the JVM suite ran again. Lint, Android-test Kotlin compilation and APK assembly completed; unchanged Android-test Java compilation reused its valid cache.
- **789 JVM cases:788 PASS,0 FAIL,1 SKIPPED**, the optional private-export fixture absent. Per-case CSV and original JUnit XML are saved. There are111 additional cases compared with the678-case package-fix baseline; do not add overlapping suite counts as separate executions.
- Focused suites: EndpointReliability44, AndroidFinalRecognition20, SpokenCaption20, ChargingParticleField20, WallpaperSurfaceRetry20 (includes14 prior cases), CoreFramePacer24 (retained prior cases). All passed. These exercise deterministic timing/state/geometry/JSON policies, not acoustic accuracy or physical frame delivery.
- Lint: **0 errors/fatals,48 warnings,1 information**. Existing SDK metadata/deprecation findings remain recorded.
- **20 Android rendering fixtures compiled,0 executed**, comprising the earlier10 core/6 wallpaper fixtures and4 new charging bitmap cases. `NEW_ANDROID_RENDER_CASES.csv` records the new cases as BLOCKED. All20 hardware acceptance scenarios remain BLOCKED; zero phone/emulator tests were run.
- **20 artifact checks PASS**, signature v2/ZIP integrity/alignment/package/versions/labels/providers/permissions/task/callback/ABI/asset retention verified. All14 packaged native libraries are AArch64. All134 native-library/asset entries from Phoenix.1 were compared byte for byte and preserved.

Source commits: `39c4049` microphone/endpoint confidence, `f6ab2cd` captions/render/charging, `1b1a344b5fc7cab6077ac5df3dc9857f29e0c66b` controls/diagnostics/version. The following artifact/evidence commit contains the saved release folder. Local rollback `checkpoint/voice-intelligence-start-b2861b3` and all earlier backups remain intact. Source assets, dependency versions, original signing configuration and historical APKs were retained; no Android app data was modified.

## Verified preview artifact and remaining limits

`downloads/voice-intelligence-2.11.9-debug/Jarvis-2.11.9-voice.1-debug-arm64.apk`: **2.11.9-voice.1-debug**, code28, package **com.jarvis.next.phoenix**, label **Jarvis Phoenix Test**, ARM64/minAPI29/target35. **55,399,061 bytes**. SHA-256 **24b08f9519231127108e10f133326eb2646ce3f6fa5757c45294441338df459c**. The copied download artifact matches the assembled checksum.

Verified debug certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`, matching the earlier Phoenix preview. The retained original2.11.8 certificate differs. Original production signing directories are not configured here; this is a signed side-by-side evaluation APK with its own settings/permissions/configuration. No production-signed update or migration of the original installation is claimed.

The requirement matrix `VOICE_INTELLIGENCE_REQUIREMENTS.csv` explicitly records partial/missing/device-blocked work. Actual Redmi9 endpoint accuracy, nearby-speaker handling, full microphone lifecycle, audible captions, main/wallpaper freezes, charger transitions and performance remain unverified. No neural VAD model was installed or benchmarked. General simultaneous-speech barge-in is unsupported. Backend/output availability still depends on the owner's real service/model/account configuration. Other historical master-spec features were preserved, not newly declared complete.

Evidence directory: `TEST_EVIDENCE/VOICE_INTELLIGENCE/` contains build/failure logs, unit CSV/XML, lint, final manifests/metadata, artifact-inspection script/results, device acceptance cases and exact build receipt. Tracked console copies strip trailing whitespace only; raw originals remain under `/workspace/work/jarvis-voice-intelligence/`.

GitHub `jarvis-next` was checked read-only at `1c2e1bb05301cebaa2598c892eb52acf86164494` during final preparation. No new push/publication has occurred. Under the owner's Phoenix publication rule, confirmation of the concrete final push is required; a live immutable URL must then be independently downloaded and SHA-256 checked before delivery.
