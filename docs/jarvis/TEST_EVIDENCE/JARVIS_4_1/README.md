# Jarvis 4.1 verification evidence

These are results from the actual cloud workspace, before GitHub publication. The final APK's source revision is recorded in the new artifact directory's README. The published starting revision is `62d3e8e86146157b6f35d648977acb971d0985d7`.

| Check | Actual result | Evidence |
| --- | --- | --- |
| Full JVM suite | 2,109 cases: 2,108 passed, 0 failed/errors, 1 skipped | `jvm-summary.json`, 124 raw XML files in `jvm/` |
| Android lint | 0 errors, 56 warnings, 1 informational item | `lint-results-debug.xml`, `lint-results-debug.txt` |
| Debug APK assembly | Passed | `release-gate.log` |
| Android test compilation | Passed; **0 device tests executed** | `release-gate.log` |
| Companion suite, final serial run | 36 passed, 0 failed/skipped, 2.657 seconds | `companion-tests.log` |
| Browser fixture | Actual isolated Linux Chromium and local HTTPS receipts, idempotency and 404 failure | `companion-browser-result.json` |
| APK SDK/ZIP/binary checks | 30 passed, 0 failed | `artifact-verification.json`, SDK text outputs |
| Physical Redmi 9 install/update/audio/control tests | **NOT RUN** | No connected authorized device or emulator |
| Live provider and Google account reads | **NOT RUN** | No account credentials available here; Google project setup still required |

The skipped JVM case is `PrivateMemoryBackupTest.validatesUserExportPrivatelyWhenProvided`: the optional private export fixture was not supplied. The suite's other cases ran. `feature-test-summary.json` maps 16 repaired/added feature groups to real suites, each with at least 21 JVM cases. Shared policy tests are not counted as independent hardware or per-product tests.

## Reproduction

The existing project toolchain was used: JDK 21, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android SDK/build tools 35. No emulator or additional toolchain was installed during this gate.

From the repository root, with the compatible JDK and Android SDK configured:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  :app:compileDebugAndroidTestKotlin \
  --no-daemon --max-workers=1 --console=plain \
  -I scripts/phoenix-resource-limits.gradle -Dorg.gradle.parallel=false \
  '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process
```

The final Android gate exited 0 in 13m19s. For the companion, run `python -m unittest discover -s tests -v` in `companion/` using its configured test environment. Its actual browser fixture requires Chromium and local HTTPS support; absence of those tools must not be represented as a pass. The cloud browser fixture used its documented sandbox adjustment only for the isolated fixture.

With `ANDROID_HOME` pointing to the same SDK, `python docs/jarvis/TEST_EVIDENCE/JARVIS_4_1/verify_artifact.py` checks the built APK against the retained previous Phoenix and original production APKs. It writes these SDK inspection outputs. It does **not** attempt installation or establish compatibility with an unmeasured handset certificate.

## Retained earlier failures

`verification-history.json` describes failed and interrupted attempts. `history/` preserves their logs rather than replacing them with the final pass. Raw logs retain terminal whitespace, including Gradle's trailing spaces; source/document whitespace checks exclude those unchanged captures. The initial concurrent companion suite had 35 passes and one real `PAGE_LOAD_TIMEOUT`. An instrumented idle browser run and then the complete serial suite passed without changing companion production code. Resource/timing contention is a plausible contributor, not a proven root cause.

Earlier Android attempts include corrected compilation errors, one missing Persian intent idiom and two lint errors. Interrupted gates are not counted as passes. The manually recorded Google compile-error note is labeled as such, rather than presented as a complete raw log.

## Limits and integrity

Google dependency compatibility is recorded separately from account authorization. The public Perplexity catalog request was network-blocked; it is not a verified live catalog or inference result. Mocked HTTP tests demonstrate request, parsing and failure handling, rather than real account access.

The APK retains all 134 previous native/asset files, including 14 native libraries, byte for byte. Its retained Vosk library still has 4 KB ELF page alignment. No current Google Play large-page or production-release readiness is claimed.

`SHA256SUMS` covers the evidence files in this directory, excluding the checksum file itself. It detects subsequent changes; it is not an independent signature or a device-test attestation. See the [engineering report](../../JARVIS_4_1_REPORT.md), [Google setup](../../GOOGLE_SETUP.md) and [unrun device acceptance procedures](../../JARVIS_4_1_DEVICE_ACCEPTANCE.md).
