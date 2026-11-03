# Sentinel Ascension 10.4 beta evidence

Baseline was actual branch jarvis-next code-11 10.0 beta, commit 5ffbb8f. An intervening 10.3 source release was not present. This upgrade preserves package com.jarvis.next and uses the previously retained reinstall certificate; it does not recover the original Jarvis 7.0 private key. No new signing key, phone uninstall, phone data clear or destructive database migration was performed.

## Signed build

Final ARM64 signed pipeline `sentinel104-final-signed.log` completed successfully in 6m 26s: testReinstallUnitTest, assembleReinstall, lintReinstall. **280 JVM tests passed; zero failures/errors/skips. Lint: zero errors, 34 warnings**, not suppressed to obtain a pass. Build identity: code 12, 10.4.0-beta1-reinstall, min SDK29/target35, non-debuggable. Toolchain remains pinned Gradle8.9, Temurin21.0.13+9, SDK/buildtools35, NDK26.3.11579264/CMake3.22.1.

The downloadable APK is 83,708,901 bytes with SHA-256 `7660ce08e6662d4abdd133bfe9b976d86d3d8115a7c63d2d4f5d0ff74d9c6cd7`. apksigner verifies v2 and certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. ZIP 4-byte alignment passes. Only ARM64 native libraries are packaged. Content checks exclude signing stores, encrypted backups/unlock files, personal export, optional ONNX/GGUF weights and Android-test fixture classes. No claim of universal 16KiB native-page compatibility: the inherited Vosk library retains its known 4KiB ELF limitation.

New JVM cases cover cancellation/stale/replayed/expired/rollback credential state, compatibility confirmations vs real authentications, exact current-user lock/unlock intents, foreground/notification/manual presentation policy, ten-second inactivity with bounded active speech, encrypted-shortcut policy/provider number normalization, exact Unicode entity highlighting, twenty-minute multilingual parsing, explicit interruption and output-voice privacy/MessagePack constraints. Unit tests of callbacks are policy checks, not real Android authentication. Existing transport, cancellation, model-install, memory/parser and tool regression cases also pass. Output voice requires its own genuine provider preview; an ID alone is insufficient.

## Android execution evidence

No new Android instrumentation test ran or passed in this attempt. Earlier debug/Android-test compilation passed before the output-voice addition; compilation alone is not execution. The existing task-owned API29 x86_64 test AVD was preserved, stale locks from dead processes moved aside, and restarted without resetting userdata. It booted in 415808ms, but package-list commands repeatedly exceeded their 10–15 second bounds, logcat retrieval exceeded 20 seconds, and process inspection showed system_server in uninterruptible state after service restarts. The task-owned emulator was stopped without wiping its AVD. No synthetic device credential or device administrator was configured. No physical phone is attached.

The final debug application and Android-test APKs were compiled for x86_64 (`sentinel104-frozen-debug.log`, success in 2m39s); this is compilation evidence only. Compiled cases include no-lock refusal/settings, three actual OS-owned challenges, incorrect credential and stage cancellations, Stop invalidating late success, compatibility-mode labeling, linked Android-contact number change/encrypted storage, durable nonsecret credential privacy pause, and requested output ID edit/preview gate. Android-test source contains a public synthetic fixture for isolated-emulator use only; it is not a user credential and is absent from production APK classes. These cases remain unexecuted here.

## Required security acceptance boundaries

| Test | Automated scope and remaining boundary |
|---|---|
| A · No screen lock | Pure policy passes; Android no-credential flow compiled, runtime execution blocked. |
| B · Correct system credential | Pure matching callback policy passes; real three-challenge Android flow uses only a public synthetic credential on isolated emulator when run. |
| C · Incorrect credential | Policy failure case passes; runtime execution blocked. |
| D · Stage two cancelled | Pure fail-closed state case passes; runtime execution blocked. |
| E · Stage three cancelled | Pure fail-closed state case passes; runtime execution blocked. |
| F · All stages complete | Three unique live callbacks required by tested policy; runtime execution blocked. |
| G · Already unlocked | Presentation policy passes; actual controller result is included in the credential flow when run. |
| H · Securely locked unlock | Public-only nonce/expiry/UI implemented. Actual OEM voice-to-Keyguard-dismiss path remains unverified. |
| I · Authentication failure | Fail-closed policy/expiry/recreation tested; actual OEM lockscreen callback failures remain unverified. |
| J · Background restriction | Foreground-vs-notification/manual decision unit tested. Physical Android/OEM background and notification-tap behavior remains unverified. |

## Unfinished / blocked / device dependent

Requested output ID is editable and actually wired into optional Fish Audio speech requests. No provider was identified by the user and no matching account key is available here; the ID's provider, permission/access, charged service call and audible voice remain unverified. Existing local/OpenAI settings are preserved. Do not claim this voice has been heard or approved. Private replies retain local speech. See VOICE_SETUP.md.

No physical Redmi9, sustained local-model/thermal/battery measurement, human microphone recognition in English/Spanish/Persian, accessory battery/discovery/pairing, real music/Spotify account, camera provider, map/geocoder field test, exact-alarm delivery or full picker migration was established by this build. Existing real metrics are retained; no quantified speed gain is invented. Acoustic “wait wait” barge-in during active TTS is unfinished; explicit recognized/text interruption exists. Real maps are not a weather globe. Superdesign exports/reference remain inaccessible; the new native security reactor is original work, not an imported design. Full inherited technical-string localization and complete private-feature backup remain unfinished. See the 65-item REGRESSION.md inventory.

This is a sideload beta with clearly bounded evidence, not full master-directive completion or a commercial-readiness certification. Android's signature check still blocks updating original Jarvis7.0 with this retained newer signing line. Preserve that installed app/data and the private signing backup files.
