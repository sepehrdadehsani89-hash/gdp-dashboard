# Production stabilization — working record

Baseline: `8d7ac1ec35c72bb1628f70a9f7d99ccf7aed5f74`, branch `jarvis-next`.
Rollback: `checkpoint/production-stabilization-start-8d7ac1e`. The previous published APK remains intact. No dependencies, models or signing keys were installed or replaced in this milestone. The supplied production directive is preserved verbatim in `PRODUCTION_STABILIZATION_DIRECTIVE.md`.

## Audit and root causes

1. `VoicePreferences.playbackInterruption` defaulted to true. On actual audio start the runtime enabled playback controls and WakeListenerService reopened recognition before its normal speech suppression check. Echo filtering could not establish exclusive ownership. Legacy playback interruption is now disabled, including previously persisted true values. Generation-fenced speech reservation, actual start/completion callbacks and cooldown own recognition eligibility.
2. Native battery replies were classified private. The public speech acknowledgement substituted “the result is on your screen”; time and greeting replies used a different route. Battery replies now speak the actual Android reading. Streaming deltas previously launched overlapping speech and could mark speech handled without successful audio. Deltas remain visual; the validated final response enters a single speech route.
3. The HUD state collector copied `reducedMotion=true` and particle density zero whenever memory performance restrictions were active. A settings save could then persist this derived restriction. The collector now preserves the saved preference; renderer frame budgets enforce memory, Battery Saver and system animation restrictions separately.
4. Prior checkpoint already contains frame scheduling, surface retry and charging particle fixes. Their software policy regressions are rerun. Real 60-second rendering, lock/unlock and charger tests are pending; changing a preference is not physical rendering evidence.

## Implemented behavior with software verification; device acceptance pending

- One authoritative voice session reserves the microphone during synthesis, playback and cooldown. Stale callbacks cannot interrupt a new response. Default continuous follow-up is off. The existing foreground wake listener resumes after actual completion and cooldown, without opening a command window by default.
- Stop Speaking retires the generation, stops playback, clears captions and resumes safe wake mode after cooldown. Speech retry retains the exact failed response transiently and preserves inference evidence. Logs contain metadata, not response text or credentials.
- AI response, TTS preparation, audio start and completion/failure are separate diagnostics. Muted media/voice volume is an output failure; an audio API callback is not proof of physical speaker audibility.
- Safe native contact summaries explain the real confirmation step. Contact names, memories and private content still require explicit private-speech consent before Fish Audio receives them. No replacement voice was installed.
- Typed, allowlisted voice edits write the existing settings stores. Protected privacy/provider edits require confirmation and use the existing Android identity policy; verified provider/model proofs are still mandatory. Credentials must be entered through private UI, never voice/history/model routing. Unsupported Android settings are not arbitrary preference writes.
- Explicit contact nickname commands use local encrypted shortcuts. Exact unique Android matches require Contacts permission. Ambiguity, unsafe numbers, changed contacts or expired confirmations fail closed. `M-A`, `M A` and `Ma` resolve consistently. Android's final Call action is retained; no inferred relationship or automatic call.
- First visible use offers the OS microphone permission and optional downloaded speech pack. Existing opt-in is reused; Stop listening is respected. No capture starts immediately after installation, before consent, from boot, while locked or while Jarvis owns speech. Android microphone controls and OEM process restrictions remain authoritative.
- Searchable ten-section settings navigation opens existing controls; detailed preferences and the existing main holographic interface are retained. Thirteen-step major-update setup can be skipped/reopened and records its completion revision. Ready requires a selected-model test, playback completion, microphone permission and the user's audibility confirmation.

## Existing platform actions preserved

| Action | Mechanism and evidence | Device result |
| --- | --- | --- |
| Battery | Android battery reading; spoken actual percentage | Pending Redmi 9 |
| Flashlight | CameraManager, permission and TorchCallback | Pending hardware |
| Media volume | AudioManager readback | Pending hardware/audio |
| Brightness | System write-settings access or Android panel | Pending device |
| Wi-Fi / Bluetooth / mobile data | Supported Android request or settings panel; no privileged toggle claim | Pending device |
| DND | User-granted policy access/system panel | Pending device |
| Timers / alarms | Existing owned timers/Android alarm intents and confirmation | Pending device |
| App launching | Installed-package resolution and actual dispatch/status | Pending device |
| Contacts | Local exact match, approval, requery, ACTION_DIAL | Pending permission/contact fixture |
| Secure unlocking | Android authentication only | No bypass |
| Vision / location | Existing camera consent/model checks and measured location | No live camera/GPS test |

## Verification

Actual logs, XML, per-case results and artifact checks are in `TEST_EVIDENCE/PRODUCTION_STABILIZATION`. The initial focused policy runs passed 110 and 174 tests respectively, with zero failures or skips. The settings/contact/setup focused run then passed 207 tests with zero failures/skips after a recorded compile-error correction. The final JVM task completed 1,297 cases: 1,296 passed, zero failures/errors and one optional user-export fixture skipped (`PrivateMemoryBackupTest.validatesUserExportPrivatelyWhenProvided`). Final full verification run 3 passed compilation, lint (zero errors, 55 warnings, one informational item), APK assembly and Android-test compilation. Twenty artifact checks passed. The released candidate is 56,199,459 bytes with SHA-256 `ad796f1920f4853ff855acd710d1360bfa32a1e185ca733d4850c071ab7261ed`. Physical/instrumented tests executed: zero. No Android device or emulator is connected. No live provider account was used in this cloud environment. An installed Local Brain on the user's phone does not establish local inference in this workspace.

## Release boundary

Candidate: version code 30, `2.11.9-stabilize.1-debug`, display name Jarvis. The existing isolated preview application ID `com.jarvis.next.phoenix` and retained preview signing certificate are preserved. The production ID remains `com.jarvis.next`; its signer is not available here, and the user reports the original keystore is unavailable or uncertain. Updating the previous Phoenix preview preserves its data; the original differently signed production application remains a separate install.

The user confirmed Jarvis has not been published. This does not authorize replacing an existing signing identity. A production AAB is blocked by the original signing configuration, target API migration and native/Play policy work described in `GOOGLE_PLAY_READINESS.md`. This is a tested preview candidate, not a store-approved production release or a guarantee of zero bugs.
