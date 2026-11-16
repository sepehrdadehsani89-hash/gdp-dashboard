# Requirements → code → evidence

Active user directive: EXECUTION_DIRECTIVE_2026-11-16.md (SHA256 4a4400f1840489ce46ae0fb47a792ca95bf7db45c074c1d82284ab2753ca2f96), especially §§5, 10, 14–16, 19, 21–24. It supersedes conflicting old priority/verification directions; originals remain immutable. Previously indexed archive SHA256 1b1d964bf6e59ae631a25659a0151ffd827bf59d145fc18625aff0d248700f93. Part 24 SHA256 a422c7f5915a2e897d937fac52926c6e4eb76198918df57b682e30a7e5548e53.

| Stable requirement | Exact source/provenance | Code | Evidence / status |
|---|---|---|---|
| VOICE-OUTPUT-COMPLETION | Active §§5,13,15,16; original P24-B00019467 (audio interruption/cancellation); P19-B00010926 (natural voice/routing recovery) | voice/OfflineVoiceManager.kt; voice/OfflinePlaybackDrain.kt | 20 focused JVM cases passed; hardware integration pending; PARTIALLY_IMPLEMENTED |
| VOICE-EXCLUSIVE-MIC | Active §5; P24-B00019467 | voice/ManagedMicrophone.kt; rebirth/SessionPrimitives.kt | Existing source and selected baseline tests; actual microphone pending; PARTIALLY_IMPLEMENTED |
| VOICE-ASR | Active §5; P24-B00017337 | voice/SpeechController.kt; voice/OfflineSpeechSession.kt | Platform/Vosk paths exist; provider/model/device results unverified; PARTIALLY_IMPLEMENTED |
| VOICE-VAD | Active §5; P24-B00017337 | Current endpoint logic: voice/UtteranceAssembler.kt and SpeechInactivityClock.kt | Energy/text endpointing exists; Silero VAD not integrated; MISSING |
| VOICE-WAKE | Active §5; original Part 24 §6 | voice/WakeListenerService.kt; WakePhraseMatcher.kt | Text/state tests passed in baseline; acoustic/background tests blocked; PARTIALLY_IMPLEMENTED |
| CORE-FREEZE | Active §10; P24-B00012943 | ui/JarvisHud.kt; sentinel/JarvisWallpaperService.kt | No runtime freeze reproduction; 20 original acceptance cases BLOCKED; BLOCKED_BY_ENVIRONMENT |
| DESKTOP-TRUST | Active §8; original Part 24 §8 preserves desktop pairing | companion/jarvis_companion.py; Android companion/ | Existing 36 Python checks passed including local TLS/Chromium; Android/native OS pending; PARTIALLY_IMPLEMENTED |
| DESKTOP-EXTENDED-ACTIONS | Active §8 | Existing companion has only Chrome and search actions | General files/apps/media/shell workflows remain MISSING; never claim unrestricted control |
| IPAD-SYSTEM-CONTROL | Active §9; original Part 24 §8.4 | No system-control implementation | Remote unlock/global Wi-Fi/brightness/arbitrary background control: UNSUPPORTED_BY_PLATFORM |
| IPAD-FOREGROUND-WEB | Active §9 deprioritizes; original Part 24 §8 | No deployed Safari companion | MISSING |
| RELEASE-UPDATE-IDENTITY | Active §16; original Part 24 §16 | app/build.gradle.kts; downloads/polish-2.11.8/build-info.json | Existing signed APK verified previously; new signed update REQUIRES_USER_AUTHORIZATION and retained signing material |

The original 21,331 passage ledger is provenance, not 21,331 implemented features. The full semantic reconciliation of all archived requirements remains incomplete. Further rows must cite source, implementation, actual tests and platform boundaries.
