# Set up Jarvis on your Redmi 9

Android preview 0.1.2 · Android 10 or newer · ARM64.

[Download the Android APK, about 98 MB](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/jarvis-android-download/downloads/Jarvis-0.1.2-arm64.apk)

[Open the download QR code](../downloads/Jarvis-QR.png)

You can complete this setup entirely on your phone. No computer, USB debugging, or API key is required. Use Wi-Fi, keep Jarvis open while its packs download, and start with about 2 GB of free storage for the app, temporary files, and optional models.

## 1. Download and install

**Known block:** If Play Protect shows “App blocked to protect your device,” stop installation and follow the [official review steps and APK packet](PLAY_PROTECT_REVIEW.md). Allowing a browser to install unknown apps is a separate Android setting and does not clear a Play Protect verdict. No feature has been removed from this full build.

Open the APK link on your **Redmi 9**, or scan the QR from your computer's screen with the phone camera. If the chat browser does not download it, copy the link into Chrome. Save the file, then open **Downloads → Jarvis-0.1.2-arm64.apk**.

If Android asks, open **Settings → Allow from this source** for the browser/file manager you used, return, and tap **Install → Open**. You can revoke that installation permission afterward. The QR starts a download; Android still requires you to approve installation.

## 2. Choose your language and microphone access

Select **English**, **Español**, or **فارسی**. Your name is optional. Tap **Allow microphone** and approve Android's microphone prompt.

| Button | Spanish | Persian |
| --- | --- | --- |
| Allow microphone | Permitir micrófono | اجازهٔ میکروفن |
| Test voice | Probar voz | آزمایش صدا |
| Get offline voice | Descargar voz | دانلود صدا |
| Meet Jarvis | Conocer a Jarvis | شروع با جارویس |
| Settings | Ajustes | تنظیمات |

## 3. Install the selected language's speech pack

On the opening screen, scroll to **Prepare offline speech** and tap **Download [language] speech model**. It is about **40 MB for English**, **39 MB for Spanish**, or **53 MB for Persian**. Wait for the progress bar to finish.

This pack lets the Redmi 9 recognize your speech locally and supports wake listening in that language. Start with one language; you can install the others later in Settings.

## 4. Make sure you hear Jarvis

Turn up **media volume**, then tap **Test voice**. For this first check, Silent/Do Not Disturb should be off and the audio should be routed where you expect it, such as the phone speaker.

If you do not hear a suitable voice, tap **Get offline voice**. This downloads about **65 MB** for the selected language. Test again after it finishes. This is the Persian fallback when the phone has no installed Persian system voice; no API key is needed.

Tap **Meet Jarvis** to enter the main screen. These downloads and checks are also available in **Settings** as **Download language model**, **Download offline [language] voice**, and **Check this language's voice**.

## 5. Try the microphone and memory

On the Assistant screen, **tap the microphone once**, speak a short command, then pause. You do not need to hold the button. The square Stop button cancels a request.

Start with **“What time is it?”**. Then try these commands in your selected language:

| English | Spanish | Persian |
| --- | --- | --- |
| Give me yesterday's news | Dame las noticias de ayer | خبرهای دیروز را بگو |
| Open YouTube | Abre YouTube | یوتیوب را باز کن |
| Remember that my favorite color is blue | Recuerda que mi color favorito es azul | یادت بماند که رنگ مورد علاقه‌ام آبی است |

News requires an internet connection. YouTube opens after you approve its action preview. Open **Memory / Memoria / حافظه** to see and delete explicitly saved notes.

## 6. Add general AI conversation without a key

Open **Settings / Ajustes / تنظیمات**. Keep the provider set to **On-device · no key / Local · sin clave / روی گوشی · بدون کلید**, then tap **Download on-device model / Descargar modelo local / دانلود مدل روی گوشی**.

The download is about **400 MB**, so use Wi-Fi and leave Jarvis open until it finishes. Tap **Save settings / Guardar ajustes / ذخیرهٔ تنظیمات** after changing options.

The local brain adds general questions and answers. News, time, supported app shortcuts, and explicit memories already work without it. The model is small: responses can be slow on a Redmi 9, and Persian answer quality is limited. Close other apps if memory is tight. You do not need to enter a cloud API key for this route.

For Gemini instead, use the [phone connection guide](GEMINI_SETUP.md): select Gemini, find a supported text model, test the connection, then save. A stored key is not a verified connection. Live-only models use a different API.

## 7. Enable the wake phrase

After the microphone and voice checks work, open Settings and tap **Start listening / Escuchar / شروع شنیدن**. Keep the microphone notification enabled.

| Selected language | Say |
| --- | --- |
| English | Hey Jarvis |
| Spanish | Hola Jarvis |
| Persian | سلام جارویس |

Say the phrase, then your command. **Keep the phone unlocked for commands in this preview. Only one listening language is active at a time.** To switch, choose another language in Settings, download its packs if missing, and tap Start listening again.

If the phone locked or listening became paused, unlock it, tap **Pause**, then **Start listening** again.

Wake capture pauses while Jarvis processes or speaks. This preview does not provide voice interruption during an answer; use the Stop button or notification's **Stop everything** action.

MIUI may stop background listening. Where available, use **Phone Settings → Apps → Manage apps → Jarvis → Battery saver → No restrictions**. Labels vary across MIUI versions. After a phone restart, force-stop, or OS shutdown of the service, reopen Jarvis and start listening again. No setting makes it permanent.

## 8. Connect optional phone information

Open Jarvis Settings and enable only the features you want. Contacts, calendar, and location have separate Android permissions. **Notification access** for reading notifications is separate from allowing Jarvis to display its own notification. Optional **Accessibility** enables supported screen text and navigation.

Use the **paperclip** for a document or the **picture icon** for a photo. You select what is shared; up to three attachments are processed per question. Photo reading extracts printed text locally, including Persian. It does not identify faces, objects, or scenes. PDFs are limited to the first eight pages.

Android does not give Jarvis unrestricted access to other apps' private databases or your entire phone. Private tool, memory, screen, notification, and file answers use on-device voices even if optional cloud voice is enabled. Protected banking/payment/password-manager automation is not provided.

## If something does not work

| Problem | First check |
| --- | --- |
| No speech input | Microphone permission, the selected language's completed speech-pack download, and tap–speak–pause |
| No audible answer | Media volume, Silent/DND, Bluetooth/output route, and Test voice; install the offline voice if missing |
| General questions ask for setup | Finish the optional 400 MB on-device model download |
| Wake phrase fails | Start listening again, check the selected language/notification, and use the microphone button as a fallback |
| No headlines | Internet connection and feed availability; try a topic or latest headlines. The offline model is not a live news source |
| Download fails | Wi-Fi, free storage, and keeping the app open; retry after correcting the issue |
| APK will not install | Android 10+, ARM64, completed download, and free space; retain the exact Android error message |

This is a development preview. Its build, UI/privacy checks, actual Android OCR, and three-language neural synthesis were tested. Real Redmi 9 microphone recognition, speaker audibility, wake accuracy, battery use, and speed still need testing on your phone. [Verification details](VERIFICATION.md).
