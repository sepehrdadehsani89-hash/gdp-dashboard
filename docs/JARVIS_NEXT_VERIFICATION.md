# Jarvis Next 0.1.0 verification

This is the separate `com.jarvis.next` installation, based on published Jarvis 0.1.3. The unfinished 0.1.4 source and old signing key were not recovered. Original Jarvis and `main` remain unchanged.

| Check | Result |
| --- | --- |
| JVM suite | 98 passed; zero failures, errors or skips |
| Android checks | 15 passed; one optional neural-voice check skipped because its downloaded models are absent; zero unresolved failures |
| Connected folder | Real owner-issued SAF grant, retained read-only access, nested browsing/file reads, disconnect, fresh reconnect grant and revocation passed |
| Live controls | Direct opening of a fixture app, semantic tap, actual focused-draft replacement, stale-target rejection and manual-only Send control passed |
| Local provider | Real UI switches Gemini selection to local Qwen and disables cloud speech without sending a request or downloading a model |
| UI and privacy | Onboarding without keys, keyless time, memory persistence, Persian UI, private on-device speech and provider-dialog fixture flows passed |
| Native OCR and screen | Rendered-pixel OCR and Android-consented one-shot screenshot capture/clear passed |
| Lint | Debug and release: zero errors, zero fatal findings; warnings retained in build metadata |
| Phone artifact | Signed, non-debuggable, unminified release; ARM64-only; native local engine is ELF64 AArch64 |
| Signature/alignment | Android APK Signature Scheme v2 and zip alignment verification passed |
| Private signing backup | Encrypted backup decrypted and compared byte-for-byte with the new keystore and its password; private files are outside Git |

Android checks ran on a dedicated API29 x86_64 software emulator; the shipped phone APK is ARM64. Initial fixture permission, projection and grant-lifecycle errors were corrected. Slow emulator DEX verification occurred before application code, so the test installations were precompiled using Android's package compiler. The framework was restarted to clear the resulting dead Accessibility binding. The local-provider button check was rerun using lazy-list scrolling. Final individual outcomes are in [verification.json](../downloads/jarvis-next-verification.json).

The optional neural-voice synthesis check is skipped, not a pass. Physical Redmi 9 microphone quality, audible output, wake reliability, battery, RAM/latency, real WhatsApp recipient/send behavior, and an actual local-model download/generation were not tested here. Local inference requires the separate ~400 MB model download and may be slow. No real Gemini key or account/quota was used. Protocol error handling and provider selection were tested without claiming Google quota changed.

Keep Play Protect enabled. Neither the new package nor its signing identity establishes Google approval or fixes HTTP 429. Install Jarvis Next alongside Jarvis; do not uninstall the original app. Keep the new signing backup privately outside this environment for future Next updates. See [build identity and checksum](../downloads/jarvis-next-build-info.json) and [setup/backup instructions](JARVIS_NEXT.md).
