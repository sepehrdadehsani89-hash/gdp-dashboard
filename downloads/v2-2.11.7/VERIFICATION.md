# Jarvis 2.11.7 verification

Implementation checkpoint: `c1854612ce6dde22d6ffe031be7a62d877654058`; base: `3734c0dcf897eb9d80f5f3c7ec466a8495293893`. Final pipeline: 8m 8s. The source manifest freezes and verifies 1610 source/config inputs; manifest SHA-256 `c30d58e2232856df8f6805debd157eb9b4884ff6f160267d3a608d970521e752`.

| Check | Observed result |
| --- | --- |
| Package / version / code | `com.jarvis.next` / `2.11.7` / 24 |
| JVM tests | 548 passed; zero failures/errors/skips |
| New regressions | 21 tests: actual Vosk N-best JSON schema, malformed-score rejection, exact low-confidence controls, repeated call lifecycle, genuine failure/protected-action guards, language phrases and pickup sensor sequences |
| Logical call sequence | 20 deterministic low-score wake → call → ringing → Answer → follow-up → End repetitions; not acoustic tests |
| Lint | Zero errors/fatals; 48 warnings, 1 informational |
| Android instrumentation | Source compiled only; zero runtime cases executed |
| Signature | V2 verified, retained certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` |
| APK alignment / ABI | 4-byte ZIP alignment; ARM64 only |
| SDK / debug | Min 29, target 35; non-debuggable |
| APK bytes / SHA-256 | 85,347,645 / `a27ff31e9f569658fdfe079cb8ac0deec4c9f0515dd3dbc054f303a9e90ecf0d` |
| Merged permissions versus verified 2.11.6 | No additions |
| APK inspection | No private keys/backups/passwords/data export/model weights/new test classes; prior critical feature classes retained |
| Physical Android device | None attached; no real Redmi 9 verification |
| Fish account/acoustic voice/latency | Not verified here |
| Pickup/proximity / language hot handoff | Software policy and source checks; hardware testing pending |
| Companion/catalog | Unchanged from 2.11.6; previous 30/4 tests recorded separately, not rerun or counted as new tests |

No replacement Android signing key was generated. This APK updates the retained newer signing line, including 2.11.6. An original-7 certificate mismatch is not repaired by this release. Keep installed data if Android rejects the certificate.

[Implementation report and phone acceptance steps](../../docs/voice-2.11.7/REPORT.md). Recognition regressions use supplied final JSON, clocks and sensor samples. They cannot prove microphone decoding, human speaker identity, perfect pocket rejection, audible performance or every possible question. The full master backlog remains staged.
