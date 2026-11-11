# Jarvis 2.11.8 measured release checks

Source base `edf4464971926e30004885d1e118eed511b4d154`; implementation checkpoint `c8b3a20119759ab85d1add90aa280018e29738be`. Final Gradle pipeline: 6m 17s. Verified source/config manifest: 1633 inputs, SHA-256 `ef48ca9520fb77cc27fca0afdecb7b897d9d299058e92332ba0c202b2f33bf45`.

| Check | Observed result |
| --- | --- |
| Package/version/code | `com.jarvis.next` / `2.11.8` / 25 |
| JVM software tests | 619 passed, zero failures/errors/skips; 71 new methods |
| Companion Python | 36 passed; actual local HTTPS/Chromium fixture, authenticated capabilities, no browser launch on probe, duplicate idempotence and actual404 failure |
| Lint | Zero errors/fatals, 48 warnings, 1 informational |
| Android tests | Source compilation passed; zero Android runtime cases executed |
| APK signing | V2 verified; retained certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` |
| APK size / SHA-256 | 85,793,661 / `ed7eddcb66d0a99aac34992098a1e36c47e400276732c286149ddc94f5fddf0a` |
| ABI / SDK / debug | ARM64; min29/target35; non-debuggable; 4-byte ZIP alignment |
| Merged permissions versus 2.11.7 | No additions |
| Packaging/source integrity | Frozen inputs match; no private keys/backups/passwords/private export/model weights/new test-only classes packaged |
| Video6158 | Not attached or locally available; no invented timeline/transcription |
| Hardware/target platforms | No connected physical Android; no native Windows/macOS/phone audio round trip |
| Performance | No actual handset wake accuracy, acoustic latency/p95, FPS, battery/thermal, crash-rate or screenshot measurement |

Unit loops include 100 text wake,30 follow-up and20 combined-prefix trials. These are logical regressions. The standalone arithmetic check and local browser fixture are actual host checks, not Redmi9 measurements. A test caught unsupported exponent syntax being accepted by the basic arithmetic request parser; the guard was corrected and the final suite passed. No failing test was deleted to obtain a pass.

Existing source datasets, encrypted profiles/pairing/voices and assets were preserved. New stores are additive; see [rollback](../../docs/finish-polish/Rollback.md), [security review](../../docs/finish-polish/Security_Review.md) and [remaining acceptance](../../docs/finish-polish/Feature_Matrix.md). Original-7 signing compatibility is not recovered by this newer signing-line update.
