# Jarvis 2.11.7 — spoken calls, language and pickup

[**Download the signed ARM64 APK**](Jarvis-2.11.7-arm64-update.apk) · [Changes and phone test steps](../../docs/voice-2.11.7/REPORT.md) · [Verification](VERIFICATION.md) · [Checksum](SHA256SUMS).

Corrects a Vosk format mismatch: alternative-result words omit per-word confidence, and the old gate rejected those valid finals. Missing scores now differ from invalid supplied scores, without treating whole-hypothesis likelihood as a probability. This restores decoded commands and questions reaching their handlers. It also corrects low-score exact “call me now” rejection. Exact public wake/call-start/language controls survive low word confidence; empty/partial transcripts, contact calls, compounds and protected actions keep their safeguards. Answer remains explicit. The existing voice pipeline and core design remain.

Say **“change to Spanish”** to select Spanish mode and its saved Spanish Fish voice; **“change to English”** restores the English profile. The selected mode remains active for subsequent answers; Voice health can restore automatic reply-language detection. The old decoder/microphone closes before the other language starts, paused until acknowledgement finishes. Missing speech packs and unavailable voices are reported truthfully. No credentials or saved voice IDs are replaced and no disabled voice is silently enabled.

Adds **six pickup greetings in each language**. While normal wake listening is enabled, an unlocked idle phone can greet after an uncovered stationary-to-lift-and-tilt gesture. Proximity, quiet hours, Silent/DND, calls, music, active requests and a five-minute cooldown suppress interruptions. Disable it in **Settings → Rebirth → Voice health · fix recognition, language & pickup greetings**. That screen also shows real recognition/model/voice status, a balanced-settings repair, a local call test and explicit selected speech-pack download.

Package `com.jarvis.next` · version **2.11.7** · code **24** · Android 10+ · ARM64 · 85,347,645 bytes. Retained newer certificate unchanged. Install over a matching newer build without clearing data. If Android rejects a signature, preserve the installed app/data; do not uninstall to force it.

**548 JVM tests passed**, zero failures/errors/skips. Lint: zero errors/fatals, 48 warnings. Android instrumentation compiled only; **no physical phone tests ran**. Actual Redmi 9 recognition, room noise/accent, Fish playback, language handoff, pickup/proximity reliability, lock-screen behavior and latency need device acceptance. General answers require a working configured backend or installed local model. Existing features and the staged master backlog remain.

APK SHA-256: `a27ff31e9f569658fdfe079cb8ac0deec4c9f0515dd3dbc054f303a9e90ecf0d`.
