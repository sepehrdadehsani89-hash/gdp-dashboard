# Jarvis 5.2 setup and reconfiguration

This continues the existing Android application. Existing accounts, settings, memories, original holographic geometry and prior release files are retained. The 5.2 build is a debug preview; actual Redmi 9 installation and physical acceptance must be reported separately from cloud tests.

## Install the correct update

The preview package is `com.jarvis.next.phoenix`, version `5.2.0-preview.1-debug`, versionCode **33**, minimum Android **10 / API 29**, target API **35**, ARM64. Android 12 is within this SDK range. The build requires the retained Phoenix signer; it refuses to generate a replacement certificate.

Certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`.

Certificate SHA-1: `64:71:1C:61:B2:AC:55:D4:78:E5:F9:22:80:F5:40:61:36:9E:E3:1B`.

The new artifact location is `downloads/jarvis-5.2-preview/`; its README, metadata and checksum identify the verified source and final binary. A public HTTPS URL is supplied only after actual authorized publication and a complete anonymous download/checksum verification. Do not use the older 4.1 URL as a 5.2 download.

Download the published APK, open the downloaded file and approve Android's normal installer prompt. If Android requires permission for that download source, grant it deliberately in Android. Installation is never silent. This is a candidate update for the known Phoenix 4.1 code 32 APK with the same certificate. The package/signature actually installed on the owner's phone still has to be measured or an update actually accepted.

If Android rejects the update, **keep the existing application installed and preserve its storage**. Do not uninstall, clear data, downgrade or switch package identifiers to evade a rejection. The original production `com.jarvis.next` package has a different required signing identity; this preview cannot replace it. See [troubleshooting](JARVIS_TROUBLESHOOTING.md).

## Use the guided setup

Open Settings → Connections → **Complete setup / reconfiguration wizard**. The original first-launch wizard also links to this extended flow. Existing users are not forced through a new onboarding migration.

The 15 stages cover welcome, Android permissions, AI, Google Cloud, account choice, service consent, Gmail, Calendar/Drive, notification access, Android controls, voice, wallpaper, computers, live verification and completion. Optional steps may be skipped. A reviewed or skipped step records progress only. It does not create an account grant, API proof or physical test result. Existing working connections remain available when setup is skipped.

## AI and voice

Open AI Brain Manager. Choose the provider and credential mode you actually own, save the authorized credential privately on the phone, load its actual model list and run its real model test. OpenAI API billing is separate from a consumer ChatGPT subscription. A ChatGPT sign-in is not model inference proof; only an eligible authorized connection can invoke a model. Gemini, Claude and Perplexity require their own configured access. There is no automatic switch to another paid provider.

Local Brain uses its existing explicit model installation. No cloud workspace is added to it and no extra language model is downloaded automatically. Offline speech recognition packs, wake recognition and the local language model are distinct installations.

Grant Microphone through Android when you choose listening. Test recognition, the actual selected AI answer, output voice and whether you really hear it. A playback completion callback does not establish audibility. Configure English, Spanish or Persian and the existing personality/courtesy preferences. Stop All Listening stops conversation and wake monitoring; a farewell finishes its final speech and blocks automatic conversation restart. Physical microphone release still needs device verification.

## Google and notifications

Use [Google Cloud setup](JARVIS_GOOGLE_CLOUD_GUIDE.md). Account selection, API-key access and private Google OAuth permissions are separate. Each private service needs its own consent and a real successful read before it is shown verified.

Notification Awareness needs Android's Notification Listener access and explicit allowed sources. It reads only the current exposed notification previews, filters protected content and reports permission/disconnection states. It is not a private chat-history scraper or a substitute for Gmail's UNREAD-label count.

## Android controls and modes

Use Android Controls / Command Lab to see typed capabilities and real test results. Flashlight, volume and battery use Android APIs where available. Protected Wi-Fi/Bluetooth/DND/cast/sharing/lock/unlock features use legitimate system permission or settings flows. A launched settings screen is not a verified setting change. Device/vendor-dependent capabilities are labeled unsupported or action required.

Silent Mode and Deep Shutdown are under Appearance & Wallpaper and the extended wizard. Silent Mode dims/reduces owned visuals, hides private wallpaper previews and suppresses optional Jarvis briefing/battery activity. Configure its wake/reminder exceptions. Wake permission and starting a listening service remain explicit actions. A four-second visual awakening can run while commands proceed; explicit Reduced Motion still wins.

Android DND is a separate permission-gated request with actual interruption-filter readback; Android retains control of its exceptions. Jarvis does not silently restore or override another application's DND rule.

Deep Shutdown requires confirmation and Android authentication where configured. It stops Jarvis capture, wake recovery and current optional AI/companion/download work. It preserves accounts, permanent pairing trust, files, layouts and reminder data. Reminder exceptions follow your chosen setting. It does not power off the phone or stop unrelated services. Manually opening the main Jarvis app reactivates it without granting or automatically restarting the microphone.

## Wallpaper Studio

The original wallpaper is the default. Open Appearance & Wallpaper → **Wallpaper Studio · Color Studio · Widget Library**. Select a layer and drag the actual preview or use X/Y, rotation, scale, opacity and ordering controls. Native groups use offsets around their original positions; data widgets use normalized absolute positions.

Use HEX (`#RRGGBB` / `#AARRGGBB`), RGB, HSV/HSL or a two-color gradient. Apply presets or custom colors. Save up to eight profiles with up to 24 owned layers each. Saving a profile does not bypass Android's live wallpaper chooser: use **Preview / select the real live wallpaper in Android**. Restore Original composition preserves the saved custom profiles and original preferences.

Private widgets are off by default. They require the profile opt-in, an unlocked active device and the source's actual permission. Google counts/snapshots come only from recent authorized reads and expire; notifications are separate Android events. Unknown calendar/task/music metadata or unsupported sources remain unavailable, not decorative fake values. Device rendering performance, home-screen persistence and reduced-motion behavior still require the physical acceptance run.

## Pair a computer

Use the [computer pairing guide](JARVIS_COMPUTER_PAIRING_GUIDE.md). No USB cable or ADB is required for ordinary Jarvis companion operations. The companion currently implements only reviewed Chrome opening and search; native Windows power, files, media and arbitrary-app operations remain unsupported.

The complete real-phone/Windows acceptance checklist is separate from software fixtures. A scanned invitation, public HTTPS reachability, saved pairing and authenticated signed capability response are different stages. None alone proves a browser command completed.
