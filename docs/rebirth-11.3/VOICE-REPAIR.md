# Jarvis 11.3: speech and animation repair

Install the 11.3 ARM64 APK over the matching 11.2 development build. The package and retained signing certificate are unchanged. Keep the installed app and its data if Android rejects the update; do not uninstall or clear data to force installation.

## What changed

An unfinished Vosk partial previously prevented a turn from finishing. Room noise could also keep resetting the final-segment pause. The command recorder now asks Vosk for its real final result after one second of quiet or when the words have stopped changing for 2.5 seconds. Incomplete clauses keep a longer four-second grace period. The actual final passes the existing word-confidence check and optional speaker-focus check before any command dispatch. A partial caption is never executed as an action. Changing speech can still continue within the existing two-minute bound.

Accepted complete questions finish after one second of quiet, with a 1.8-second ceiling when background RMS stays high. Incomplete clauses retain their longer pause. Screen locking, app launching, timers and the existing fast commands can dispatch on an accepted engine final without waiting for the idle timeout. An unclear or rejected final now produces one fixed spoken retry prompt, then returns to wake-word detection instead of keeping an unusable turn open.

The Android speech-service fallback now requests a final when a partial stalls, waits at most 2.5 seconds after that request or the service's end-of-speech callback, and releases a service that never returns its final. Android can ignore the requested silence extras; the app's watchdog still bounds this wait. No partial is promoted into an action to hide a failed service.

Some native results are intentionally text-only/private. With a configured general Fish voice, these now receive a fixed audible acknowledgement such as “Sir, the result is ready on your screen” or “Please review the confirmation on your screen, sir.” The full result stays in captions/chat. The acknowledgement does not claim that a failed or unconfirmed action succeeded and contains no private tool output. Full private speech still requires the existing separate Fish Audio private-speech consent. Ordinary answers already eligible for Fish speech retain their full spoken response; no substitute voice is introduced.

The reactor uses a display-frame clock while its lifecycle is visible, retains its rotation across listening/thinking/speaking changes, and resumes without counting time spent hidden. Active states move more visibly. Reduced motion, zero intensity/speed, system animation settings and battery-saving limits remain respected. Charging behavior is preserved.

## Check on your phone

1. Open Jarvis after updating. Keep the existing English/Spanish voice configuration. Run its Output voice preview if you cannot hear a reply. Fish Audio needs your configured credential, connection and available quota; the build contains no account key.
2. Start listening in the visible app. Say **Hey Jarvis**, wait for its greeting to finish, then say **What time is it?** Stop speaking. An accepted request should reach the result/speech path without waiting for the twelve-second idle timeout.
3. Say **Hey Jarvis, lock my phone**. Existing lock-only Android administrator approval is still required. The requested spoken countdown and the existing visible fallback, Cancel/Stop safeguards and actual Android lock request remain. **Turn off**, **turn off my phone**, **turn off the screen**, and **turn my screen off** now use this same screen-lock action; they do not power down Android. Other commands such as turning off Bluetooth or the flashlight retain their own routes.
4. Open **Settings → Rebirth → Widgets & Jarvis panels** to use both installed-app widget copies and the existing Jarvis panels. Existing Android binding approvals remain required.
5. For a full spoken private tool/file result, review and explicitly enable the existing private-speech consent under Output voice only if you want that text sent to Fish Audio. Otherwise, the fixed acknowledgement and visible result are intentional.
6. Check the core while listening, thinking and speaking, then leave and reopen Jarvis. If you deliberately enabled reduced motion or zero animation speed/intensity, that preference still takes priority.

All previous panels, permissions, local chat cleanup/session notes, owner enrollment, Fish voice IDs, file grants, app aliases, charging and widget features are preserved. Voice recognition does not bypass Android unlock. Gmail remains removed as previously requested.

## Verification limits

The release includes JVM regression tests for stale partials, constant room noise, natural pauses, long changing speech, confidence rejection, correction of partial text, serial-bound one-time command acceptance, screen-off aliases, public/private speech routing, and visible-frame continuity after resume. Build, lint, package, signature, permission and public-download checks are recorded with the APK. These checks are not a real Redmi 9 recording, Fish account playback, microphone/OEM accuracy benchmark or device-lock test. Verify the phone steps above on your installed device.
