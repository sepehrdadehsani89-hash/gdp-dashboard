# Verification attempts

1. Focused attempt 1: application Kotlin/Java compilation passed; unit-test compilation failed in the newly added OpenAiResponsesProtocolTest history-budget assertion because String.length was called as a function. No tests executed by that attempt. Corrected the test syntax; the exact compiler error is preserved in focused-test-attempt-1.log.
2. Focused attempt 2: PASS — 277 JVM tests, zero failures/errors/skips. Includes final review fixes for retained signing validation order, validation before key storage, API-card failure visibility/retry timing, and exact snapshot model matching.

No failed attempt is represented as a successful test run. Mock fixtures do not establish live OpenAI access. All physical-device and emulator test execution remains unavailable in this workspace.

3. First full verification: intentionally stopped with exit 143 before completion to close the discovered legacy-stream validation gap. No results from that interrupted run are reported as passing. Adds six regression tests for periodic metadata-only proof expiry while settings are closed; preserves recent success and real failure evidence. No background inference is performed.

4. Final full verification after legacy completion repair: PASS — Gradle exit 0 in 11m 42s, 1,516 JVM cases (1,515 passed, zero failures, one optional private-fixture skip), lint zero errors/55 warnings/one informational item, assembleDebug passed and Android test Kotlin compilation passed. Physical/emulator tests executed: zero. Modern Responses and legacy Chat Completions both require completed, nonempty output. The legacy route has 26 dedicated mock protocol/transport tests. Application source is frozen again before verification.

5. SDK artifact inspection: PASS — 23 checks, zero failures; apksigner/zipalign/aapt2, CRC, ARM64 ELF, previous signer and 134 native/asset files checked. This is static APK inspection, not handset installation.
