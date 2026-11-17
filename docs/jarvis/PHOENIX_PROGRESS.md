# Phoenix evaluation milestone — not full product completion

Package-conflict follow-up: see [the preview package repair](PHOENIX_PACKAGE_CONFLICT.md) and [Phoenix preview 2](../../downloads/phoenix-2.11.9-preview2/README.md). The first debug evaluation below shared the installed release's package ID with a different certificate. Preview 2 isolates debug identity as `com.jarvis.next.phoenix` / **Jarvis Phoenix Test** to preserve the original app and its data. The milestone details below record the earlier artifact's preparation.

## Actual artifacts and source

New APK: downloads/phoenix-2.11.9-debug/Jarvis-2.11.9-phoenix.1-debug-arm64.apk. Version **2.11.9-phoenix.1-debug**, code **26**, ARM64 / min API29, target35, package com.jarvis.next. Size **55,353,197 bytes**. SHA256 **833a9687cb27378945e6ee612de39bd851a4c5199a140d2f905a29d83ae16305**. APK signature v2 and zip alignment verified. All 14 packaged native libraries report AArch64 ELF machine type183.

Source commit **51b51539c1abf33477455ccb966117e38e8c1977** includes core, wallpaper, version/packaging and setup-label changes. Prior TTS repair is included. The final artifact/evidence commit follows this source commit. GitHub still holds a7f2c83 at the time of preparation; no new push/publication has been performed. Local rollback checkpoint/phoenix-start-dc06606 preserves all earlier repair work; the original 0d57181 and a7f2c83 rollback points remain intact.

This artifact uses a newly generated standard Android DEBUG test identity. It is **not** signed by the retained 2.11.8 release identity and cannot update that installation. Preserve the existing installed release/data. No device installation, uninstall or data migration was performed. Original release credentials were unavailable and were not replaced or overwritten. A compatible update requires the original matching signing material.

## Repaired implementation paths

1. **Core callback cadence** (a2bfd33): the old gate reset its timestamp to every accepted callback; slight callback-boundary differences could repeatedly skip an extra callback and under-deliver the requested cap. CoreFramePacer retains cadence debt across small boundary jitter, accepts at most one update per callback, resynchronizes after large gaps and handles clock restart/rollover. Existing ring geometry, particles, colors and audio-state mapping are unchanged.
2. **Wallpaper transient surface stall** (c16699a): the old invalid-surface branch returned before posting its next callback. JarvisWallpaperService now retains a recovery opportunity, with up to eight retries (50→1000ms backoff) while visible, ready, interactive and not destroyed. External lifecycle/state requests reset the recovery window. Hidden/unready/destroyed states do not spin. Normal renderer/cadence/power/accessibility rules remain.
3. **Debug diagnostics**: long-press the main core to toggle real Canvas draw/callback counters. Rates are labeled Canvas draws/s and clock callbacks/s, **not display FPS**. Default off; debug builds only. No raw audio or transcripts are logged by these counters.
4. **Truthful setup text**: microphone permission no longer implies an initialized recorder. Existing Fish Audio preview/reply routing and optional English pack labels match the actual handlers. No new SDK connection, wizard step, setting handler, persistence migration or account authentication is claimed.
5. **Prior output repair**: offline TTS cursor drain timeout is an error rather than false completion; cancellation/generation checks preserved.

These are specific source-supported fixes, not proof that every observed handset freeze is repaired. Physical-device rendering/audio/lifecycle evidence remains unavailable.

## Executed verification

