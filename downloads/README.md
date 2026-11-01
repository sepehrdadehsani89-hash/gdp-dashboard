# Jarvis for your Android phone

Development preview 0.1.1 · Android 10+ · ARM64 · intended first device: Redmi 9.

Direct Android download:

[Download Jarvis APK](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/ed1fdf2f2a53a480565d288d05cd644657bce1e1/downloads/Jarvis-0.1.1-arm64.apk)

1. Open the link on Android and download the APK. Android asks you to approve installation; it cannot be installed silently.
2. Open Jarvis and choose English, Español, or فارسی. No API key is required.
3. Download the selected language's speech pack in Settings (~40–55 MB). Run Voice Check; download its offline voice (~65 MB) if needed.
4. For general questions without a key, download the optional local brain (~400 MB). On a Redmi 9 it can be slow and its Persian quality is limited. News/YouTube/memory shortcuts work without this brain.
5. Enable microphone listening from the visible app when ready. Use the selected wake phrase: Hey Jarvis / Hola Jarvis / سلام جارویس. Only the selected listening language is active at a time.

You decide which permissions to grant. Files/photos are selected explicitly; Android's private-app sandboxes remain in place. Background listening is best effort and does not restart silently after boot/force-stop.

SHA-256 and build details: [SHA256SUMS](SHA256SUMS), [build-info.json](build-info.json). Source and licensing accompany this download in the same branch. See [verification](../docs/VERIFICATION.md) for tested versus unverified capabilities.

[Step-by-step phone setup](../docs/PHONE_SETUP.md)

![Scan on your Android phone](Jarvis-QR.png)
