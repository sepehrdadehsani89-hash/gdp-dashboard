# Verification evidence

Results refer to this source build, not to the user's physical Redmi 9. See downloads/build-info.json for the phone APK hash and SDK/ABI details.

| Check | Result / evidence |
| --- | --- |
| ARM64 debug APK assembly | Passed: Gradle assembleDebug; signed APK preserved under downloads |
| JVM unit tests | 83 passed, 0 failures/errors/skips; app/build/test-results/testDebugUnitTest |
| Android lint | Passed with zero errors; warnings retained and reviewed in app/build/reports/lint-results-debug.html |
| APK signing | APK Signature Scheme v2 verification passed using Android apksigner |
| Phone ABI and SDK | ZIP inspected: arm64-v8a only; min29 / target35 |
| Local LLM JNI | Native ARM64 and x86_64 compilation passed; Qwen weights were unavailable under environment policy, so local LLM inference on a phone remains unverified |
| OCR native SDK | Built from official source for ARM64/x86_64; 16 KiB ELF alignment checked; language assets hashed; actual Android OCR read rendered HELLO JARVIS pixels |
| Neural speech models | Official English/Spanish/Persian archives hashed; real synthesis passed on Linux and Android x86_64 for all three languages with non-silent output |
| Android emulator | 0.1.2: two Gemini settings scenarios passed on API29 x86_64, zero failures/skips. 0.1.1 historical baseline: six UI/privacy scenarios passed; unchanged native checks were not repeated |
| Live news/weather/model download | Environment destination policy blocks the runtime hosts; parser fixtures and URL policy tested, live operation on the phone unverified |
| Cloud provider requests | Protocol/SSE tests passed; no real API keys provided, so account-backed calls unverified |
| Hardware-dependent behavior | Actual Redmi microphone, audible voice quality, wake accuracy, Bluetooth, background persistence, battery and speed remain unverified |

Lint warnings include Android storage-space guidance, the deliberate phone-only default ABI, synchronous credential commits whose result is checked, version-catalog guidance, license-text concatenation, and unused permissive crypto-library TLS helper classes from PDFBox's transitive Bouncy Castle dependency. The app's OkHttp clients use default certificate validation; no trust-all manager is installed. Warnings were not suppressed to imply a cleaner result.

Instrumented checks exercise real UI/runtime/Room and native inference. Native OCR and all-three-language neural synthesis passed on API29 x86_64. Voice models were preloaded from verified archives for native tests; that does not establish Android download-client behavior or speaker audibility. Initial UI runs exposed a duplicate selector and reply visibility/keyboard issue; those were corrected and the affected cases passed. The final complete UI-only run passed all six scenarios with zero failures or skips. Native OCR and all-three-language synthesis passed separately; there was no need to repeat the unchanged native inference after the keyboard fix. The public downloads/verification.json records the final outcomes. A successful emulator run does not validate physical microphone or Redmi performance.

Version0.1.1 adds a real Android regression proving private-memory speech stays on-device even with optional cloud voice enabled. All tool replies deny cloud speech by default; only general AI conversation without private attachment/memory context can opt into it.

Version 0.1.2 adds Gemini model discovery (only models advertising `generateContent`), explicit connection testing, and bounded known-code error classification. Seven new JVM checks include real HTTP requests against a local mock server, exact API paths/headers, Live-only filtering, sample-only test data, failure reporting and credential non-disclosure. No real Google key was available; live account access, regional eligibility and generation quota must be checked on the phone. The package, certificate and requested phone capabilities are preserved.

The 0.1.2 Android checks use explicit local UI fixtures, not a live Google account. They verify compatible-model selection → test → save, and a failed test that never displays a success claim. Both passed on the existing API29 x86_64 emulator. An initial test could not find the lazy list's off-screen Save button; the test now scrolls the containing list before clicking it.
