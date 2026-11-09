# J.A.R.V.I.S. 11.4 — OVERCHARGE

[**Download the signed ARM64 APK**](Jarvis-11.4.0-arm64-reinstall.apk) · [Phone setup and implementation report](../../docs/overcharge-11.4/IMPLEMENTATION.md) · [Verification](VERIFICATION.md) · [Checksum](SHA256SUMS)

Adds a shared Android battery observer, supported approximate charging ETA, actual percentage/temperature/voltage/current information, charging-start speech, explicit 3%/2%/1% handling, completed-playback acknowledgement and bounded critical retries. Verified 100% receives one full announcement, with configurable repeats, snooze, dismissal until unplugged, quiet hours and optional progress/disconnection speech. Native charging questions use actual readings without loading the local model.

The actual Sentinel wallpaper has a charging strip with a responsive clock/caption layout. The green energy layer has an independent smooth frame-time clock and keeps its phase while listening or speaking. Existing charging colors and all earlier panels remain. Real CPU draw/post/frame-start timing and alert-audio latency are instrumented without inventing handset measurements.

Preserves 11.3 speech finalization and adds narrow final-recognized wait/stop controls through the already-enabled wake service. Quoted echo, ordinary device actions and obsolete callbacks are rejected in playback-control mode. Actual acoustic behavior still needs phone verification; the visible Stop control remains. English/Spanish continue through the configured Fish voices with existing private-speech consent. No replacement key, private API key or substitute voice is introduced.

Open **Settings → Rebirth → Power Core** to review voice alerts, the existing optional call-state permission, quiet hours and reminder settings. Use Output voice preview with your existing Fish account. Android still controls permissions, audio focus, Doze timing and screen locking.

Package `com.jarvis.next`; code 20 / `11.4.0-reinstall`; Android 10+, ARM64. APK 84,509,889 bytes. SHA-256 `ae3a9297fbef1111cd7798e39ec8f3eb7dee4a8b2367167a7b007eec7a6305f0`. Certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. This updates the matching newer signing line. Preserve the installed app/data if Android rejects a signature; do not uninstall or clear data to force installation.

**429 JVM tests passed**, zero failures/errors/skips; lint zero errors/fatals and 44 warnings. Android test sources compiled; no Android runtime cases ran. Actual Redmi 9 charging, Fish sound, microphone/echo, OEM scheduling, device lock and visual performance are not certified here. Gmail remains removed as previously requested. This is a development build, not a claim that every earlier aspirational feature or the full commercial master directive is complete.
