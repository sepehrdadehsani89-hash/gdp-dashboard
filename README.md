# Jarvis for Android

Jarvis 3.0 — Sentinel adds a native live wallpaper, coordinated reactor activation, 26 original sound cues, real audio meters and a hard 10-second command window. Say Hey Jarvis once, then continue without another wake phrase during each fresh follow-up window after its spoken answer. Existing HUD, local AI, connected folders, attachments, memory and live controls remain.

**Separate installation:** this is the renamed **Jarvis Next** app, package `com.jarvis.next`. The signed 3.0.0 APK updates Jarvis Next 0.1.0 and Jarvis 2.0 with the same key and preserves its data. Keep your original Jarvis installed and untouched. The unavailable original Jarvis key and unfinished 0.1.4 files were not recovered. `main` and `jarvis-android-download` are unchanged.

[**Download Jarvis 3.0.0 — signed ARM64 APK**](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/478c7e43a021689fe05d7be11e34b751a38d1bbd/downloads/sentinel/Jarvis-3.0.0-arm64.apk)

[Sentinel feature/setup guide](JARVIS_SENTINEL.md) · [Verification and limits](docs/JARVIS_SENTINEL_VERIFICATION.md) · [Build identity/checksum](downloads/sentinel/build-info.json) · [Public signing certificate](downloads/jarvis-next-signing-certificate.pem)

## Phone setup

1. Install the signed release APK. Its launcher name is **Jarvis** and its icon is a cyan/purple reactor. The package remains `com.jarvis.next`; the original Jarvis app is separate. No uninstall is needed.
2. Choose a language and complete onboarding. Microphone, notification and optional access permissions remain under your control. Download the matching speech model for offline wake listening: **Hey Jarvis**, **Oye Jarvis**, or **هی جارویس**.
3. Open **Settings → Sentinel · wallpaper & sound → Preview / apply Jarvis wallpaper** and confirm in Android’s picker. This does not enable the microphone. Configure clock, city/approximate weather location, voice, reduced motion, sound and auto-dismiss in Settings. Codex protection uses Android's official biometric/device-credential prompt for a pending action and requires a device screen lock.
4. Tap **Use free on-device AI** and download the optional ~400 MB Qwen model for general answers without an API key. Local inference has small-model quality/performance limits. Renaming the app does not establish Play Protect approval or change Google's HTTP 429 quota. Keep Play Protect enabled.
5. **Folders** connects a user-selected directory read-only. Attach documents/photos to a question. **Share selected photos…** opens Android's chooser; sending stays manual. `whatsapp draft +593991234567: Hello Sofia` opens a reviewed recipient draft.
6. For live controls, enable Screen access, explicitly start local listening and open the target app. Say `tap Search` or `type Hello Sofia`, then confirm by voice on the same screen. Sending, payment, destructive and permission controls stay manual. `open WhatsApp` directly opens an unambiguous installed app.

## Build and signing recovery

Use the [pinned toolchain](docs/COMPATIBILITY.md). Set `JARVIS_NEXT_SIGNING_DIR` to the retained private directory outside the checkout, containing `jarvis-next.p12` and `store-password`. Do not generate a new key for an installed `com.jarvis.next` app. The encrypted backup and separate unlock file must be copied privately outside this cloud workspace; see [recovery instructions](JARVIS_2.md#signing-and-private-recovery).

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
```

Debug APKs are for dedicated emulator tests. The phone download is signed release ARM64, Android 10+. [Local model](docs/LOCAL_MODEL.md) · [Licenses](THIRD_PARTY_NOTICES.md).

Historical artifacts and verification remain available: [Jarvis 2.0](JARVIS_2.md), [its verification](docs/JARVIS_2_VERIFICATION.md), [Jarvis Next 0.1.0](docs/JARVIS_NEXT.md), [its verification](docs/JARVIS_NEXT_VERIFICATION.md), and original `Jarvis-0.1.x` APKs. They describe their respective builds.
