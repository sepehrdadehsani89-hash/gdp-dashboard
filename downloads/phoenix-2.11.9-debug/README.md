# J.A.R.V.I.S. Phoenix — debug evaluation APK

Version **2.11.9-phoenix.1-debug**, code **26**, package `com.jarvis.next`, ARM64 / Android 10+ (API29). This is a new, actually assembled evaluation APK. It is not the old 2.11.8 download and it is not a finished full-master release.

[Download debug APK](Jarvis-2.11.9-phoenix.1-debug-arm64.apk) · [Checksums](SHA256SUMS) · [Exact build information](build-info.json) · [Milestone report](../../docs/jarvis/PHOENIX_PROGRESS.md)

## Signing and data preservation

The APK uses the standard Android **debug** signing identity, verified with APK Signature Scheme v2. It cannot update an existing installation signed by the retained 2.11.8 release key. Keep the installed release and its user data; do not uninstall it merely to force this evaluation APK onto the same phone. A compatible in-place update requires the original signing key. No old release key, user profile, model, database or pairing was overwritten. No device installation has been performed here.

## What changed

- Core callback pacing no longer compounds small frame-boundary rounding into half-rate update delivery. Existing artwork/palette/geometry unchanged.
- Wallpaper keeps a bounded retry after a transient invalid surface instead of losing its only render callback. Hidden/unready/noninteractive/destroyed engines do not retry.
- Debug only: long-press the existing main core to toggle real callback/draw counters. Labels say **Canvas draws/s**, **clock callbacks/s**, and **Not display FPS**. Default off.
- Setup states permission granted rather than claiming the microphone was initialized. Existing Fish Audio and optional English pack descriptions match their actual routing.
- Includes the prior offline TTS drain timeout/cancellation repair.

## What was verified

676 JVM cases passed, 1 optional private export fixture skipped, 0 failures. Includes 24 new core pacing/counter and 14 wallpaper recovery policy cases. Debug lint: 0 errors/fatals, 48 warnings and 1 informational finding. Sixteen new Compose/native bitmap instrumentation cases compiled only; **0 executed** because no Android runtime is connected. APK package/version/ARM64 native ELF architecture, v2 signature, checksum and zip alignment verified. DEX uses standard compressed packaging; source bytecode was not removed to reduce download size.

**This does not prove the reported on-phone freezes are fully fixed.** No handset screenshots, frame timing, microphone quality, acoustic wake rate, Google sign-in, device permissions or physical installation result is claimed.

Google sign-in/full guided wizard/Settings hierarchy, Silero VAD, broader desktop controls and the complete historical specification remain unfinished. The existing desktop companion is preserved. No new external service was deployed.

One pre-existing native ARM feature-probe command printed `unknown target CPU 'armv8-a'` and a `Failed to get ARM features` warning; CMake/native compilation nevertheless completed successfully. All packaged native libraries report AArch64 ELF machine type 183. Do not interpret this as physical CPU compatibility testing. Existing deprecation/SDK XML metadata warnings remain recorded in the build log.

SHA-256: `833a9687cb27378945e6ee612de39bd851a4c5199a140d2f905a29d83ae16305`. Size: 55,353,197 bytes.
Source checkpoint: `51b51539c1abf33477455ccb966117e38e8c1977`. Build with the existing pinned toolchain and `-I scripts/phoenix-resource-limits.gradle` for bounded native compile jobs; detailed commands/evidence are in the milestone report.

Publication requires owner confirmation. The build receipt records pre-publication status; the owner-facing delivery message will supply a live URL only after an actual upload/download checksum check.
