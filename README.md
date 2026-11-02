# Jarvis for Android

Jarvis 4.0 — Neural Core adds approved offline document retrieval, app-name matching and aliases, bounded tool planning, weather follow-ups, opt-in daily briefings, history controls and on-phone model evaluation. Sentinel wallpaper/HUD, live controls, folders, attachments, memory and ten-second voice follow-ups remain.

**Separate installation:** this is the renamed **Jarvis Next** app, package `com.jarvis.next`. The signed 4.0.0 APK updates Jarvis Next 0.1.0 and Jarvis 2.0/3.0 with the same key and preserves its data. Keep your original Jarvis installed and untouched. The unavailable original Jarvis key and unfinished 0.1.4 files were not recovered. `main` and `jarvis-android-download` are unchanged.

[**Download Jarvis 4.0.0 — signed ARM64 APK**](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/jarvis-next/downloads/neural/Jarvis-4.0.0-arm64.apk)

[Neural Core guide/report](JARVIS_NEURAL_CORE.md) · [Verification and limits](docs/neural/VERIFICATION.md) · [Build identity/checksum](downloads/neural/build-info.json) · [Public signing certificate](downloads/jarvis-next-signing-certificate.pem)

## Phone setup

1. Install the signed release APK. Its launcher name is **Jarvis** and its icon is a cyan/purple reactor. The package remains `com.jarvis.next`; the original Jarvis app is separate. No uninstall is needed.
2. Choose a language and complete onboarding. Microphone, notification and optional access permissions remain under your control. Download the matching speech model for offline wake listening: **Hey Jarvis**, **Oye Jarvis**, or **هی جارویس**.
3. Open **Settings → Neural Core** to inspect model sizes/licenses, index approved documents, configure aliases, manage history, and opt into briefings. Open **Settings → Sentinel · wallpaper & sound → Preview / apply Jarvis wallpaper** and confirm in Android’s picker. This does not enable the microphone. Configure clock, city/approximate weather location, voice, reduced motion, sound and auto-dismiss in Settings. Codex protection uses Android's official biometric/device-credential prompt for a pending action and requires a device screen lock.
4. **Use free on-device AI** selects local processing. Lite retains the optional ~400 MB Qwen model; Neural Core verifies actual bytes/license before a download. Balanced is an experimental optional profile with a higher RAM gate, and Advanced remains unavailable on unverified hardware. Actual model/Redmi 9 benchmarks could not be run in this cloud; the phone evaluation and limits are documented. Renaming the app does not establish Play Protect approval or change Google's HTTP 429 quota. Keep Play Protect enabled.
5. **Folders** connects a user-selected directory read-only. Attach documents/photos to a question. **Share selected photos…** opens Android's chooser; sending stays manual. `whatsapp draft +593991234567: Hello Sofia` opens a reviewed recipient draft.
6. For live controls, enable Screen access, explicitly start local listening and open the target app. Say `tap Search` or `type Hello Sofia`, then confirm by voice on the same screen. Sending, payment, destructive and permission controls stay manual. `open WhatsApp` directly opens an unambiguous installed app.

## Build and signing recovery

Use the [pinned toolchain](docs/COMPATIBILITY.md). Set `JARVIS_NEXT_SIGNING_DIR` to the retained private directory outside the checkout, containing `jarvis-next.p12` and `store-password`. Do not generate a new key for an installed `com.jarvis.next` app. The encrypted backup and separate unlock file must be copied privately outside this cloud workspace; see [recovery instructions](JARVIS_2.md#signing-and-private-recovery).

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
```

Debug APKs are for dedicated emulator tests. The phone download is signed release ARM64, Android 10+. [Local model](docs/LOCAL_MODEL.md) · [Licenses](THIRD_PARTY_NOTICES.md).

Historical artifacts and verification remain available: [Jarvis 3.0 Sentinel](JARVIS_SENTINEL.md), [its verification](docs/JARVIS_SENTINEL_VERIFICATION.md), [Jarvis 2.0](JARVIS_2.md), [its verification](docs/JARVIS_2_VERIFICATION.md), [Jarvis Next 0.1.0](docs/JARVIS_NEXT.md), [its verification](docs/JARVIS_NEXT_VERIFICATION.md), and original `Jarvis-0.1.x` APKs. They describe their respective builds.
