# Jarvis stabilization preview

Install the new Phoenix preview over the previous Phoenix preview. Its display name is now Jarvis; the isolated package and signer remain the same. The original production Jarvis, if present, uses another signing certificate and cannot be updated by this preview. Keep that application and its data intact. If both icons show Jarvis, use Android App info to identify this preview by package `com.jarvis.next.phoenix` and version code 30 before testing.

## Microphone and conversation

During setup choose **Enable microphone & wake listening**. Android requests microphone permission once; grant it in the system dialog. Wake listening also needs the selected language's offline recognition pack, separate from a Local Brain model. The pack is downloaded only after you approve it. Once enabled, Jarvis can reuse that permission and opt-in when opened. Android can still revoke access, stop a background process or restrict microphone startup. A powered-off phone cannot listen.

The wake service pauses while Jarvis synthesizes/speaks and during a short cooldown. By default you activate the next request with the wake phrase or microphone button. Voice & Microphone → microphone diagnostics contains an explicit continuous-conversation choice and cooldown adjustment. **Stop Speaking** stops playback without needing the microphone; **Retry Voice Playback** retries a failed spoken answer. AI Brain verification and output voice setup remain separate.

## Voice edits

Examples using the existing local stores:

- “Set reduced motion to off.”
- “Set voice volume to fifty percent.”
- “Set voice speed to one hundred percent.”
- “Turn speech captions on.”
- “Set microphone sensitivity to balanced.”
- “Set speech cooldown to 800 milliseconds.”
- “Set language to Spanish.”
- “Use ChatGPT.” This requires a previously verified exact account/model and confirmation.
- “Turn private voice replies on.” Review the explicit Fish Audio data-sharing consent and the existing Android authentication policy before approving.
- “Turn microphone off.” Confirm to stop wake listening.
- “What is the speech cooldown?”
- “Voice settings help.”

This allowlist covers supported Jarvis controls. It cannot grant Android permissions, change protected OS settings arbitrarily, dictate provider keys/passwords or invent a verified connection. Some camera/security/data-deletion controls remain in their explicit consent screens. Do not dictate secrets: credential commands are withheld and directed to the private Brain Manager.

## Contact nicknames and memory

Say **“Remember that Ma means Mom”** or **“Set contact alias M-A to Mom.”** Android Contacts access is requested if needed. Use the exact saved contact name; multiple different numbers require clarification, such as “Mom ending 4567”, or the contact picker. Confirm the proposed nickname. Jarvis saves an encrypted local shortcut; it does not rewrite Android's contact record. Later, **“Call Ma”** proposes opening the dialer. Confirm, review the number and press Android's Call button. Jarvis cannot infer that a call connected or went unanswered.

Ordinary explicit memory requests remain available. Their spoken save acknowledgement does not reveal the stored private content. Reading private contact details, memories or other selected content through Fish Audio requires the separate private-speech consent; a useful public action summary is used where the native tool provides one.

## Settings and setup

The existing holographic home interface is retained. Settings now has ten searchable sections; detailed preferences remain accessible under Account & Identity and About Jarvis. Appearance opens the existing wallpaper/motion controls. A major-update wizard runs once, can be skipped, and can be reopened from About Jarvis. Its final screen distinguishes pending setup from verified inference and a voice test you actually heard.

This is a preview with real software test evidence. Redmi 9 audio/GPU/permission/installation acceptance is still required; Google Play publication is blocked by the checks in `GOOGLE_PLAY_READINESS.md`.
