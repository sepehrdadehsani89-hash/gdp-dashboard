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
