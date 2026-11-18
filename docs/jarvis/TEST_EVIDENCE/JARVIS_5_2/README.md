# Jarvis 5.2 verification evidence

| Check | Result | Record |
|---|---|---|
| JVM | 2,569 passed, one private-export fixture skipped, zero failed | [Suite counts](jvm-summary.json) and [raw XML](jvm/) |
| Kotlin client against Python over HTTPS | All 28 passed within the JVM total | [Interop suite](jvm/TEST-com.jarvis.assistant.companion.CompanionTlsIntegrationTest.xml) |
| Linux companion | All 103 passed, including mDNS, HTTPS and isolated Chromium | [Full run](companion-full.log) and [browser receipt assertions](companion-browser-result.json) |
| Android build | Debug APK assembled and instrumented test source compiled | [Gradle log](release-gate-second.log) |
| Android lint | 0 errors, 74 warnings, one informational finding | [Counts](lint-summary.json) and [triage](lint-triage.md) |
| APK | All 35 package, signature, archive and content checks passed | [Results](artifact-verification.json) and [checker](verify_artifact.py) |
| Physical phone, native Windows and owner-account live calls | Zero runs | [Physical acceptance](../../JARVIS_5_2_ACCEPTANCE.md) |

The earlier build, test and browser failures remain in their original logs. The initial browser run recorded PAGE_LOAD_TIMEOUT. The corresponding event trace showed HTTP 200 before the browser acknowledged subscriptions. The corrected companion subscribes before navigating and verifies the frame and loader identity. A diagnostic invocation from the repository root failed to import the test module. Running it from the companion directory reached the actual browser. The discovery tests also recorded a Python asyncio ResourceWarning. The final tests completed successfully with that warning visible.

The APK checker compares the signed candidate against the retained 4.1 preview and original production certificate. The setup, test matrix and owner requirements appear in the [implementation report](../../JARVIS_5_2_REPORT.md). Run `sha256sum -c SHA256SUMS` in this directory to check the captured evidence.
