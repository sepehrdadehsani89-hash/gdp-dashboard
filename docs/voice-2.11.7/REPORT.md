# Jarvis 2.11.7 — spoken calls, language switching and pickup greetings

Updates the existing `com.jarvis.next` application with version code 24 and the retained newer signing certificate. [Download and verification](../../downloads/v2-2.11.7/README.md). Existing core design, calls/check-ins, charging, widgets, panels, protected actions, owner profiles, saved voices, files, local AI and privacy controls remain. Gmail remains removed.

## Recognition correction

The reported pair of retries comes from the uncertain-final recognition path. In 2.11.6, an exact “call me now” could be rejected by the ordinary action-confidence threshold before reaching the local AI-call handler. This update permits exact low-risk AI-call-start, public wake-greeting and language-mode controls through that final-word gate, after validating available word scores. Calling a telephone contact, unlocking, deleting, compound requests and other device actions retain their prior confidence/owner/confirmation requirements. AI-call start rings; explicit Answer remains separate. No partial or empty transcript becomes an executable command.

Both the continuous wake recorder and foreground offline recorder use the corrected final recognition policy. Existing exclusive microphone leases, greeting-completion handoff, echo suppression, bounded error recovery and follow-up windows remain. The state-machine regression repeats low-score wake → greeting handoff → exact call start → ringing → Answer → follow-up → End twenty times, with deterministic transcripts and clocks. It is not an acoustic recording test.

Voice health now displays the last result category, word count, supplied mean confidence, threshold and source. It stores no transcript, recording or key. This helps distinguish genuine decoding failure from command routing. The same screen offers a direct local call test, actual model/voice status, a balanced speech-settings repair and existing output-voice setup. The repair preserves keys, IDs, private-speech consent and owner profiles; microphone effects apply on a new capture.

## Spoken English / Spanish mode

Say “change to Spanish,” “switch to Spanish mode,” “speak Spanish,” or “cambia a español.” Switch back with “change to English” or “cambia al inglés.” The selected language uses its separate saved Fish Audio profile. The Spanish profile retains the supplied Spanish reference ID `ae74d1059517440aa6e5d2d50598d179`; the English profile and editable English ID remain independent. The explicitly selected mode stays active for subsequent answers, including questions worded in the other language. Voice health can restore the existing automatic reply-language detection. Switching does not copy or replace credentials, overwrite saved IDs or enable a disabled voice. A disabled/unauthorized/unavailable voice reports its actual output problem.

The foreground recognition service closes its old decoder and microphone before loading the other language, with bounded waiting and stale-request guards. It remains visibly foreground and restarts paused; the spoken acknowledgement finishes before follow-up capture. An existing connected AI call can retain its call ID through a successful handoff. Stop invalidates a pending restart. Android credentials are still required for private changes; a locked request cannot falsely claim the mode changed.

If the selected offline speech pack is missing, the language/output profile changes, but the app explicitly reports that listening cannot resume until that pack is installed. It does not silently retain the wrong recognizer language, invent a download or send microphone audio to another service.

## Pickup greeting

There are six fixed English and six Spanish greetings, rotated in the selected mode. They are public courtesy phrases, not claims to see you, identify you or know that you are awake. Fixed public audio may use the existing bounded greeting cache; private conversation audio remains excluded.

The pickup monitor runs only inside the already user-enabled wake foreground service. It needs both accelerometer and proximity sensors, an uncovered sensor, an unlocked interactive screen and an idle wake-only session. It first observes a stationary phone, then bounded lift motion followed by a settled change in tilt. A pocket/cover, lock, active request/call, music, Silent/DND or configured quiet hours suppresses the greeting. A five-minute cooldown prevents repeated interruptions. Stop or disabling the setting unregisters sensors. Missing sensors/registration failures are reported; no microphone is started from boot, an alarm or a sensor alone.

This is a conservative pickup heuristic, not perfect pocket detection. A flat lift without sufficient tilt can be ignored. Sensor behavior, OEM restrictions and accidental-trigger rates need physical testing.

## Phone setup and acceptance

1. Install over a matching newer Jarvis build. Do not clear data. If Android rejects its certificate, preserve the installed app/data rather than uninstalling to force the update.
2. Open **Settings → Rebirth → Voice health · fix recognition, language & pickup greetings**. Check the selected language's speech pack is installed and its saved Fish voice is enabled. Preview the existing voice in **Output voice** if needed.
3. Enable wake listening normally. Say “Hey Jarvis” once; wait for the greeting to finish, then say “call me now.” Verify ringing and tap **Answer**. Ask the time, hear the actual local time, ask another question without waking again, then **End**. The direct call-test button separately checks UI/routing.
4. Say “change to Spanish.” Hear the saved Spanish voice, then ask “¿Qué hora es?” without another wake word during the follow-up window. Verify the selected and listening languages both show Español. If the speech pack is missing, select Spanish in Voice health and explicitly approve its approximately 39-MB download; enable listening again afterward. Switch back to English and verify its own saved voice.
5. Pickup greetings are enabled by default for this explicitly requested feature, with a switch on Voice health. With wake listening on, outside quiet hours, set the unlocked phone down for two seconds with the top sensor uncovered, then lift and tilt it. Hear one greeting and ask a follow-up. Cover the proximity sensor, lock the screen, play music and test the cooldown; those cases must stay quiet. The Test-next-greeting button bypasses only the cooldown, while retaining quiet/audio/session guards.
6. Repeat after background/foreground, phone calls, speech playback, Stop and language changes. Test an interrupted language handoff and absent Spanish model; no stale restart, overlapping microphone or fabricated successful connection should occur.

## Verification and limits

The [release verification](../../downloads/v2-2.11.7/VERIFICATION.md) records final automated results, lint, APK identity, retained signature and source-input hashes. Android instrumentation compilation is reported separately from execution. No physical Redmi 9 is attached in this environment: microphone recognition, accent/noise performance, acoustic Fish playback, real language handoff, proximity/pickup accuracy, lock-screen/OEM behavior and measured latency remain unverified on hardware. The official Vosk model host was previously denied by environment network policy; no cloud acoustic decoder claim is made.

General questions still require a working configured secure backend or installed local model. The full master backlog remains staged. This release corrects a demonstrated code path and adds the requested controls; it cannot establish that every possible spoken request or phone condition works without device acceptance.
