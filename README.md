# Jarvis for Android

Current development milestone: [**Jarvis2.11.8 — signed ARM64 APK**](downloads/polish-2.11.8/Jarvis-2.11.8-arm64-update.apk). [Combined-prompt report](docs/finish-polish/REPORT.md) · [Phone guide](docs/finish-polish/Phone_Guide.md) · [Full scope/limits](docs/finish-polish/Feature_Matrix.md) · [Verification](downloads/polish-2.11.8/VERIFICATION.md).

Integrates System Doctor, opt-in taught recipes, optional source-backed Future Actions and verified paired-computer Mission Control under **Settings → Rebirth → Intelligence & Systems**. Adds local identity/time-zone/basic math, “Jarvis, are you up?”, optional wake phrases, blue speaking and true-charging accents. Original core geometry, wallpaper, theme and prior features remain; no separate fifth Living Core module.

619 JVM tests and36 companion tests passed; zero lint errors/fatals. Android instrumentation compiled only; no phone/native Windows/macOS tests or supplied Video6158. This is a scoped development release; broader desktop app/file/workspace actions, semantic learning and full hardware acceptance remain unfinished. General conversation still needs the configured backend or installed local model. [Companion source ZIP](downloads/polish-2.11.8/JarvisCompanion-source-2.11.8.zip) · [setup](companion/README.md).

Package `com.jarvis.next`, code25, retained newer signer unchanged. Update a matching newer installation without clearing data; preserve installed app/data if its certificate is rejected. No replacement Android signing key was generated. Learning/prediction are opt-in and new stores additive. Gmail remains removed.

Previous: [2.11.7](downloads/v2-2.11.7/Jarvis-2.11.7-arm64-update.apk) · [2.11.6](downloads/v2-2.11.6/Jarvis-2.11.6-arm64-update.apk) · [11.5](downloads/voice-11.5/Jarvis-11.5.0-arm64-reinstall.apk) · [11.4](downloads/overcharge-11.4/Jarvis-11.4.0-arm64-reinstall.apk). Historical artifacts remain preserved.

## Historical original-signature 7.0 release

The 7.0 download and instructions below concern the original signing line, not the newer reinstall beta.

Jarvis 7.0 — Infinity adds an explicit optional near-1 GB local-model installer, removes RAM-based refusals, and adds on-demand Wi-Fi/Bluetooth/battery diagnostics, approved HTTP speed testing and saved websites. The HUD places live captions under the reactor and weather beside the clock, with purple listening, cyan speaking and a separate green charging ring. Neural Core, Sentinel, live controls, folders, memory and ten-second voice follow-ups remain. The [implementation report](JARVIS_INFINITY.md) identifies unavailable and untested parts of the larger Infinity specification.

**Existing app update:** this is the renamed **Jarvis Next** app, package `com.jarvis.next`. The signed 7.0.0 APK updates that installation, including Jarvis 2.0/3.0/4.0, with the same retained key. Keep your original Jarvis installed and untouched. The unavailable original Jarvis key and unfinished 0.1.4 files were not recovered. `main` and `jarvis-android-download` are unchanged.

[**Download Jarvis 7.0.0 — signed ARM64 APK**](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/c31e649f60110351cdaf4ae7c9893835bcbd2e0b/downloads/infinity/Jarvis-7.0.0-arm64.apk)

[Infinity guide, limits and recovery](JARVIS_INFINITY.md) · [Build identity/checksum and verification](downloads/infinity/build-info.json) · [Neural Core architecture](JARVIS_NEURAL_CORE.md) · [Public signing certificate](downloads/jarvis-next-signing-certificate.pem)

## Phone setup

1. Install the signed release APK. Its launcher name is **Jarvis** and its icon is a cyan/purple reactor. The package remains `com.jarvis.next`; the original Jarvis app is separate. No uninstall is needed.
2. Choose a language and complete onboarding. Microphone, notification and optional access permissions remain under your control. Download the matching speech model for offline wake listening: **Hey Jarvis**, **Oye Jarvis**, or **هی جارویس**.
3. Open **Settings → Neural Core** to inspect model sizes/licenses, index approved documents, configure aliases, manage history, and opt into briefings. Open **Settings → Sentinel · wallpaper & sound → Preview / apply Jarvis wallpaper** and confirm in Android’s picker. This does not enable the microphone. Configure clock, city/approximate weather location, voice, reduced motion, sound and auto-dismiss in Settings. Codex protection uses Android's official biometric/device-credential prompt for a pending action and requires a device screen lock.
4. **Settings → Neural Core → Install optional near-1 GB brain** inspects the real Qwen2.5 1.5B Q4_K_M weights before you approve downloading. Exact bytes, license, revision, SHA-256 and additional storage are shown first. Lite (~400 MB) is retained. Installation and activation are separate; **Use selected installed profile** activates on-device AI and disables cloud voice. RAM-based blocking and low-memory cancellation are removed; memory estimates are informational. Actual model/Redmi 9 benchmarks could not be run in this cloud. Local AI does not establish Play Protect approval or change Google's HTTP 429 quota. Keep Play Protect enabled.
5. **Folders** connects a user-selected directory read-only. Attach documents/photos to a question. **Share selected photos…** opens Android's chooser; sending stays manual. `whatsapp draft +593991234567: Hello Sofia` opens a reviewed recipient draft.
6. For live controls, enable Screen access, explicitly start local listening and open the target app. Say `tap Search` or `type Hello Sofia`, then confirm by voice on the same screen. Sending, payment, destructive and permission controls stay manual. `open WhatsApp` directly opens an unambiguous installed app.

## Build and signing recovery

Use the [pinned toolchain](docs/COMPATIBILITY.md). Set `JARVIS_NEXT_SIGNING_DIR` to the retained private directory outside the checkout, containing `jarvis-next.p12` and `store-password`. Do not generate a new key for an installed `com.jarvis.next` app. The encrypted backup and separate unlock file must be copied privately outside this cloud workspace; see [recovery instructions](JARVIS_2.md#signing-and-private-recovery).

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
```

Debug APKs are for dedicated emulator tests. The phone download is signed release ARM64, Android 10+. [Local model](docs/LOCAL_MODEL.md) · [Licenses](THIRD_PARTY_NOTICES.md).

Historical artifacts and verification remain available: [Jarvis 4.0 Neural Core](JARVIS_NEURAL_CORE.md), [its verification](docs/neural/VERIFICATION.md), [Jarvis 3.0 Sentinel](JARVIS_SENTINEL.md), [its verification](docs/JARVIS_SENTINEL_VERIFICATION.md), [Jarvis 2.0](JARVIS_2.md), [its verification](docs/JARVIS_2_VERIFICATION.md), [Jarvis Next 0.1.0](docs/JARVIS_NEXT.md), [its verification](docs/JARVIS_NEXT_VERIFICATION.md), and original `Jarvis-0.1.x` APKs. They describe their respective builds.
