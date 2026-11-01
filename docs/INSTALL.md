# Install and first-run setup

## Install the supplied APK directly on your Android

1. Open the APK download from the delivery on your Android phone. An APK is an Android installation file; a Markdown document or source ZIP is not the app.
2. Save it, then open the downloaded `.apk` in Files/Downloads. If Android asks, allow that specific browser or file manager to install unknown apps. Do not disable unrelated device security settings.
3. Tap **Install**, then **Open**. Android installation remains a user-approved operation; a link or QR code cannot remove that approval.
4. Complete Jarvis onboarding and choose your language. Keep optional permissions off until you need their features.
5. In **Settings**, download the selected language's offline model, test speech, and explicitly enable wake listening when ready.

If a QR code is supplied, it must contain the actual published HTTPS APK/download-page URL. A `localhost`, private workspace path, or source-code URL will not install the app from another phone. Scanning a QR code opens the download flow; Android still asks you to install.

## Redmi 9: suggested no-key setup

Use Wi-Fi and keep adequate free storage. Downloads are separate from the APK:

| Download | Approximate archive size | Purpose |
| --- | --- | --- |
| English Vosk model | 40 MB | Offline speech and selected-language wake listening |
| Spanish Vosk model | 39 MB | Same, for Spanish |
| Persian Vosk model | 53 MB | Same, for Persian |
| Optional neural voice | 60–70 MB per language | Speech output when no offline system voice is available |
| Local Qwen 0.5B model | 400 MB | General text conversation without an API key |

Only download the languages you need first. Expanded files, temporary archives, and available RAM require more space than the listed download sizes. The app checks minimum free storage before starting model downloads.

Start with push-to-talk. Ask for news or a supported shortcut; add a fact with a “remember” command, then inspect the Memory screen. For open-ended conversation, download the local brain. It may respond slowly on this hardware; close other apps if memory is constrained.

Run the language's **Voice Check**. If no offline system voice is installed, use the optional offline voice download rather than assuming the phone supports Persian TTS. A successful settings selection is not proof that audible speech works—listen to the sample.

Wake phrases are selected by the current listening language:

- English: “Hey Jarvis”
- Spanish: “Hola Jarvis”
- Persian: «سلام جارویس»

Wake recognition is not guaranteed for every accent or pronunciation, and this build does not listen with three models simultaneously. Switch the selected language when needed. The microphone notification shows active background listening. Reopen Jarvis to resume if Android stops it.

## Permission choices

| Access | Needed for | How it is granted |
| --- | --- | --- |
| Microphone | Push-to-talk and wake listening | Android runtime permission |
| Notifications | Listening/status notifications on supported OS versions | Android runtime permission where applicable |
| Accessibility | Optional supported screen context/navigation | Android Accessibility Settings, explicitly enabled by you |
| Notification access | Optional notification summaries | Android special-access screen, explicitly enabled by you |
| Contacts/calendar/location | Related supported utilities | Separate runtime permission when requested |
| Files/photos | Selected documents or images | Android system picker; select what you want to share |

No installation script enables accessibility, grants all permissions, edits secure settings, or removes Android sandbox protections. Private content and actions remain subject to the application's policy. You can revoke permissions in Android Settings.

## Build and install using a computer

Install the prerequisites in [COMPATIBILITY.md](COMPATIBILITY.md), then run from the project root:

```bash
bash scripts/doctor.sh
bash scripts/build.sh
```

Windows PowerShell:

```powershell
.\scripts\doctor.ps1
.\scripts\build.ps1
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. The build script prints its SHA-256 checksum. The complete source distribution includes the Gradle Wrapper, vendored native source, required packaged runtime, and upstream notices/source material.

### USB

Enable Developer options and USB debugging on your phone, connect a data-capable USB cable, and approve the phone's debugging prompt. Check device status:

```bash
adb devices -l
```

`unauthorized` means the phone has not approved this computer; `offline` means the transport is not ready. The installer only accepts the `device` state. With more than one authorized device, specify the serial:

```bash
bash scripts/install.sh --serial YOUR_DEVICE_SERIAL
```

```powershell
.\scripts\install.ps1 -Serial YOUR_DEVICE_SERIAL
```

The installer builds a debug APK, installs with `adb install -r`, and opens Jarvis. The app itself handles permissions and user-initiated listening. To reuse a built file, pass `--apk /path/to/file.apk` or PowerShell `-Apk C:\path\file.apk`.

### Wireless debugging

Android 11 and newer may expose **Developer options → Wireless debugging**. Put the phone and computer on the same trusted network, open **Pair device with pairing code**, and use the exact host/ports shown by your phone:

```bash
adb pair PHONE_IP:PAIRING_PORT
adb connect PHONE_IP:DEBUGGING_PORT
adb devices -l
```

Enter the pairing code in your own terminal. The pairing and debugging ports can differ. Then run the installer with the displayed device serial.

A Redmi 9 still running Android 10 may not offer this pairing flow. Use USB or direct APK installation. Do not expose an unauthenticated ADB port to the Internet.

## Recovery

- **APK does not install:** confirm Android 10+ and ARM64 support, sufficient storage, and that the APK download completed. An update signed by a different development key cannot replace an existing installation. Preserve/export anything you need before choosing to uninstall it; the script never removes your app automatically.
- **No speech input:** grant microphone access, finish the selected-language model download, and try push-to-talk. Network speech is a separate opt-in fallback.
- **No audible reply:** check volume, DND, audio route, system voice availability, and Voice Check. Download the offline voice if required.
- **No open-ended answer:** confirm the local Qwen model is installed or configure your own optional cloud key. News/shortcuts and general model conversation use different paths.
- **Listening stopped:** Android/MIUI may stop long-running capture. Reopen Jarvis and explicitly resume. Battery settings can affect reliability, but no setting makes an Android process permanent.
- **Files or apps are unavailable:** Android does not give arbitrary access to another app's private data. Use supported intents/pickers or the optional access feature that fits the task.
- **Download fails:** check connectivity, storage, and service availability. Integrity failures must be retried from the official source, never bypassed.

For phone testing, keep installation/UI evidence separate from microphone, voice quality, background, inference-speed, and battery results.

## Redmi / MIUI background settings

After explicitly starting listening, MIUI may limit it. Where available, open Settings → Apps → Manage apps → Jarvis → Battery saver and select No restrictions. Some MIUI versions also expose an Autostart permission under app permissions. Labels vary. These settings do not make this build start the microphone at boot or survive force-stop: reopen Jarvis and tap Start listening when necessary. Keep the listening notification enabled.
