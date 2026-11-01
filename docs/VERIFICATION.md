# Verification evidence

Results refer to this source build, not to the user's physical Redmi 9. See downloads/build-info.json for the phone APK hash and SDK/ABI details.

| Check | Result / evidence |
| --- | --- |
| ARM64 debug APK assembly | Passed: Gradle assembleDebug; signed APK preserved under downloads |
| JVM unit tests | 76 passed, 0 failures/errors/skips; app/build/test-results/testDebugUnitTest |
| Android lint | Passed with zero errors; warnings retained and reviewed in app/build/reports/lint-results-debug.html |
| APK signing | APK Signature Scheme v2 verification passed using Android apksigner |
| Phone ABI and SDK | ZIP inspected: arm64-v8a only; min29 / target35 |
| Local LLM JNI | Native ARM64 and x86_64 compilation passed; Qwen weights were unavailable under environment policy, so local LLM inference on a phone remains unverified |
| OCR native SDK | Built from official source for ARM64/x86_64; 16 KiB ELF alignment checked; language assets hashed; actual Android OCR read rendered HELLO JARVIS pixels |
| Neural speech models | Official English/Spanish/Persian archives hashed; real synthesis passed on Linux and Android x86_64 for all three languages with non-silent output |
| Android emulator | API29 x86_64 software emulator installed and rendered app; all five final UI scenarios passed, zero failures or skips |
| Live news/weather/model download | Environment destination policy blocks the runtime hosts; parser fixtures and URL policy tested, live operation on the phone unverified |
| Cloud provider requests | Protocol/SSE tests passed; no real API keys provided, so account-backed calls unverified |
| Hardware-dependent behavior | Actual Redmi microphone, audible voice quality, wake accuracy, Bluetooth, background persistence, battery and speed remain unverified |

Lint warnings include Android storage-space guidance, the deliberate phone-only default ABI, synchronous credential commits whose result is checked, version-catalog guidance, license-text concatenation, and unused permissive crypto-library TLS helper classes from PDFBox's transitive Bouncy Castle dependency. The app's OkHttp clients use default certificate validation; no trust-all manager is installed. Warnings were not suppressed to imply a cleaner result.

Instrumented checks exercise real UI/runtime/Room and native inference. Native OCR and all-three-language neural synthesis passed on API29 x86_64. Voice models were preloaded from verified archives for native tests; that does not establish Android download-client behavior or speaker audibility. Initial UI runs exposed a duplicate selector and reply visibility/keyboard issue; those were corrected and the affected cases passed. The final complete UI-only run passed all five scenarios with zero failures or skips. Native OCR and all-three-language synthesis passed separately; there was no need to repeat the unchanged native inference after the keyboard fix. The public downloads/verification.json records the final outcomes. A successful emulator run does not validate physical microphone or Redmi performance.
