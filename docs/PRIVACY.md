# Privacy and control

- No phone data is bundled with the APK. Jarvis starts without an API key and requests access only through Android's permission/settings flows.
- The wake service processes microphone audio locally after the user starts it. There is a foreground notification. It does not start at boot or defeat force-stop.
- Offline speech models and voices require explicit downloads. Platform network speech is a separate disabled-by-default setting. Cloud LLM/TTS use requires a configured key and explicit selection.
- Contacts, calendar, saved coarse location, and notifications are read only on supported explicit requests and stay out of cloud context. The separate screenshot workflow can send one user-approved image and its visible text for explicit screen questions. Private requests are rejected while the device is locked.
- Files/photos are selected using Android's picker. Photos undergo local printed-text recognition; there is no general scene/face recognition or entire-library scan. Selected text can be sent to a cloud model only when the user has selected that processing route; attachments are scoped to that question.
- Conversation and memory are stored in app-private Room SQLite, not advertised as database encryption. Android cloud backup and device-transfer backup are excluded. Provider credentials use Keystore AES-GCM encryption and are not included in prompts, diagnostics, or source.
- Web tools send the requested public topic/city/query to the stated service. Source material is not authority to reveal unrelated private data or run actions. No analytics SDK is included.
- Memory contains facts explicitly requested with “remember” commands. The user can inspect/delete them, clear conversation, or wipe app-owned data. Uninstalling removes private app storage. Permission grants remain controlled by Android Settings.
- Stop everything halts app-owned capture, playback, pending confirmations, generation, and downloads. It cannot revoke an OS permission or undo an action already performed elsewhere.

The protected-app baseline and sensitive-text heuristics reduce exposure; they are not a claim to identify every secret or every banking package. Accessibility text does not reliably expose every app's secure-window state. Screenshot capture uses a visible per-capture consent activity followed by Android MediaProjection consent. It never bypasses secure windows or implements payment/password automation. Screens recognized as protected, or containing visible password fields, are rejected; the protected-package baseline is not exhaustive.

Private phone/tool outputs and answers using selected files or saved memories never use optional cloud speech. Only general AI chat without those private sources may opt into the user-enabled cloud-voice route. Voice Check can send its public greeting when the user explicitly enables cloud voice.

Private data and phone actions require an unlocked phone. General chat while locked is an optional, disabled-by-default setting. It sends no saved name, conversation history, saved facts, screen image or attachments. Nearby people may ask general questions and hear replies when enabled. The microphone must be started while the app is visible and the phone unlocked. A partial CPU wake lock supports active screen-off listening; it consumes battery and is released when listening stops. Android may restart an already user-enabled service after process death; there is no boot restart or force-stop bypass.

## Provider connection checks

In Settings, Find Gemini models sends the entered or saved Gemini key to Google in the API-key header and requests its model catalog. Test connection sends only a short sample prompt through the selected provider API. It excludes conversation history, saved memories, files, screen contents and notifications. These explicit tests may consume provider quota or incur charges. No key is sent to the source-code repository or included in an app download. Jarvis shows fixed explanations and known status codes rather than raw remote error bodies.

## Conversation memory and screenshots in 0.1.3

Up to four recent general conversation turns (at most 12,000 characters) are retained in RAM for follow-ups and sent only to the selected AI provider. Phone-tool, attachment and screenshot turns never enter this general buffer. It is cleared on screen-off, provider changes, memory-sharing changes, conversation clearing and data wipe. Explicitly saved facts remain in the app-private database across restarts. The optional Use saved memories with cloud AI setting sends at most 20 facts / 5,000 characters with general questions; it is off by default. Those answers still use local speech.

One captured JPEG (maximum 1.5 MB / longest side 1,280 pixels) and its visible accessibility text stay in RAM for up to two minutes, or until cleared, locked/screen-off, stopped, or replaced by an unrelated request. The image is never saved to the gallery or database. Follow-up screen questions share the image with Gemini only after the capture disclosure is accepted; other providers receive only visible text. Local processing uses only visible text, not visual inference. The capture service releases projection immediately after one frame. The user can preview or clear the image in the app. Screen imagery can contain sensitive data that heuristics miss; the capture disclosure explains what the selected provider receives.

Connection verification uses a 15-second deadline and a short non-streamed API response. A success dialog displays the actual sample and elapsed time; neither a saved key nor model listing is treated as a verified connection.
