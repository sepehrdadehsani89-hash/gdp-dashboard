# Jarvis 2.0

A native Android HUD based on the supplied reference: black panels, cyan and purple concentric reactor, actual battery in the center, clock, supported telemetry, weather, transcript, assistant responses, and functional Assistant / Memory / Settings tabs.

This is the renamed **Jarvis Next** installation: package `com.jarvis.next`, version `2.0.0` (code 2). It updates Jarvis Next 0.1.0 using its original certificate and preserves that installation's data. It remains separate from original Jarvis (`com.jarvis.assistant`); do not uninstall the original app. The cyan/purple reactor launcher icon helps distinguish this installation. Renaming changes neither Play Protect decisions nor Google API quota.

## Data and behavior

- Battery uses Android's protected battery broadcasts and sticky reading, including level/scale, charging/full, power source(s), temperature and health. No estimates of remaining battery time.
- Clock uses phone time and timezone, minute/time/date/locale broadcasts and system 12/24-hour observation. Format can be overridden. No clock polling every frame.
- Network shows actual transport and Android-validated internet status. Memory is explicitly **system available/total** and **this app's PSS**. CPU load and bandwidth are omitted because no supported measurement is implemented. A saved provider key is never described as a verified connection.
- Weather is Open-Meteo current/daily/hourly data, with city geocoding or a one-time approximate network location request. Permission is requested only for that button. Coordinates are rounded and not saved. City-selected responses are cached; missing measurements remain unavailable. Observation time is in the weather location's timezone. Stale/error/retry states are visible. Refreshes are limited to visible activity and sensible intervals; requests cancel on dismissal or city changes. City/coordinates are sent to Open-Meteo, without provider keys.
- Concentric arcs and mesh are decorative. Listening bars use real PCM RMS or Android recognizer RMS callbacks. Speaking motion is explicitly labelled decorative because system/cloud playback offers no output-level callback here. The battery number never becomes a fabricated listening percentage.
- Animation has cinematic/balanced/saver profiles, intensity, reduced motion, Android animator-disable support, and lower battery/power-saver rates. The drawing loop stops below STARTED and when the dashboard leaves composition. Caps are 30/20/8 FPS, with additional low-battery limits; these are design limits, not measured physical-device performance.
- Voice settings select actual installed offline system voices for the chosen language, or the existing optional neural models. System TTS pitch defaults to 0.90; rate and reply volume are adjustable. Neural voices support rate and volume; their pitch follows the voice model. Mic while speaking interrupts playback and starts listening. Existing streaming speech and audio-focus/quiet-mode protections remain.
- Wake phrases: **Hey Jarvis**, **Oye Jarvis**, **هی جارویس**, with existing aliases. Recognition uses the selected language's downloaded model; an English model is not advertised as multilingual. Wake events are deduplicated and cooldown-limited, and playback suppresses self-triggering. Background microphone use retains its foreground notification and stop action. Calls or Android-silenced capture suspend/stop recognition rather than fabricating listening.
- Wake activation posts a tappable notification. An unlocked background activity launch is attempted only with an enabled accessibility service, subject to Android policy; the notification remains the fallback. No full-screen intent, launcher replacement, overlay, or keyguard bypass. A protected locked command can remain pending briefly for normal unlock and device authentication. Idle auto-dismiss and a dismiss button return to the previous app.
- **Codex** triggers an action-bound Android `BiometricPrompt` with device-credential fallback. It is not a wake phrase or identity proof. Requests expire after 90 seconds, can be cancelled, reject mismatched IDs and cannot replay. Privacy/provider-credential changes and destructive data controls require this workflow when enabled. Existing screen capture consent and target-bound live-control confirmations remain. An Android screen lock is required for device authentication; the app never receives or stores the PIN.
- Room schema, existing settings and encrypted provider-key storage are unchanged. HUD preferences and memory categories are additive stores. Memory has search, categories, explicit save and delete. No automatic sensitive memory collection.
- Existing local Qwen, cloud providers, local speech, document/photo attachments, OCR, screen capture, connected SAF folder, manual WhatsApp/photo sharing, and target-bound accessibility taps/draft editing remain. Final Send stays manual. Local AI still needs the optional model download; it does not reset Gemini quota.
- Arithmetic (`calculate 2 + 3 * 4`) is bounded local decimal parsing. `remind me …` opens a confirmed system calendar draft: the user must choose a date and save it. Timers/alarms likewise expose the real system UI. No reminder is claimed saved merely because a draft opened.

## Permissions and setup

Android 10+ ARM64. Microphone and the language speech model are needed for local listening. Notification permission is needed for the activation fallback on Android 13+. Optional accessibility, notification access, contacts, calendar and approximate location remain explicit choices. Models are never downloaded silently, nor is a cloud provider silently substituted.

## Signing and private recovery

Release certificate SHA-256: `56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`.

The retained key remains outside this public repository at `/workspace/private/jarvis-next/jarvis-next.p12`, with its password in the adjacent private file and directory mode 0700. The encrypted backup is `/workspace/private/jarvis-next-signing.tar.enc`; its separate unlock file is `/workspace/private/jarvis-next-backup-password`. Download both privately and store them separately in an encrypted vault/offline storage. Neither belongs in Git, an issue, or a public download directory. Retain this certificate/key for every future update. The workspace alone is not a durable backup.

Restore the encrypted archive into a private directory using `openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in jarvis-next-signing.tar.enc -out signing.tar.gz -pass file:jarvis-next-backup-password`, inspect/extract locally with restrictive permissions, and verify the public certificate fingerprint before signing. Remove plaintext temporary archives after recovery.

The unavailable original Jarvis key cannot be reconstructed from its public APK. This release deliberately uses the separate installation's retained key.
