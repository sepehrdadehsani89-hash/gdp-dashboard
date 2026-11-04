# JARVIS 11.2 · Project Rebirth

**Release status: BLOCKED / HELD.** This is a development validation candidate based on Phantom11.1 commit `4100cbe2479fe482693ded1055e88bde938003d4`. It is not a claim that the entire master directive is complete or that the user's reported handset defects have been reproduced and fixed. No public11.2 APK/download is published. The existing11.1 artifact remains unchanged.

Package: `com.jarvis.next`; candidate code15, name `11.2.0-candidate1-reinstall`; arm64, Android10/API29 minimum, target35. The build type name is historical: this task does not instruct the user to uninstall anything. The retained signing line can update only an installation with its matching certificate and a lower version code. It cannot update original Jarvis7.0 signed with the unavailable original key. No key was generated and no installed data was removed.

## Implementation status

“COMPLETE” below means the specified source change is implemented and subject to the host verification in VERIFICATION.md. It does not imply phone, provider or commercial acceptance. “PARTIAL” means a useful implementation exists but the wider requirement has known gaps. “BLOCKED” names an external prerequisite. “NOT TESTED” is an execution status, never a passing result.

| Area | Status | Implemented behavior and limits |
| --- | --- | --- |
| One microphone owner | PARTIAL | Global synchronized lease for wake, offline foreground STT, Android SpeechRecognizer and visible private PCM capture. Constructor failure, recording callbacks and final cleanup release the same owner. Busy recovery is bounded. Actual OEM/audio-route behavior is NOT TESTED. |
| Recognition callbacks | PARTIAL | Session/generation guards, actual Android support-based language selection, Vosk/Android alternatives and stale wake broadcast rejection. Android can still provide no support list or no confidence. Recognition accuracy is NOT TESTED. |
| Recovery / voice activity | PARTIAL | Two recoverable wake retries inside an already authorized foreground service, bounded foreground busy retry, adaptive inactivity and utterance cap. Permission/native/memory failure requires user action. No claim of universally reliable Bluetooth or TV isolation. |
| Semantic correction | PARTIAL | Explicit opening intent; approved alias first, exact installed name, known spoken variant, supported alternatives and high-margin phonetic match. Cloud→Claude only for an app intent with an actual Claude target. Conflicts ask. Numbers/recipients are preserved. General semantic/contact/destination learning is not a new universal engine. |
| App launch architecture | PARTIAL | Shared coordinator for direct app requests/approved open_app actions. Package visibility fallback for saved targets, exact alias priority, foreground/visible-overlay eligibility, private notification otherwise. Reports dispatch only. Android may restrict launches. Some older deep-link/media paths remain separate. All25 saved aliases require phone verification. |
| Live-only ordinary chat | COMPLETE (source) | No new ordinary chat Room inserts/reloads; bounded RAM24 messages/24k text, per-message and source limits, Stop/lock/idle expiry. Legacy rows preserved and hidden until separate explicit deletion; settings/memories/aliases unchanged. No Room schema reset. Process/deletion UI acceptance is NOT TESTED. |
| Memory / crash hardening | PARTIAL | Reference-counted Vosk model cache, bounded speech queue, managed capture cleanup, stale-audio cleanup, ephemeral reduced visual quality on actual memory pressure and content-free exit diagnostics. Physical crash causes, heap/ANR profiles and long-run stability are NOT TESTED. |
| Fish Audio | PARTIAL / NOT TESTED | Existing strict Fish-only EN/ES policy retained. Editable English ID `05b36da8574341d0803391491850db20`; exclusive Spanish ID `ae74d1059517440aa6e5d2d50598d179`. Native responses segmented without silently dropping their tail. Generation/config checks before and after synthesis. Follow-up resumes after a Fish error where permitted. No Android/OpenAI voice substitution. Real authorization, quota, sound and timing NOT TESTED. |
| Social/native commands | PARTIAL | Native courtesy replies, neutral title unless fresh owner confidence, exact let's-cook/self and Android Home-screen routes. No LLM round trip for these source routes. Real request timing/opening NOT TESTED. |
| Locked actions | PARTIAL | Android DevicePolicyManager lock path preserved, no power-off/wipe. Unlock continuation uses one-use120s ticket bound to generation and real keyguard dismissal; otherwise private notification. No voice authentication bypass. Physical lock/unlock NOT TESTED. |
| Wallpaper / Phantom | PARTIAL | Shared response state, scoped heard/understood view, actual task headings, captions, cyan mini state, actual telemetry colors, persisted visual controls, explicit-close suppression and reduced motion. Existing artwork retained. Touch/insets/rotation/battery behavior NOT TESTED. No invented Superdesign download or new cinematic renderer is claimed. |
| Google Account Center | PARTIAL / BLOCKED | Official Google authorization SDK, account picker, read-only Gmail scope, actual profile verification before saving, encrypted vault tokens, credential-gated metadata/2min mailbox session, disconnect/revoke outcome. Google project/API/Android OAuth not registered by owner; no live account grant verified. |
| Gmail | PARTIAL / BLOCKED | Official inbox/latest/unread/sender metadata, bounded inline-text reading, session-bound follow-ups, optional local-model summary and explicit excerpt without it. No automatic attachment download or mail sending. OAuth configuration and real mailbox acceptance remain blocked. |
| Personal Content Center | PARTIAL | Android SAF and Photo Picker, read-only tree, bounded name browsing/search, local text/PDF/OCR extraction, official viewer/manual sharing, selected-content local summary/translation with optional explicit next-question cloud consent. No arbitrary full-device access, background upload, video understanding or photo scene recognition. Picker/grant/large-file acceptance NOT TESTED. |
| World clocks | COMPLETE (source) | Actual Java/Android IANA zone rules for six named cities or valid explicit zone IDs, unknown city rejected; no guessed time. OS time/zone data must be correct. Runtime visual acceptance NOT TESTED. |
| Other accounts / commercial readiness | UNSUPPORTED / BLOCKED | This Gmail grant does not connect Drive, Calendar or Microsoft. No Google public-app restricted-scope verification, payments, backend or commercial clearance is supplied by this source change. |

