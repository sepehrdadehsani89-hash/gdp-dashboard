# Jarvis 11.5 — voice and core repair

[**Download the signed ARM64 APK**](Jarvis-11.5.0-arm64-reinstall.apk) · [Changes, owner training and phone acceptance test](../../docs/voice-11.5/REPORT.md) · [Verification](VERIFICATION.md) · [Checksum](SHA256SUMS)

Repairs wake/greeting/capture handoff, duplicate greetings, empty decoder endpoints and false ASR error feedback. The default greeting is “Hello sir, how can I help you?”, followed by actual command capture after playback completion. Native time/date use the real local device clock; existing battery and installed-app handlers remain. General questions use the configured backend or installed local model and report sanitized actual failures.

Adds an optional locally trained owner-only spoken-command filter using the existing recorder and encrypted templates. Open **Settings → Rebirth → Set up owner voice · 20-second local recording**, review/download the optional model, consent and train, then test/calibrate with new phrases. Unknown/unavailable matches run no command. The public greeting is not identification. Voice similarity is not Android authentication and cannot guarantee rejection of similar voices or recordings; existing protected-action authentication, typing and Stop remain.

The original core design gains visible idle rotation/breathing, actual microphone/playback response and execution/error motion. Target 60 FPS is adaptive to device/display, heat, low RAM and power. Reduced motion and system animation settings remain respected. Existing charging/ETA/reminders, widgets, panels, local AI, configured Fish English/Spanish voices, privacy and conversation controls remain. Gmail stays removed.

Package `com.jarvis.next` · version `11.5.0-reinstall` · code 21 · Android 10+ · ARM64 · 84,609,461 bytes. Certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` matches 11.4's retained newer signing line. Install over the matching build. If Android rejects the signature, keep the installed app and data; do not uninstall or clear data to force it.

**474 JVM tests passed**, zero failures/errors/skips. Lint: zero errors/fatals, 44 warnings. Android test sources compiled, but no Android runtime cases ran. An actual speaker-model host test used synthetic fixtures only. Physical Redmi 9 microphone/owner accuracy, Fish playback, lifecycle, device actions and actual FPS need hardware verification. This development update does not claim the whole cross-platform master backlog or commercial readiness.

APK SHA-256: `9befe589da0175b4e6c894d6319a36acc163c00cd12ff19f8b22027799145de8`.
