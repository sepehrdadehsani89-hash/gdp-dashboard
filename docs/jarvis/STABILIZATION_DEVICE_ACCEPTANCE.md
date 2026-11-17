# Device acceptance — not executed in this workspace

All rows below are **BLOCKED / NOT RUN**: no authorized Redmi 9 or emulator is connected. These are acceptance procedures, not test results. A cloud JVM test cannot prove microphone isolation, audible playback, FPS, camera/GPS results, Android permission UI or installation compatibility.

Use the retained Phoenix preview update, not the original differently signed production package. Do not uninstall an application or clear its data to work around a signature conflict. Record device model, Android/MIUI version, artifact SHA-256, installed version code, current provider/mode/model and configured voice. Never record credentials, phone numbers, private transcripts or contact lists in shared evidence.

| # | Scenario and actual procedure | Required observation | Status |
| --- | --- | --- | --- |
| 1 | Ask a real authorized AI for a long answer, at normal media volume | Mic indicator/diagnostics show recognition paused during synthesis and playback; no duplicate user command | NOT RUN |
| 2 | Introduce ordinary background speech/noise during that answer | TTS is not cancelled by wake/ASR callbacks | NOT RUN |
| 3 | Tap Stop Speaking midway | Playback stops, caption clears, old callbacks cannot resume speech; cooldown then safe wake-only state | NOT RUN |
| 4 | Sign in using the supported provider flow | Authenticated identity and authorized AI access are accurately distinguished | NOT RUN |
| 5 | Explicitly test the exact selected model using authorized access | Real inference response; invalid/expired credentials, quota or model denial shown honestly | NOT RUN |
| 6 | Ask a question and compare spoken final answer with received text | Meaningful actual answer, no “look at the screen” replacement | NOT RUN |
| 7 | Mute media/voice or cause a supported output error, then restore output and tap Retry | AI response retained; TTS-specific failure; retry speaks it without another model request | NOT RUN |
| 8 | Set Reduced Motion off, change another setting, fully restart | Saved user choice remains off; system animation/Battery Saver restrictions shown separately | NOT RUN |
| 9 | Keep core visible for 60 seconds; exercise listening, thinking and speaking | Continuous real frame updates/state reactions; record measured pacing, no invented FPS | NOT RUN |
| 10 | Select live wallpaper, hide/show home, lock/unlock and recreate surface | No invisible rendering; visible surface resumes without frozen core | NOT RUN |
| 11 | Connect/disconnect charger and compare measured battery percentage | Emerald particles follow real charging state; effects stop on disconnect | NOT RUN |
| 12 | Change settings by UI and voice; fully restart | Both paths read the same values; unrelated preferences/provider keys unchanged | NOT RUN |
| 13 | Exercise torch, volume, brightness, app launch, timer, alarm, media, Wi-Fi, Bluetooth, mobile data and DND | Spoken result matches observed state or honestly requests Android UI/permission | NOT RUN |
| 14 | Complete/skip setup, restart, then reopen from About Jarvis | Wizard runs once per revision; accurate pending vs verified readiness; Back/Next/Skip work | NOT RUN |
| 15 | Repeat 30 recognition→inference→speech cycles, including one provider error and one manual stop | No frozen owner, duplicate request, self-interruption, leaked mic resource or endless listening loop | NOT RUN |
| 16 | Clean install in a test profile; choose Enable microphone, then test denial/grant | OS prompt required; no capture before grant; wake pack download needs its own confirmation | NOT RUN |
| 17 | Stop wake listening, restart, lock, revoke mic and return | Stop/revocation honored; no unauthorized locked/background/boot capture | NOT RUN |
| 18 | Use a dedicated Contacts fixture named Mom; say “remember that M-A means Mom”, then confirm | Exact match, encrypted nickname save, no Android contact rewrite or call; “call Ma” opens reviewed dialer only after confirmation | NOT RUN |
| 19 | Add two Mom numbers; test suffix disambiguation, changed contact before confirmation, denial and expired ticket | No guessed number, stale save or automatic dialing; permission prompt resumes only the current request | NOT RUN |
| 20 | Say “set voice volume to fifty percent”, “turn reduced motion off”, and a protected privacy/brain edit | Actual persisted readback; protection/consent honored; unverified brain not selected | NOT RUN |
| 21 | Say a credential-writing command using a dummy canary, then inspect diagnostics/history | Canary withheld from Jarvis history/model/TTS; UI-directed credential entry only | NOT RUN |
| 22 | Update prior Phoenix APK and inspect models/settings/memories | Same signer/package and increasing version; data preserved without uninstall | NOT RUN |
| 23 | Exercise Spanish setup, recognition, saved voice and public confirmation summaries | Functional selected language, no silent provider fallback or lost voice choice | NOT RUN |
| 24 | Use the optional owner enrollment prompt and cancel midway; test a battery-alert cancellation | Audio ownership released on every cancellation; enrollment capture waits for completion plus configured cooldown | NOT RUN |

The 20 new `StabilizationPreferencesTest` cases and existing instrumented test suites are compiled in the cloud verification task, but **zero instrumented tests have executed**. Those tests use isolated preference fixtures; run UI/hardware suites only on an authorized dedicated test device and keep existing fixture cleanup intact. Live ChatGPT, Gemini, Claude, Fish Audio and installed GGUF checks require actual authorized accounts/models and remain unexecuted here.
