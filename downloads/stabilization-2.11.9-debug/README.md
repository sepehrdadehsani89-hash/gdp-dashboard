# Jarvis stabilization preview

[Download the ARM64 APK](Jarvis-2.11.9-stabilize.1-debug-arm64.apk) · [Phone guide](../../docs/jarvis/STABILIZATION_USER_GUIDE.md) · [Engineering report](../../docs/jarvis/STABILIZATION_ENGINEERING_REPORT.md) · [Device acceptance](../../docs/jarvis/STABILIZATION_DEVICE_ACCEPTANCE.md)

Version **2.11.9-stabilize.1-debug**, code **30**, Android 10+ / ARM64. Display name **Jarvis**; package **com.jarvis.next.phoenix**. This updates the retained Phoenix preview, not the differently signed original production app. Preserve the original app and its data if Android rejects a different installed certificate. Android still requires your installation approval.

Repairs microphone/TTS ownership, meaningful battery speech, voice retry and the forced-motion preference projection; adds allowlisted voice settings/contact nicknames, searchable settings and one-time guided setup. The holographic home, renderer, assets and working integrations remain. Some new navigation labels use English; existing Spanish voice/language controls remain. Native handset behavior still needs actual testing.

Verification: **1,296 JVM passed, zero failed, one optional export fixture skipped**; lint **zero errors, 55 warnings, one informational item**; Android tests compiled but **zero executed**; **20 artifact checks passed**. All 134 compared native/asset entries match the previous preview byte for byte. No real microphone/GPU/provider-account/device result is claimed. Read the report before treating this as a production release.

SHA-256: `ad796f1920f4853ff855acd710d1360bfa32a1e185ca733d4850c071ab7261ed`

Bytes: 56,199,459

Source checkpoint: `fbf2707b8da467af251dcd09f8b822cf369eeb50`

Preview certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`

No signed production AAB was produced. The original key is unavailable/uncertain and the signing guard fails; API 36, Vosk 16 KB compatibility and Play/publisher/device requirements remain open. See [Play readiness](../../docs/jarvis/GOOGLE_PLAY_READINESS.md) and [signing recovery](../../docs/jarvis/SIGNING_RECOVERY_PLAN.md). No store approval or zero-bug guarantee is claimed. Existing release artifacts were not overwritten.
