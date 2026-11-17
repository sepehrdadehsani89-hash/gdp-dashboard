# Stabilization verification evidence

`final-summary.json` records the final result and `jvm-cases.csv` maps all 1,297 actual JUnit cases to PASS/FAIL/SKIP. Original XML is in `jvm/`. Full verification run 3 passed; previous attempts and their actual failures/cancellations are retained in `verification-attempts.md` and their logs. Android tests compiled, but zero instrumented/device tests ran.

`artifact-verification.json` contains 20 passing APK checks; `apk-signature.txt` records the retained preview certificate. `final-native-16k.json` separately records the Vosk alignment blocker, rather than hiding it as Play-ready compatibility. `production-signing-guard.txt` is an actual failed signing precondition, not a successful AAB build.

Raw tool-output `.txt` files are byte-preserved and may contain the tools' own trailing spaces. Source/authored-document whitespace checks exclude these raw logs. The derived CSV uses LF line endings; its test values are unchanged. No physical-device/provider results are fabricated, and no credentials or private user export were supplied for this run.
