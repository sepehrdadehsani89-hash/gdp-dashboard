# Privacy and control

- No phone data is bundled with the APK. Jarvis starts without an API key and requests access only through Android's permission/settings flows.
- The wake service processes microphone audio locally after the user starts it. There is a foreground notification. It does not start at boot or defeat force-stop.
- Offline speech models and voices require explicit downloads. Platform network speech is a separate disabled-by-default setting. Cloud LLM/TTS use requires a configured key and explicit selection.
- Contacts, calendar, saved coarse location, screen text, and notifications are read only on supported explicit requests and stay out of cloud context. Private requests are rejected while the device is locked.
- Files/photos are selected using Android's picker. Photos undergo local printed-text recognition; there is no general scene/face recognition or entire-library scan. Selected text can be sent to a cloud model only when the user has selected that processing route; attachments are scoped to that question.
- Conversation and memory are stored in app-private Room SQLite, not advertised as database encryption. Android cloud backup and device-transfer backup are excluded. Provider credentials use Keystore AES-GCM encryption and are not included in prompts, diagnostics, or source.
- Web tools send the requested public topic/city/query to the stated service. Source material is not authority to reveal unrelated private data or run actions. No analytics SDK is included.
- Memory contains facts explicitly requested with “remember” commands. The user can inspect/delete them, clear conversation, or wipe app-owned data. Uninstalling removes private app storage. Permission grants remain controlled by Android Settings.
- Stop everything halts app-owned capture, playback, pending confirmations, generation, and downloads. It cannot revoke an OS permission or undo an action already performed elsewhere.

The protected-app baseline and sensitive-text heuristics reduce exposure; they are not a claim to identify every secret or every banking package. Accessibility text does not reliably expose every app's secure-window state. There is no screenshot capture or payment/password automation in this preview.

Private phone/tool outputs and answers using selected files or saved memories never use optional cloud speech. Only general AI chat without those private sources may opt into the user-enabled cloud-voice route. Voice Check can send its public greeting when the user explicitly enables cloud voice.

This preview requires an unlocked phone to execute commands. It does not provide lock-screen assistant behavior.
