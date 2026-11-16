# Executed verification — source repair 36f2c27

| Check | Actual result | Evidence |
|---|---|---|
| Before-change baseline compile/selected JVM suite | PASS, 201 cases, zero failures/skips | TEST_EVIDENCE/baseline-jvm/ and baseline-tests.log |
| New playback-drain suite | PASS, 20 cases, zero failures/skips | TEST_EVIDENCE/drain-jvm/ and VOICE_OUTPUT_UNIT_RESULTS.csv |
| After-change complete JVM regression | 639 discovered: 638 PASS, 0 FAIL, 1 optional fixture skipped | TEST_EVIDENCE/regression-jvm/ and JVM_REGRESSION_RESULTS.csv |
| Android debug lint | PASS: 0 errors/fatals; 48 warnings, 1 information | TEST_EVIDENCE/lint-results-debug.xml |
| Existing desktop companion | PASS, 36 cases, zero skips | TEST_EVIDENCE/companion-36-tests.log |
| New Android output acceptance | BLOCKED, 20 defined cases | TEST_EVIDENCE/VOICE_OUTPUT_DEVICE_PLAN.csv |
| Original Android animation acceptance | BLOCKED, 20 defined cases | TEST_EVIDENCE/ANIMATION_20_CASES.csv |

Full regression includes the focused and baseline cases; do not add those overlapping counts as independent tests. The one skipped case is PrivateMemoryBackupTest.validatesUserExportPrivatelyWhenProvided, whose private fixture was not supplied. It is NOT_RUN, not PASS. No request to share private exports is necessary for this repair.

The companion suite includes real local TLS pairing/revocation and a real controlled Linux Chromium page; most policy cases use explicit adapters/fixtures. It does not test Windows/macOS, real Google search, owner desktop accounts or Android-to-desktop voice. No acoustic wake rate, recognition WER, output quality, handset FPS, battery drain, device permissions or physical lifecycle result is claimed.

The new unit cases test the real helper used by OfflineVoiceManager, with controlled playback cursors, virtual monotonic time and cancellation. They do not instantiate AudioTrack or run native sherpa models. Static source proof of the prior defect: the old wait loop exited after its wall-clock deadline and invoked onDone for any current request without checking the final playback cursor. Hardware reproduction remains pending.

Verification commands (source the isolated toolchain env first):

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests 'com.jarvis.assistant.voice.*' --tests 'com.jarvis.assistant.sentinel.*' --tests 'com.jarvis.assistant.power.*' --no-daemon --max-workers=1 -Dorg.gradle.parallel=false '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process
./gradlew :app:testDebugUnitTest --tests 'com.jarvis.assistant.voice.OfflinePlaybackDrainTest' --no-daemon --max-workers=1 -Dorg.gradle.parallel=false '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process
./gradlew :app:testDebugUnitTest :app:lintDebug --no-daemon --max-workers=1 -Dorg.gradle.parallel=false '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process
/workspace/work/jarvis-milestone-voice/companion-venv/bin/python -m unittest discover -s companion/tests -v
```

The up-to-100 meaningful checks per significant feature target is open. Current repair has 20 executed policy cases and 20 blocked device scenarios; no artificial repetitions or production/device certification is claimed. Case evidence includes purpose/expected behavior, observed outcome, environment, repair commit and retest status.