## Source findings versus physical root causes

The investigation found cleanup occurring after recognition-result callbacks, independent recorder ownership, callback generations missing on intermediate Android events, resource-allocation exceptions escaping old cleanup boundaries, repeated Vosk Model loads, persistent ordinary chat and unbounded queued speech. Those are concrete source risks repaired here. They do not prove the physical cause of missed words, long delays, background TV recognition, crashes or failed openings on the Redmi9. No new handset logcat, tombstone, heap dump or ANR trace was available.

The earlier attempt at generic repeated-word removal failed a legitimate “very very” regression and was reverted. Duplicate-final/session protection remains; arbitrary repeated words are preserved. Lint caught a StateFlow read in composition; the diagnostics screen now collects that state through lifecycle-aware Compose observation. Both changes are included in final validation.

## Changed files and services

* `voice/ManagedMicrophone.kt`, `OfflineSpeechSession.kt`, `SpeechController.kt`, `WakeListenerService.kt`, `RecognitionModelCache.kt`, `omnisystem/PrivateAudioCapture.kt`: lease ownership, cleanup, support/alternatives and bounded recovery. The existing user-enabled microphone foreground service is modified; no boot-starting listening service is added.
* `rebirth/SessionPrimitives.kt`, `SemanticAutocorrector.kt`, `RecognitionPayload.kt`, `SpeechSegments.kt`, `WorldClocks.kt`: bounded/pure policies exercised by host tests.
* `rebirth/AppLaunchCoordinator.kt`, `neural/AppRegistry.kt`, `AppMatching.kt`, `tools/DeviceTools.kt`, `titan/TitanLaunch.kt`: actual packages, approved alias priority and Android outcomes.
* `data/JarvisStore.kt`, `neural/NeuralPreferences.kt`, `NeuralSettingsActivity.kt`: live RAM chat and separate legacy-history deletion. Explicit user-saved permanent memory features remain.
* `rebirth/GoogleAccounts.kt`, `GmailRepository.kt`, `AccountCenterActivity.kt`: official OAuth/read-only mail, encrypted storage, authenticated metadata and short-lived mailbox access.
* `rebirth/PersonalContentActivity.kt`, `core/ConnectedFolders.kt`: explicit URI selections and bounded authorized-tree browsing.
* `rebirth/CrashDiagnostics.kt`, `RebirthCenterActivity.kt`, `NativeNavigation.kt`, `AndroidSettingsActivity.kt`: real diagnostics/private settings and guarded Android settings handoff.
* `AssistantRuntime.kt`, `JarvisApplication.kt`, `MainActivity.kt`, `ui/JarvisUiState.kt`, `JarvisAppUi.kt`, `phantom/*`, `omnisystem/ClockHudActivity.kt`, `security/ScreenSecurityController.kt`, `ScreenUnlockActivity.kt`, `voice/VoiceSettingsActivity.kt`: wiring, privacy/session expiry, source-of-truth surfaces and actual native transitions.
* `app/build.gradle.kts`: candidate version and official Google auth SDK21.3.0. Manifest: four internal, nonexported activities. Tests: `rebirth/RebirthPolicyTest.kt`, `GmailPolicyTest.kt`. No new backend or public API key is added.

Exact final production/test/configuration hashes and artifact properties accompany the local candidate outside the repository. Existing public11.1 artifacts are not overwritten.

## Permissions and Android limits

No new app-authored dangerous Android permission was requested. The new account picker uses Android/Google UI without GET_ACCOUNTS; SAF/Photo Picker use explicit URI grants without broad storage/media enumeration. Existing microphone, notification, overlay, camera, contacts/location and optional accessibility/device-admin requirements remain user-controlled. WorkManager's existing merged boot permission is not a new automatic microphone startup. See final APK permission comparison in verification evidence.

Supported native artifact is arm64/API29+, not every Android/ABI. The inherited native Vosk library has4KiB ELF alignment; no universal16KiB-page or Play distribution compatibility is claimed. OEM background restrictions, Android14/15 foreground launch rules, Google Play services and actual installed recognizer/language support still apply. Speaker similarity is an optional signal, never proof of identity or authorization. One phone microphone does not provide reliable source separation, TV rejection or far-field speaker isolation.

## Benchmarks and device execution

All requested wake/partial/final/correction/intent/dispatch/tool/model/first-token/Fish/audio/interruption/memory/crash/ANR measurements are **NOT TESTED** on a development handset and **NOT TESTED** on Redmi9. No numeric improvement is claimed. Build time and host-test count are not phone-performance measurements. Existing trace events provide future instrumentation points, not observations by themselves.

The preserved API29 emulator previously stalled in Android system_server/package operations. It was not reset or wiped to manufacture acceptance. Compiling Android test code is not executing instrumentation. No real Google/Fish/Redmi/account/Photo Picker success is asserted.

See [VERIFICATION.md](VERIFICATION.md) for final host results, [ACCEPTANCE-MATRIX.md](ACCEPTANCE-MATRIX.md) for required unexecuted device cases, [RELEASE-GATES.md](RELEASE-GATES.md) for blockers and [GOOGLE-SETUP.md](GOOGLE-SETUP.md) for button-by-button registration.
