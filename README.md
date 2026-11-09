# Jarvis for Android

Current development build: [Jarvis 11.4 — OVERCHARGE · signed ARM64 APK](downloads/overcharge-11.4/Jarvis-11.4.0-arm64-reinstall.apk). [Implementation and phone setup](docs/overcharge-11.4/IMPLEMENTATION.md) · [Verification](downloads/overcharge-11.4/VERIFICATION.md) · [Build/checksum](downloads/overcharge-11.4/build-info.json).

Adds shared Android charging data and approximate ETA, actual charging announcements, explicit 3%/2%/1% warnings with completed-playback acknowledgement and bounded retry, plus configurable full-charge reminders, snooze, dismissal and quiet hours. The real Sentinel wallpaper gains a charging strip and independent smooth green animation. Native charging questions use supported measurements without invoking a generative model.

Preserves 11.3 speech finalization and adds narrow wait/stop playback controls through an already-enabled wake service, time-aware confirmed-owner greetings, and neutral unknown-speaker greetings. Existing widgets, all panels, local AI, Fish English/Spanish, private-voice consent, captions/chat cleanup, approved local notes, owner profiles, app aliases, files and screen-lock safeguards remain. Gmail stays removed as previously requested. Real Redmi 9/Fish/charger/OEM performance remains unverified here; see the implementation status and release checks.

**Package and retained newer signing certificate are unchanged. Update matching newer builds in place. Preserve the installed app and data if Android rejects the signature; do not uninstall or clear data to force the update. No replacement key was generated.**

Previous: [11.3 speech repair](downloads/rebirth-11.3/Jarvis-11.3.0-arm64-reinstall.apk) · [11.2 beta3](downloads/rebirth-11.2-beta3/Jarvis-11.2.0-beta3-arm64-reinstall.apk) · [lock and widget setup](docs/rebirth-11.2/LOCK-AND-WIDGETS.md) · [11.2 beta2](downloads/rebirth-11.2-beta2/Jarvis-11.2.0-beta2-arm64-reinstall.apk) · [11.2 beta1](downloads/rebirth-11.2/Jarvis-11.2.0-beta1-arm64-reinstall.apk) · [11.1 Phantom](downloads/phantom-11.1/Jarvis-11.1.0-beta1-arm64-reinstall.apk). Historical artifacts and their limits remain preserved.

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
