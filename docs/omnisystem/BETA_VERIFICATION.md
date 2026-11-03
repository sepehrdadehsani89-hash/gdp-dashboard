# Code-11 beta verification

The phone is still running original Jarvis 7.0. This work did not uninstall, modify or clear its data. No original private signing key was recovered and no signing key was generated in this upgrade. This beta uses the previously retained reinstall identity and cannot install over the original signature. It can update the earlier code-10 reinstall build with the same certificate.

## Completed checks

| Check | Executed result |
| --- | --- |
| Final signed-variant JVM suite | **250 tests, zero failures/errors/skips**, including the privately supplied legacy-export fixture. The fixture was never committed or packaged. |
| Final signed-variant build/lint | `testReinstallUnitTest`, `assembleReinstall`, `lintReinstall` succeeded. Lint: **zero errors, 33 warnings**. Error enforcement remains enabled. |
| Preceding debug checkpoint | Same 250 distinct tests passed; debug build/lint and Android-test Kotlin compilation passed, with zero lint errors/33 warnings. The final additional change registers/releases shared telemetry visibility owners in private activity lifecycle methods; the signed variant was recompiled and retested afterward. These counts are not 500 distinct tests. |
| APK identity | `com.jarvis.next`, version code **11**, `10.0.0-beta1-reinstall`, min SDK 29/target SDK 35, non-debuggable, ARM64 only. |
| Signature and alignment | APK Signature Scheme v2 verified; retained certificate matches the pinned fingerprint below; four-byte ZIP alignment passed. |
| Package content | No private export, keystore, backup/unlock file, GGUF or ONNX weights. Optional integration license/attribution notice is packaged. |
| Source consistency | Public `source-inputs.sha256` records the final selected source/test/resource inputs. Its digest is in build-info.json. No application source changed after final artifact verification. |

APK: **83,439,601 bytes**. SHA-256: `f9bcaf84b3a350772c5cf58898864d74aca6179452184df40eb60711997212b1`.

Retained public certificate SHA-256: `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. This differs from the original installed 7.0 certificate. The original `release` guard remains pinned to the original certificate; the separate `reinstall` guard accepted the retained newer key. Future updates on this newer line need that same private key and an increasing version code. Off-cloud copies of the encrypted signing backup and separate unlock file have not been confirmed.

New unit cases cover actual AudD parsing, WAV/spectrum math, Spotify PKCE/state/expiry/origin checks, bounded/cancelled HTTP and redirect refusal, finite embeddings/similarity/ambiguity/profile limits, bounded wake PCM buffers, map coordinate/host constraints, real multimodal image payloads for three providers, elapsed timer/reboot/status rules, contextual weather/date parsing and current-user-bound compound device clauses. These are automated policy/transport checks, not real account or handset tests.

Logs remain in the workspace under `/workspace/android-toolchain/`: `omni-public-v11.log` for the final signed pipeline and `omni-delivery-v11.log` for the preceding debug/Android-test compilation checkpoint. The final pipeline completed in 5m21s. Toolchain: verified Gradle 8.9 wrapper, Temurin 21.0.13+9, SDK/build-tools 35, NDK 26.3.11579264, CMake 3.22.1.

The warnings include existing dependency/style/drawing/ABI findings and the intentionally enabled JavaScript used by the consented official OSM embed. The WebView disallows file/content access, mixed content, cookies, storage and an app JS bridge; resource hosts are restricted. Dependency bytecode also triggers trust-manager findings; the new transport uses normal certificate validation with no trust-all manager. Warnings were not suppressed to obtain a pass.

## Android runtime checks blocked

The existing API-29 x86_64 emulator was restarted with one software-emulated CPU without wiping userdata. Earlier attempts had system-service/package-install failures. In this attempt `sys.boot_completed` remained unset, the package service was initially missing, then package commands repeatedly exceeded their 20-second bounds. After roughly twelve minutes the task-owned emulator was stopped gracefully; userdata was retained. **No new instrumentation case ran or passed.**

The compiled new Android checks cover AndroidKeyStore AES-GCM/AAD storage, cancellation preventing wake resumption and optional real Sherpa embedding execution when the pinned model is explicitly provisioned. Compilation is not execution. The complete importer file-picker UI check also remains unestablished after earlier emulator failures; historical database/parser passes do not prove the picker flow.

## Unverified and unfinished boundaries

No physical Redmi 9, human microphone enrollment, replay/identity accuracy, live AudD/Spotify account, cloud image-provider call, map/geocoder field test, actual accessory, exact-alarm delivery or sustained performance/battery measurement was available. The host-only real-model smoke described in OPTIONAL_SERVICES.md is not an Android or handset test. Optional services require real account access, applicable commercial approval and configured credentials; no successful live call is claimed.

Full localization of inherited technical diagnostics/settings is unfinished. Plans stop at approval and explicitly report unrun steps; automatic continuation afterward is not implemented. The legacy export preserves only its documented subset and this beta adds no complete private-feature export. Shared headers display cached weather; the Weather module holds detailed source timestamps and stale status. Source implementation does not establish physical UI, permissions or service correctness.

Existing native `libvosk.so` has a 4 KiB ELF-page alignment limitation. ZIP alignment does not establish universal 16 KiB-page or Play-distribution compatibility. This is an ARM64 sideload beta for Android 10+, not the complete master directive or a commercially certified release.

See [acceptance status](ACCEPTANCE.md), [service/model setup and licenses](OPTIONAL_SERVICES.md), [change inventory](BETA_CHANGED_FILES.txt) and [private migration/signing instructions](REINSTALL.md).
