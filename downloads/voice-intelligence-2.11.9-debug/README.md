# J.A.R.V.I.S. Voice Intelligence — signed preview

[APK](Jarvis-2.11.9-voice.1-debug-arm64.apk) · [SHA-256](SHA256SUMS) · [Build receipt](build-info.json) · [Implementation and test report](../../docs/jarvis/VOICE_INTELLIGENCE_PROGRESS.md)

Version **2.11.9-voice.1-debug**, code **28**. Application ID **com.jarvis.next.phoenix**, launcher label **Jarvis Phoenix Test**, ARM64 / Android10+ (API29), target35. Size **55,399,061 bytes**.

SHA-256: `24b08f9519231127108e10f133326eb2646ce3f6fa5757c45294441338df459c`.

## Installation identity

This preview has a separate package ID to avoid the reported conflict with the original release certificate. Keep the existing J.A.R.V.I.S. app and its data. This separate app starts with its own settings, permissions, speech packs, backend and output-voice configuration; existing credentials/pairings are not automatically migrated. The debug certificate matches the earlier Phoenix preview certificate. Original production package/signing configuration and all older APKs remain intact. A compatible in-place update of the original app still requires its original signing material, unavailable here.

## Changes

- Configurable pause allowance, stable/oscillating transcript endpointing, actual final-result confidence gates,60s absolute turn cap and finite stalled-input/finalization recovery. An unrecognized compound tail requires retry rather than executing an accepted prefix.
- Captions show only the actual assistant audio segment when playback starts. User preview is opt-in in the main app; wallpaper/overlay are assistant-only. Cancel/error clears; completion has configurable linger. Separate conversation history remains available.
- Wallpaper recovery covers unsuccessful Canvas acquisition/posting as well as invalid surfaces. Existing core cadence repair is retained.
- Real-charging-gated green transport particles in the existing app core and wallpaper, with reused buffers and bounded18/6 particle budgets. Hidden/unplugged/full/reduced-motion states stop incoming transport.
- Settings → Voice Intelligence offers functional controls and an explicit microphone test that never submits speech to AI or executes commands. Diagnostics show actual runtime metadata when run on a phone.

## Actual verification and limits

Build succeeded. **789 JVM tests:788 passed, zero failed, one optional private-export fixture skipped.** Debug lint: zero errors/fatals,48 warnings, one information finding. **20 APK inspection checks passed**, including v2 signature, package/provider/permission isolation, alignment/CRC and ARM64 ELF architecture. All134 native-library/asset files from Phoenix.1 are unchanged byte for byte.

Twenty Android rendering fixtures compiled (including four new charging bitmap cases); **zero executed**. Twenty device acceptance scenarios are documented as **BLOCKED**. No Redmi9 installation, acoustic accuracy, actual frozen-core recovery, charger, FPS, CPU/GPU/thermal or battery measurement has been performed. Pure timing/geometry tests do not establish those physical results.

Current VAD is explicitly ASR/RMS/text heuristic processing. A validated Silero model was not installed; the optional existing local speaker model needs separate consent/configuration and cannot promise perfect separation. Interruption remains narrow stop/wait control with the enabled wake service and supported AEC, not unrestricted overlapping speech. Live AI/output requires the owner's actual configured service/model/credentials. No fake answers, neural confidence or production signing are claimed.

Source checkpoint: `1b1a344b5fc7cab6077ac5df3dc9857f29e0c66b`. GitHub publication remains subject to the owner's explicit confirmation rule. These relative links describe the prepared artifact folder; a public URL must be independently downloaded and checksum-verified after an approved push.
