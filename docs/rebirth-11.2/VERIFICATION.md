> Historical code15 candidate record. The owner subsequently requested Gmail removal and publication of a speech-repaired beta. See [the current release scope](CONVERSATION-UPDATE.md) and [current download/verification](../../downloads/rebirth-11.2-beta2/README.md). Gmail integration is absent from the current build.

# 11.2 local candidate verification — release held

Final frozen-input pipeline **BUILD SUCCESSFUL in 15m 8s**. **336 JVM tests passed**, zero failures/errors/skips. Android lint: **zero errors/fatals,43 warnings**, no lint baseline or suppression added to obtain this result. Android instrumentation test sources compiled with `:app:compileDebugAndroidTestKotlin`; **zero runtime instrumentation cases executed**. The full master directive and physical Redmi9 acceptance are not signed off.

Executed tasks: `:app:testReinstallUnitTest :app:lintReinstall :app:assembleReinstall :app:compileDebugAndroidTestKotlin`. Test count increased from317 at11.1 to336: fourteen Rebirth policy tests and five Gmail synthetic-fixture tests. The existing private-export regression input was read only by the authorized host test; it is not included in the APK or this repository change.

The new tests exercise concurrent/late-release microphone ownership, bounded recovery and RAM chat, intent-scoped app correction/ambiguity/sensitive targets, alternatives, approved alias precedence/missing target, native self/Home/social routing, speech segmentation, actually reported recognizer locales, navigation boundaries, real time-zone rules, Gmail intent/query/metadata/body decoding and attachment nonfetch. These are host tests. They do not exercise AudioRecord, SpeechRecognizer, Google OAuth, a real inbox, Fish playback, Android window routing or database migration on a phone.

## Artifact inspection

| Property | Verified result |
| --- | --- |
| Package | `com.jarvis.next` |
| Version | code15 / `11.2.0-candidate1-reinstall` |
| ABI / OS | arm64-v8a only; Android10/API29 minimum; target35 |
| Debuggable | false |
| APK bytes | 85,459,350 |
| APK SHA-256 | `06bd5958ff0b68a2ed09c5794c992d497c98538a79c5341890807ec31078469f` |
| Existing certificate SHA-256 | `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` |
| Existing certificate SHA-1 | `5F:8A:64:DE:13:21:BC:CC:6B:53:B2:55:53:4C:2E:70:5A:2C:47:71` |
| Signature / ZIP alignment | V2 verified;4-byte ZIP alignment verified |
| Added merged permissions versus11.1 | none |
| Private/optional model material | no keystores, signing archives/password files, private-memory export, optional `.gguf`/`.onnx` weights or Android-test fixture classes packaged |
| Frozen production/test/build inputs | 1537 SHA-256 hashes verified unchanged after build |
| Input-manifest SHA-256 | `a78e01120fb38c84d58e323ca80619748b2f86b7268b8f5a0dda7f4bd14814e3` |
| Publication | **No public11.2 APK; retained locally outside the checkout** |

Public11.1 APK remains byte-identical: SHA-256 `1dcc9458ed6241d86075aa76f9c177727b556f937af732c3d665ac8fac59abed`. Main and jarvis-android-download are not changed by this source update. The APK's original7.0 in-place update compatibility remains **false**: different original signing certificate, original key unavailable. No replacement key, uninstall or phone-data clear was performed.

The local validation bundle is under `/workspace/android-toolchain/artifacts/rebirth-11.2/`: APK, SHA256SUMS, public certificate verification, permissions, build-info, source-input manifest and final build log. It is not a hosted download. [Machine-readable properties](build-info.json) and [exact input hashes](source-inputs.sha256) are retained with the source. Historical artifact/release files are not rewritten.

## Warnings and limits

The43 lint warnings remain visible: ApplySharedPref4, UnusedAttribute1, ComposableNaming5, ChromeOsAbiSupport1, SetJavaScriptEnabled1, TrustAllX509TrustManager3, DrawAllocation2, ObsoleteSdkInt1, UsableSpace2, UseTomlInstead15, SetTextI18n7 and RtlHardcoded1. The three trust-manager findings point into the existing BouncyCastle1.72 dependency, not newly added app trust code. The build result is not a complete dependency/security audit. Compiler deprecation warnings also remain. The inherited Vosk4KiB ELF alignment does not establish universal16KiB-page compatibility or Play readiness.

No physical device/accessory/provider is connected. No new runtime case passed on an emulator or Redmi9. Google API/project/Android OAuth registration is not set up by the owner, so actual consent, profile, read-only mailbox queries and remote revocation are **BLOCKED / NOT TESTED**. Fish credentials/IDs, actual sound, TV/background noise, app/alias launches, content grants, overlay/privacy/rotation, locked behavior and process recreation remain **NOT TESTED**.

No wake, partial/final recognition, model/first-token, Fish/first-audio, dispatch, memory, crash or ANR benchmark was observed. No improvement percentage or “instant” measured performance is claimed. The preserved API29 emulator previously stalled in system_server/package commands and was not reset to manufacture results.

See [implementation report](IMPLEMENTATION-REPORT.md), [device checklist](ACCEPTANCE-MATRIX.md), [Google setup](GOOGLE-SETUP.md) and [release gates](RELEASE-GATES.md). Release is held under the user's explicit11.2 directive.
