# Jarvis 11.3 verification

The frozen-source build completed successfully in 8m 39s: `:app:testReinstallUnitTest :app:lintReinstall :app:assembleReinstall :app:compileDebugAndroidTestKotlin`.

## Automated checks

All **376 JVM tests passed**, zero failures/errors/skips. Seventeen new cases cover stalled partials with room noise, natural silence, continuous changing speech, incomplete clauses, accepted finals with constant RMS, compound requests, noise-only input, rejected confidence, corrections between partial/final, one-time serial-bound command acceptance, screen-off aliases, public/private Fish routing, disabled voice, pending-action feedback and reactor continuity/resume. The prior 359 tests remain. Lint has zero errors/fatals and 43 warnings. No lint baseline or suppression was added to force this release through.

The signed APK is non-debuggable ARM64, min SDK 29 / target 35, package `com.jarvis.next`, code 19, name `11.3.0-reinstall`. V2 signature and four-byte ZIP alignment passed. Certificate SHA-256: `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. APK SHA-256: `b88dea0ec09abab410ca87fdf47b45e4e211e2e6c527802fdc00a6c23bfc4878`; size 84,397,409 bytes. Added merged permissions against 11.2 beta3: []. Private signing/password archives, private memory exports, model weights, test classes and removed Gmail/auth integration are excluded. No new key was generated.

All 1548 production/test/build input hashes match the final build: [manifest](source-inputs.sha256), SHA-256 `8ea9bde43489b65367eb1a815af1945f0c4b3312aa8384057f48dd69388a8e64`. Source base is `f71fefdad332cabb0d766e8cfd1be18303ffbcec`. The release commit contains the frozen source and APK. The 11.2 beta3 APK remains byte-identical. Publication is followed by a complete immutable public URL GET with independent size/SHA-256 verification.

## Behavior boundaries

Only actual, confidence-accepted decoder finals enter the command route. Partial text remains a caption. Existing optional speaker focus, microphone ownership, session invalidation and stop/privacy controls remain. The stale-text bound prevents constant RMS from holding a request forever; changing recognized speech can still continue up to the existing two-minute bound. Incomplete clauses keep their longer pause.

The Android speech fallback requests a final through `stopListening` and releases the service if no result arrives within 2.5 seconds. A failed service cannot silently turn a partial into a device action. Rejected or unclear heard speech receives fixed public feedback once, then returns to wake-only mode. An ordinary no-speech follow-up still expires without starting an endless retry conversation.

Private/native results that cannot use cloud speech receive a fixed acknowledgement only when the configured general Fish voice is eligible. The private result stays on screen. Existing explicit private-voice consent can still permit speaking it. Public acknowledgements distinguish pending confirmations and never claim an unconfirmed action succeeded. Fish credentials, quota, connectivity, Silent/DND restrictions and Android permissions are not bypassed.

The core's frame loop is lifecycle-bound and reads the current phase speed without restarting its clock on every phase change. Hidden time is excluded and reduced motion remains honored. Additional `turn off`/phone/screen aliases mean the existing Android screen-lock countdown, not a device power shutdown. Bluetooth, flashlight and compound-request parsing are not redirected to the lock command. The existing lock-only administrator permission, cancellation, actual speech completion and visual fallback safeguards remain.

## Not executed here

No Android runtime/instrumentation cases ran. There was no connected phone or emulator available for these checks. Actual Redmi 9 microphone recordings, background/locked-screen/OEM behavior, Fish account acoustic playback, device administrator activation/lock transitions, provider widget rendering and animation performance remain unverified. Existing 4 KiB native ELF/Android 16 KiB-page limitations are not claimed fixed. This release is not a commercial-readiness or universal-action guarantee.

Use the [phone checks](../../docs/rebirth-11.3/VOICE-REPAIR.md). All older artifacts remain available. No installed app or original filesystem was reset and no uninstall was performed.
