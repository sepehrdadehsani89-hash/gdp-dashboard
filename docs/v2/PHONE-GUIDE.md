# Jarvis 2.11.6 phone guide

Install over a matching newer Jarvis build. Package and retained newer signing identity stay the same; version code 23 is higher than 11.5's 21 even though the displayed version now follows the requested 2.11.6 naming. If Android rejects a certificate, preserve the installed app/data and compare signatures; do not uninstall to force an update.

## Pick up an AI call

1. Keep your selected offline speech pack and configured Fish voice working. Output voice settings include a real provider preview; general questions need your configured AI backend or installed local model. Existing private-speech consent remains under your control.
2. Say “Hey Jarvis” once, then “call me now,” “let's do a call,” or “call me.” From the home screen, a Jarvis notification provides Answer/Decline; inside the app, the calling banner and call screen show the same state.
3. Tap **Answer**. Grant microphone access if needed; Android may require normal unlock to begin a new microphone session. The ringtone stops before command capture. Hear the connection greeting, then ask “What time is it?”
4. Ask follow-up questions without a new wake word. The call screen shows actual listening/thinking/speaking phases. Use phone media volume for speech. Private content and protected actions can require unlock or consent.
5. Tap **End call**, use the notification's End control, or say “end the call” while Jarvis is listening. Calls also end after a bounded quiet period or maximum duration. End restores the pre-call wake-listening choice; the app's overall Stop still turns listening off.

If Android restricts background ringing, tap the notification or open Jarvis. Silent mode, Do Not Disturb, another audio call, unavailable speech models, Fish configuration/HTTP errors and real backend failures are reported rather than disguised as recognition errors. This is an in-app AI voice conversation, not a telephone/carrier call. Unknown actions cannot execute merely because an AI says they can.

## Set a dated “call me” request

Open **Settings → Rebirth → Call me · editable date/time voice check-ins**, or say “call me tomorrow at 4:00 pm.” Review the date, time, AM/PM and displayed time zone. Choose **Exact** only after granting Android exact-alarm access, or explicitly choose **Approximate**, which Android can delay. Enable check-in notifications, select ringing/spoken invitation, then save.

Saved entries offer Edit date/time, Cancel/delete and Answer when due. Revision checks invalidate old alarm callbacks after an edit or cancellation. Exact scheduling requests do not guarantee exact acoustic playback: Doze/OEM policies, permissions, calls, network and voice output can affect it. Reboots restore future requests without starting a microphone. An interrupted or missed check-in is not labeled as spoken. No private reminder title is read aloud by the generic invitation.

## Battery calls at 20%

Open **Settings → Rebirth → Power Core** and enable **20% battery · invite me into a conversation**. Keep the existing call-safety permission, quiet hours and volume configured. A real threshold observation invites a call; Answer hears the latest valid battery reading, then you can ask questions/tasks. It does not continually repeat at the same level. Charging/recovery rearms it, and existing critical 3%/2%/1% alerts remain. Completed playback acknowledges an alert; ringing alone does not.

## Follow the first voice

Open **Settings → Rebirth → Set up owner voice** for the existing model/download controls and new **First-speaker focus** switch. This mode accepts any first speaker and uses a temporary local reference, normally seeded from the wake phrase. With the optional verified speaker model installed, other/uncertain voice windows become silence before ASR, so background speech does not extend your turn. No enrollment is needed for this temporary reference. Restart listening after changing capture mode.

The requested any-speaker conversation mode becomes the default once on this update; previous encrypted owner templates are kept. Owner-only questions/actions remain a separate optional switch. Speaker similarity can mistake similar voices or mixed recordings; it is not an unlock credential, perfect diarization, or guaranteed separation of overlapping voices. With no speaker model installed, ordinary ASR remains available and the UI reports that focus is unavailable. Test in your real room.

## Make the wallpaper move

Open **Settings → Rebirth → Fix / select moving Jarvis wallpaper**. The screen checks Android's actual selected component and reports real rendering counters. Tap **Select the real moving Jarvis wallpaper**, review Android's preview and apply it. Return to the launcher to observe it; full-screen apps and screen-off pause a hidden wallpaper.

A screenshot cannot animate. Restore app core motion explicitly if needed; system accessibility/reduced-motion and power/thermal limits are still respected. The existing geometry/colors remain, with a visible orbit marker and recoverable rendering failures. Frame targets are not measured phone FPS.

For computer pairing and the approved Chrome-only test, see [the companion guide](../../companion/README.md).
