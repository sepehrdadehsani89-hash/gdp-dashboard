# Baseline before implementation

Source checkpoint `edf4464971926e30004885d1e118eed511b4d154` (2.11.7, code 24). Kotlin/Compose app `com.jarvis.next`, Android 10/API29 minimum, target/compile35, AGP8.7.3, Kotlin2.0.21, Gradle8.9, ARM64 native Vosk/local brain. Retained newer certificate SHA256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. No signing replacement or data reset authorized.

Existing microphone foreground service, exclusive ManagedMicrophone leases, VoiceSessionStateMachine, wake greeting gate, Vosk N-best final recognition with absent-score handling, typed errors and follow-up coordination. Fish Audio has independent saved EN/ES profiles and explicit private-speech consent. Fixed-public greeting cache; no conversation audio cache. Local time/date/battery and app handlers exist; local arithmetic/identity need audit. Optional local models and configured cloud providers already exist; no gateway required for local handlers.

Existing core uses Compose HUD/Canvas plus WallpaperService/NativeRenderer, actual microphone and decoded-playback meters, BatteryStateRepository and adaptive frame policy. Original layout/theme must remain. Existing paired private-LAN HTTPS companion uses QR/manual approval, pinned certificate, bounded typed Chrome actions and verified HMAC receipts; Windows/macOS native execution unverified. Existing check-ins, battery calls, owner voice profiles, pickup sensing, widgets, private content, summaries and licensed assets remain. Gmail stays removed.

Previous verified release: 548 JVM tests; zero lint errors, 48 warnings; instrumentation compiled only. No physical Android is attached in prior final ADB list. Actual speech latency/noise accuracy, handset FPS, thermal/battery impact and native Windows/macOS behavior are not measured. No Video6158 or source recording was found among attachments/repository; described observations are targets only.

Reference UI source freeze is the above Git commit and its 1610-input manifest in downloads/v2-2.11.7. No screenshot is available from a physical Android surface; source identity can be compared, not a fabricated visual baseline.
