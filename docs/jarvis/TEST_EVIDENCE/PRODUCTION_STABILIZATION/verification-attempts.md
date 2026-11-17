# Verification attempts

- Focused audio/motion runs 1 and 2 passed 110 and 174 tests.
- Settings/contact/setup attempt 1 failed Kotlin compilation: a misplaced String.size check, unsupported regex escapes and a copy call on a non-data voice config. Errors are retained verbatim in its log; no tests were claimed for that run.
- Settings/contact/setup attempt 2 passed 207 tests after correction.
- Full verification attempt 1 was deliberately interrupted with SIGINT (exit 130), before JVM execution/lint/packaging completed, to move Stop Speaking from scrollable content into the fixed control bar and correct the microphone button's playback behavior. This is not a passing verification run. Main compilation completed; the cancelled run is retained.
- Full verification attempt 2 passed compilation, 1,291 JVM cases (1,290 passed / one optional fixture skipped), lint (zero errors / 55 warnings / one informational item), APK assembly and Android-test compilation. Final review then tightened canonical contact-alias ambiguity handling and corrected disable-private-voice disclosure text; these changes are verified again in attempt 3.

- Full verification attempt 3 verifies the final reviewed source, including six additional contact alias ambiguity regressions.

- APK inspection helper attempt 1 failed because its previous-APK comparison path used the new filename in the old release folder. The actual FileNotFoundException is retained in artifact-helper-1-error.txt. The comparison path was corrected to the unchanged published Brain Manager APK; the application itself was not changed or rebuilt for this correction.

- Full verification attempt 3 PASSED in 9m 35s: 1,297 JVM cases, zero failures/errors, one optional fixture skipped; zero lint errors, 55 warnings and one informational item; APK assembled; Android test sources compiled, zero device tests executed. Corrected artifact inspection PASSED 20 checks. Production signing guard ran once and FAILED with the exact missing-directory message, retained in production-signing-guard.txt; no production AAB or retry was attempted.
