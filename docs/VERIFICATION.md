# Verification evidence

Results refer to this source build, not to the user's physical Redmi 9. See downloads/build-info.json for the phone APK hash and SDK/ABI details.

| Check | Result / evidence |
| --- | --- |
| ARM64 debug APK assembly | Passed: Gradle assembleDebug; signed APK preserved under downloads |
| JVM unit tests | 75 passed, 0 failures/errors/skips; app/build/test-results/testDebugUnitTest |
| Android lint | Passed with zero errors; warnings retained and reviewed in app/build/reports/lint-results-debug.html |
| APK signing | APK Signature Scheme v2 verification passed using Android apksigner |
| Phone ABI and SDK | ZIP inspected: arm64-v8a only; min29 / target35 |
| Local LLM JNI | Native ARM64 and x86_64 compilation passed; model download/inference on the phone remains unverified |
| OCR native SDK | Built from official source for ARM64/x86_64; 16 KiB ELF alignment checked; language assets hashed |
| Neural speech models | Official English/Spanish/Persian archives hashed; real synthesis produced valid WAVs on matching Linux runtime |
| Android emulator | API29 x86_64 software emulator booted; application UI tests pending execution |
| Live news/weather/model download | Environment destination policy blocks the runtime hosts; parser fixtures and URL policy tested, live operation on the phone unverified |
| Cloud provider requests | Protocol/SSE tests passed; no real API keys provided, so account-backed calls unverified |
| Hardware-dependent behavior | Actual Redmi microphone, audible voice quality, wake accuracy, Bluetooth, background persistence, battery and speed remain unverified |

Lint warnings include Android storage-space guidance, the deliberate phone-only default ABI, synchronous credential commits whose result is checked, version-catalog guidance, and unused permissive crypto-library TLS helper classes from PDFBox's transitive Bouncy Castle dependency. The app's OkHttp clients use default certificate validation; no trust-all manager is installed. Warnings were not suppressed to imply a cleaner result.

Instrumented checks are real UI/runtime/Room interactions, not fake LLM answers. Their final results and screenshots will be recorded here after execution. A successful emulator run does not validate physical microphone or Redmi performance.
