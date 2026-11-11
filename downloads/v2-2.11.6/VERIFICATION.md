# Jarvis 2.11.6 release verification

The final signed Gradle pipeline completed in 5m 15s. Implementation source checkpoint: `99f8a44525512fd3e1eb675735d9c7ea3bf418ee`; source base: `237b3638a769e6ce523840332f90fb785597015f`. The adjacent source-input manifest records 1604 inputs, SHA-256 `a6dca9f4e8a91e94ae1632b94e460b2b53279e001c44ad37dcb39686c3e2d0e1`. Every recorded input matched after the final build.

| Check | Observed result |
| --- | --- |
| Package / version / code | `com.jarvis.next` / `2.11.6` / 23 |
| Android JVM unit tests | 527 passed; 0 failures/errors/skipped |
| Companion Python tests | 30 passed, including actual HTTPS and isolated Chromium local-page fixture |
| Catalog integrity | 4 passed; 1,500 requirements / 1,000 scenarios / 2,000 examples preserved |
| Lint | 0 errors/fatals; 48 warnings |
| Android instrumentation | Source compiled; 0 runtime cases executed |
| APK signing | V2 verified; retained SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` |
| ZIP alignment | 4-byte verified |
| Build target | ARM64 only; min SDK 29, target SDK 35; non-debuggable |
| APK size | 85,300,261 bytes |
| APK SHA-256 | `c2be710b6e605661a276eca67ff7c44297ba51b3631ff2e2b14c9304bf7ab5e8` |
| Permissions added versus verified 11.5 | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` only; boot permission already existed in the merged APK |
| Packaging | No private keys/backups/passwords/private export/model weights or new test-only classes; no Gmail/auth SDK |
| Prior critical capabilities | Charging/reminders, widgets, lock countdown, content, speech, conversations and core classes retained in DEX |
| Connected physical Android device | None; final ADB list empty |
| Redmi 9 audio/lock-screen/latency/FPS | Not verified |
| Desktop browser | Real local Chromium page and HTTP-404 failure; not Google or Windows/macOS |
| Companion distribution | Source ZIP only; native CI recipe supplied, not executed |
| Master completion | 17 PARTIAL / 1,483 NOT_STARTED; zero claimed fully complete |

The actual test suites exercise policies, state transitions, stale callbacks, call limits, focus gating, parser behavior and protocol receipts. They do not establish real acoustic recognition, human speaker authentication or all 1,500 features. [Implementation report and hardware acceptance](../../docs/v2/REPORT.md) · [Phone instructions](../../docs/v2/PHONE-GUIDE.md).

The original Jarvis 7 signing line is not recovered by this build. It updates the retained newer signing line, including 11.5, in place. A certificate mismatch must not be worked around by deleting the installed app/data.
