# Jarvis 11.3 — speech and animation repair

[Download the signed ARM64 APK](Jarvis-11.3.0-arm64-reinstall.apk) · [Phone steps](../../docs/rebirth-11.3/VOICE-REPAIR.md) · [Verification](VERIFICATION.md) · [SHA-256](SHA256SUMS)

Stalled Vosk partials are finalized through the decoder and checked before dispatch. Completed questions cannot be held open indefinitely by background RMS. Unclear speech receives one fixed spoken retry prompt; the Android recognizer fallback also has a bounded final-result wait. Text-only/private native results receive a public spoken acknowledgement without uploading the private result. Existing private-speech consent and exclusive configured Fish English/Spanish voices remain.

The reactor uses a visible lifecycle/display-frame clock, retains its rotation across phase changes, and resumes without counting hidden time. Active states move more visibly while reduced-motion, battery, speed and intensity preferences remain respected. Additional screen-off phrases reach the existing cancellable spoken lock countdown. Locking still needs Android's lock-only administrator approval.

Charging, widgets from installed apps, all Jarvis panels, captions/live chat cleanup, approved local notes, owner enrollment, aliases, memories and explicit file grants remain. Gmail stays removed.

Package `com.jarvis.next`; code 19 / `11.3.0-reinstall`; Android 10+, ARM64. Size 84,397,409 bytes. SHA-256 `b88dea0ec09abab410ca87fdf47b45e4e211e2e6c527802fdc00a6c23bfc4878`. Retained certificate `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. Install over the matching newer signing line; no replacement key, uninstall or data clearing is required. If Android rejects the signature, preserve the installed app/data.

**376 JVM tests passed**; lint zero errors/fatals with 43 warnings. Android test sources compiled, but no Android runtime cases or physical Redmi 9/Fish acoustic/lock tests ran. This is a development build; phone verification remains necessary.
