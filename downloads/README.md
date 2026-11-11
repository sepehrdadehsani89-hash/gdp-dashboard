# Current development update: Jarvis 2.11.7

[**Download the signed ARM64 APK**](v2-2.11.7/Jarvis-2.11.7-arm64-update.apk) · [Changes and phone setup](../docs/voice-2.11.7/REPORT.md) · [Verification](v2-2.11.7/VERIFICATION.md).

Corrects Vosk alternative-result parsing and exact spoken AI-call starts; adds English/Spanish saved-voice switching, six guarded pickup greetings per language and Voice health settings. Existing capabilities and retained newer signing identity remain. 548 JVM tests passed; zero lint errors/fatals. Actual phone audio, language handoff and sensor behavior remain unverified.

# Previous development build: Jarvis 2.11.6

[**Download the signed ARM64 APK**](v2-2.11.6/Jarvis-2.11.6-arm64-update.apk) · [Phone guide](../docs/v2/PHONE-GUIDE.md) · [Changes and limits](../docs/v2/REPORT.md) · [Verification](v2-2.11.6/VERIFICATION.md).

Ringing in-app AI calls with Answer/End, dated check-ins, 20% battery conversation invitations, first-speaker focus, fixed-greeting warmup and real live-wallpaper repair. Existing features and the retained newer package/signing identity remain. Install over a matching newer build without clearing data; preserve the installed app if its certificate is rejected.

527 JVM tests, 30 companion tests and 4 catalog tests passed; zero lint errors/fatals. Physical phone behavior remains unverified. General answers need the configured AI backend or installed local model. [Optional companion source ZIP](v2-2.11.6/JarvisCompanion-source-2.11.6.zip).

# Previous development build: Jarvis 11.5 — voice and core repair

[Download the signed ARM64 APK](voice-11.5/Jarvis-11.5.0-arm64-reinstall.apk) · [Changes, owner training and phone test](../docs/voice-11.5/REPORT.md) · [Verification](voice-11.5/VERIFICATION.md).

Repairs wake/greeting/capture handoff, empty speech finals and false recognition-error feedback; adds actual local clock answers, truthful provider errors and an optional locally trained owner-only spoken-command filter. The existing core uses adaptive state/audio-reactive animation. All prior charging/widgets/panels/privacy features remain. Real handset behavior still requires verification.

Package/signing identity match the newer 11.2/11.3/11.4 builds. Install over the matching build without clearing data. If Android rejects the signature, preserve the installed app/data. Owner similarity does not replace Android authentication; train and test on your phone.

Previous: [11.4](overcharge-11.4/Jarvis-11.4.0-arm64-reinstall.apk) · [11.3](rebirth-11.3/Jarvis-11.3.0-arm64-reinstall.apk) · [11.2 beta3](rebirth-11.2-beta3/Jarvis-11.2.0-beta3-arm64-reinstall.apk).

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