- Baseline Gradle compilation/unit task check: BUILD SUCCESSFUL in23s, all26 tasks up-to-date. This reused the previous verified baseline; it did not re-execute those tests.
- Full modified-source run: **BUILD SUCCESSFUL in10m58s**, 66 actionable tasks. Includes unit tests, debug lint, Android instrumentation Kotlin compilation and debug APK/native generation.
- JVM reports: **677 discovered, 676 PASS, 0 FAIL, 1 skipped** (optional PrivateMemoryBackupTest export fixture absent). Includes **24 new core/counter** cases and **14 wallpaper retry** cases. Do not add overlapping old/new suite counts as independent tests.
- Lint: **0 errors/fatals, 48 warnings, 1 information**.
- **16 new Android rendering fixtures compiled only**: 10 Compose core and6 native wallpaper pixel fixtures. Actual execution **0**; status BLOCKED. They do not test real microphone/battery/FPS or SurfaceHolder lifecycle on a phone. The original20 animation acceptance cases remain blocked too.
- Standard DEX compression repackage: **BUILD SUCCESSFUL in39s**, 3 tasks executed/37 up-to-date. Reduced the evaluation download from108,579,717 to55,353,197 bytes without removing bytecode/assets/native libraries. APK checks were repeated on the final compressed artifact.
- Existing36 companion cases were verified in the prior milestone; companion source is unchanged and those cases were not rerun here.

Evidence: TEST_EVIDENCE/PHOENIX/JVM_RESULTS.csv, jvm/*.xml, ANDROID_RENDER_CASES.csv, RESULTS.json, build logs and lint XML. Console-log copies strip trailing whitespace only; original logs are under /workspace/work/jarvis-phoenix/.

Known build diagnostics: the existing native ARM feature probe printed `unknown target CPU 'armv8-a'` / `Failed to get ARM features`; native compilation and APK assembly still completed. SDK XML/deprecation warnings remain. Architecture/package verification is not physical CPU/device compatibility evidence.

## Remaining Phoenix work

| Area | Accurate status |
|---|---|
| Main core / wallpaper | Focused repairs compiled/JVM-tested; handset freeze and actual render lifecycle BLOCKED |
| Microphone / recognizer / wake | Existing implementations preserved; no acoustic/permission/BT/background verification |
| Silero VAD | MISSING in capture path; no model installed this milestone |
| TTS/provider routing | Prior offline-helper guard included; main conversation remains configured Fish Audio; general offline fallback not restored |
| Google sign-in / Gmail / Drive cloud authorization | MISSING; requires owner OAuth project/configuration and scoped authorization |
| Calendar / Maps / YouTube | Existing Android providers/intents are not Google OAuth connectivity; runtime availability unverified |
| Guided onboarding | Existing persisted onboarding/optional setup links preserved; only misleading status text corrected; requested full resumable wizard incomplete |
| Settings organization | Existing controls/handlers/persistence retained; truthful descriptions corrected; full requested hierarchy incomplete |
| AI backend | Existing providers preserved; no live account/server credentials verified |
| Android actions / desktop companion | Existing supported capabilities preserved; broader requested desktop actions incomplete |
| iPad unsupported controls | Remain platform-restricted; no fabricated system automation or Safari deployment |
| Complete24-part feature reconciliation /100-check subsystem target | In progress, not complete; no artificial repetitions or invented hardware results |
| Compatible release update | BLOCKED by original release signing material; this is debug evaluation only |

## Build/recovery

Existing toolchain: JDK21.0.13, Gradle8.9, AGP8.7.3, Kotlin2.0.21, SDK35/build-tools35.0.0, NDK26.3.11579264, CMake3.22.1. No Android dependency upgrades/models/emulator installs. Native compile/link pools capped to1 to protect the8GiB environment. Reproduce using the existing environment and:

```bash
source /workspace/.jarvis-toolchain/env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon --max-workers=1 --console=plain -I scripts/phoenix-resource-limits.gradle -Dorg.gradle.parallel=false '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process
```

SDK/core artwork, original24 master files, stored user data, settings schemas and previous APKs were preserved. No public upload/push/deployment is authorized until the owner confirms the concrete artifact/source push. Chat's configured /codex output directory is read-only in this runtime, so GitHub is the prepared stable-delivery route. No live new download URL is asserted before publication and verification.

Next publication step: review the final artifact commit and request a normal push to origin/jarvis-next. After an approved push, independently download the new immutable URL and compare its SHA256 before giving the live link.
