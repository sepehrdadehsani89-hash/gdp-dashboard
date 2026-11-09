# Current development build: Jarvis 11.4 — OVERCHARGE

[Download the signed ARM64 APK](overcharge-11.4/Jarvis-11.4.0-arm64-reinstall.apk) · [Implementation and phone setup](../docs/overcharge-11.4/IMPLEMENTATION.md) · [Verification](overcharge-11.4/VERIFICATION.md).

Adds supported charging ETA, critical alert retry/completion tracking, full-charge reminder controls and the wallpaper charging strip. Preserves 11.3 speech fixes and adds narrow playback interruption through the already-enabled wake service. Package/signing identity match the newer 11.2/11.3 builds. Install over the matching build without clearing data. Real handset behavior still requires verification.

Previous: [11.3](rebirth-11.3/Jarvis-11.3.0-arm64-reinstall.apk) · [11.2 beta3](rebirth-11.2-beta3/Jarvis-11.2.0-beta3-arm64-reinstall.apk).

# Historical release: Jarvis 4.0 — Neural Core

[Download the signed ARM64 APK](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/1b854d8584be588232be13df723c1dc3f981579d/downloads/neural/Jarvis-4.0.0-arm64.apk) · [Setup and implementation report](../JARVIS_NEURAL_CORE.md) · [Verification](../docs/neural/VERIFICATION.md). Package `com.jarvis.next` updates Jarvis Next/Jarvis 2.0/3.0 with the retained key. Original Jarvis remains separate; no uninstall is needed.

[Historical Jarvis 3.0 Sentinel](../JARVIS_SENTINEL.md).

The following files describe the historical original Jarvis 0.1.3 release.

# Jarvis for your Android phone

Development preview 0.1.3 · Android 10+ · ARM64 · intended first device: Redmi 9.

Direct Android download:

[Download Jarvis APK](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/cbb981303aa0e15c005384cd0b321e697c10fbe7/downloads/Jarvis-0.1.3-arm64.apk)

1. Open the link on Android and download the APK. Android asks you to approve installation; it cannot be installed silently.
2. Open Jarvis and choose English, Español, or فارسی. No API key is required.
3. Download the selected language's speech pack in Settings (~40–55 MB). Run Voice Check; download its offline voice (~65 MB) if needed.
4. For general questions without a key, download the optional local brain (~400 MB). On a Redmi 9 it can be slow and its Persian quality is limited. News/YouTube/memory shortcuts work without this brain.
5. Enable microphone listening from the visible app when ready. Use the selected wake phrase: Hey Jarvis / Hola Jarvis / سلام جارویس. Only the selected listening language is active at a time.

You decide which permissions to grant. Files/photos are selected explicitly; Android's private-app sandboxes remain in place. Background listening is best effort and does not restart silently after boot/force-stop.

SHA-256 and build details: [SHA256SUMS](SHA256SUMS), [build-info.json](build-info.json). Source and licensing accompany this download in the same branch. See [verification](../docs/VERIFICATION.md) for tested versus unverified capabilities.

[0.1.3 phone changes and setup](../docs/0.1.3_PHONE_FIXES.md) · [Gemini model selection and connection test](../docs/GEMINI_SETUP.md) · [Step-by-step phone setup](../docs/PHONE_SETUP.md)

![Scan on your Android phone](Jarvis-QR.png)
