# Output voice configuration

Open Jarvis → Settings → **Output voice · editable ID**. The initial ID is `05b36da8574341d0803391491850db20`. You may replace it. This is a synthesis reference, not a speaker profile, unlock credential or API key.

The implemented custom adapter is Fish Audio. The user has not identified the issuing service, so this ID's ownership, account access and audible result remain unverified. Do not enter an unrelated provider's key. If the ID belongs to a different service, leave this adapter disabled until that provider is implemented.

1. Confirm that your voice ID and account are for Fish Audio.
2. Enter your Fish Audio API key privately on the phone. Blank preserves an already saved key. The model defaults to `s2.1-pro` and is editable for account compatibility.
3. Tick the service confirmation. Read the network/cost notice and tap **Consent and play service preview**.
4. Jarvis requests voice metadata and a fixed public sample from `api.fish.audio`, then plays the actual returned MP3. Only successful playback permits enabling. Listen before confirming; identity metadata does not establish acoustic quality or your legal rights to use a voice.
5. Tap **Use this voice for replies** and complete Android's own authentication if configured.
6. Editing requires another preview before saving the replacement. Disable or remove saved credentials from this same screen.

General replies eligible for cloud speech use the enabled reference ID in the real TTS request. Private tool/file/screen/memory/security responses retain on-device output. API/network/audio failure attempts the existing local voice; local voice availability and Silent/Do Not Disturb still apply. Existing optional OpenAI voice settings are retained when custom output is disabled. New service keys stay encrypted in the app vault, excluded from existing memory exports, logs and model context.

A credential-free build cannot establish that a paid provider accepts this particular ID. No service request was made with a user's account during this build, and the output has not been acoustically verified on Redmi 9. Changing the output voice never authorizes phone unlock or other private actions.

Protocol reference reviewed: official `fishaudio/fish-audio-python` SDK, `fish-audio-sdk` package. Fish uses HTTPS `/model/{id}` metadata and `/v1/tts`, Bearer authentication, model header, bounded MessagePack request with `text`, `reference_id`, and `format=mp3`. Redirects and credential-bearing logs are disabled; requests are cancellable and responses bounded.
