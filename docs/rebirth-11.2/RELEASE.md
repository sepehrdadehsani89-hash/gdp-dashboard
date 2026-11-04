# JARVIS 11.2 beta1 · Gmail-free speech update

This release follows the owner's newer request to remove Gmail integration and publish11.2 with repairs to locked-screen speech. That request supersedes the previous candidate's Gmail-related release hold. This remains a development beta: no claim of full master-directive completion, physical Redmi9 acceptance or commercial readiness.

## Changes since the held candidate

* Removed the Account Center activity, Google authorization SDK, Gmail API client, mailbox routing and account UI. No Google project, OAuth client or Gmail setup is required. Asking for inbox information returns an explicit unavailable result; opening the installed Gmail app remains an ordinary app launch.
* Repaired the explicit `!locked()` filter that discarded follow-up capture after a public wake greeting. After the greeting finishes or reports an audio error, an already-enabled wake service prepares a fresh command window. Its12-second inactivity timer begins only when the recorder and echo guard are ready, not while Fish is synthesizing or speaking.
* Public locked-screen conversation can continue when **General chat while locked** is enabled. That existing setting is preserved; the update does not enable it automatically. Private replies, personal data, app openings and other protected actions still require Android unlock. A disabled locked-chat setting is now explained aloud instead of producing only an invisible screen error.
* MediaPlayer prepared/completed/error callbacks and audio-focus loss are bound to the current player/session. Stale events cannot finish a newer audio turn or cancel its listening. Audio preparation is limited to15seconds; playback has a duration/rate-based timeout, at most5minutes per segment. Cleanup failures do not leave the speech-suppression flag permanently active.
* A transient Android-silenced wake recorder now releases its resources and enters the existing two-retry recovery budget. Persistent failure or missing permission still stops with an actionable status; no background permission bypass or infinite retry is added.
* Retains the candidate's microphone lease, alternative-aware app correction, approved alias priority, live-only RAM conversations, bounded buffers, personal content access and strict Fish-only English/Spanish output.

The remaining features and device-test gaps in IMPLEMENTATION-REPORT.md still apply except for its archived Gmail/account feature. Earlier candidate documents/build-info describe the held code15 artifact, not this release. Current verification accompanies the beta download.

## Phone setup and first check

1. Download the APK from the release README. In Android's installer, choose **Update** only if it recognizes a compatible existing installation. Do not uninstall or clear data to work around a signing conflict.
2. Open **Jarvis → Settings → Memory & locked-phone replies → General chat while locked**. Enable it only if you want people near the phone to be able to ask general questions, then save settings. Leave it off if that is not wanted; the wake greeting can still accept a command, but protected/disabled requests explain the unlock requirement.
3. Confirm microphone permission, the downloaded offline wake model and your private Fish configuration. Start wake listening from Jarvis while the app is visible. The microphone notification must be present. Android/OEM restrictions can prevent background starts; this update does not bypass them.
4. Lock the phone, say the wake phrase once, wait for the greeting to finish, then speak your command without another wake phrase. After an allowed general answer, ask a follow-up. Silence returns to wake-only detection after the bounded inactivity window.
5. Check Stop, screen lock, network/audio failure and interruption. If Android stops/silences the service persistently, open Jarvis and retry from its listening controls. Silent/Do Not Disturb still suppresses output. Do not disable Android protections as a workaround.

The source policy and callback repairs are implemented and host-tested as reported with the download. Actual microphone capture, Fish sound, OEM behavior and the reported locked-phone sequence must still be checked on the phone; no handset or live provider account is connected to this cloud.

## Identity and data

Package `com.jarvis.next`, version code16 /`11.2.0-beta1-reinstall`, Android10+/ARM64. The retained certificate is unchanged; no signing key was generated. The historical build-type suffix does not instruct an uninstall. Only a matching installed signing certificate with a lower version code accepts an in-place update. Original Jarvis7.0 uses a different certificate; its unavailable original private key is still needed for an update that preserves that installation. Keep the installed app/data if Android rejects this APK.

Saved settings, aliases and approved memories are preserved. No new dangerous permission or automatic cloud content sharing is introduced. Gmail account storage from an experimental candidate is not accessed by this build. The configured English Fish ID remains editable; Spanish remains exclusive. Private-content speech retains its separate consent requirement, and no other voice silently substitutes for Fish.
