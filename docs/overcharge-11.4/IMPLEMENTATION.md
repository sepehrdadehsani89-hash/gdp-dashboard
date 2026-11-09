# J.A.R.V.I.S. 11.4 — OVERCHARGE

This upgrades the existing application and retains the validated 11.3 speech fixes, Android package, newer signing certificate, configuration, local model support, live chat policy, saved memories, widget host and all existing panels. The optional short local session summaries remain governed by the user's existing explicit preference; full conversations are not archived.

## Source audit and actual repairs

The baseline is the successful 11.3 build, source commit `a91679338f4ea1af7681125455ad8344426a24f3`, with 376 passing JVM tests. Build/manifest, battery receivers, existing alert policy, wallpaper, wake/offline/Android recognizers, Fish playback, settings, WorkManager and local operational crash-diagnostic code were inspected. A connected phone and its crash history were unavailable, so no handset-specific crash cause is asserted. The remote Superdesign project was not accessed through a connected design tool; the existing Sentinel geometry, colors, panels and native assets were reused.

The former battery code already included numerical thresholds through 1%. It nevertheless reserved a level before speech completed, so failed speech could suppress later attempts. Its pending alert could also expire without a spoken retry, and its existing safety policy blocks speech without reliable call-state permission. The new coordinator acknowledges actual playback completion, persists a bounded retry record separately, keeps the latest observed critical level, reports blocking conditions, and offers an explicit retry action. These are identified source failure paths, not proof of which path occurred on the Redmi 9.

The 11.3 speech repair finalizes stalled recognition through the actual decoder, validates final confidence, bounds Android's final-result wait, and caps background RMS's ability to hold a completed question open. Its 12-second inactivity timer is separate from the two-minute active-utterance bound and from temporary conversation context. Changing speech can continue beyond twelve seconds. A partial is never promoted to a device action. Unclear speech produces fixed feedback instead of an unusable open turn. Native private/text-only results receive a public acknowledgement without uploading their private content; full private speech keeps its separate Fish consent.

During a voice reply, the already-enabled wake service can recognize a narrow set of actual final playback controls: “wait, wait”, “stop talking”, “stop speaking”, “espera, espera” and “deja de hablar”. This mode rejects ordinary commands, low-confidence finals, phrases quoted in the current spoken segment, obsolete generations and credential-private states. It stops the current reply and opens a fresh follow-up window without treating voice as Android authentication. It never starts a disabled wake service or stores control audio/transcripts. Voice Lab has an opt-out. This remains subject to real microphone routing, available echo cancellation and acoustic conditions; it is not a guarantee of perfect full-duplex separation. The existing visible Stop control remains available.

Owner-confirmed wake greetings use the existing time-of-day boundaries without uploading saved names; uncertain/unknown voice identity receives a neutral greeting. A replaced/cancelled request also cancels its owned battery-alert watchdog, preventing an old timeout from stopping newer playback.

## Charging intelligence

`BatteryStateRepository` is the single battery broadcast observer. Existing HUD, wallpaper, Power Core and alert consumers share its state. It distinguishes discharging, actual charging, full, plugged-but-not-charging and unknown status; a plugged cable alone does not establish charging.

The repository requests `BatteryManager.computeChargeTimeRemaining()` off the main thread. Only positive bounded platform results are displayed. A missing, zero, negative, expired or unsupported estimate becomes unavailable. An isolated large jump needs a second consistent measurement. No fixed charge rate, battery capacity, charger wattage or invented ETA is substituted. A statistical fallback was deliberately not inferred from insufficient measurements.

The strip and native answers display approximate remaining duration and a local estimated finish time, including a date across midnight. Temperature, voltage, signed current and Android's health classification are shown when reported. Signed current is not relabeled as measured charger wattage or a capacity-health percentage.

Charging-start speech uses the actual percentage and available estimate. A later first reliable estimate can be announced once. Repeated unchanged broadcasts do not restart the session's announcement. Optional progress reports and disconnection speech are configurable. A known unplug ends the charging session; temporary unknown data does not invent a new plug-in.

## Critical and full-charge alerts

All observed levels 10% through 1% remain eligible while not charging. The new policy has explicit passing regression cases for 3%, 2% and 1%, plus the exact special English 1% phrase. Skipped readings such as 4% → 2% produce the observed 2% warning only. There is no burst replay of an unobserved 3% reading. A new lower level replaces obsolete pending battery speech.

Attempts, completed-level masks, discharge episode, charging session, snooze/dismissal and reminder deadlines are minimal battery metadata. No raw audio or chat is stored for this purpose. An interrupted or failed request is not labeled audible. Retries are bounded to three attempts for the same current item, with cooldowns and a text notification when permitted. A new more critical observation can still receive its own warning. The explicit Retry current pending alert button permits a user-requested fresh attempt without replaying successfully acknowledged levels.

Full-charge speech requires a connected, Android-confirmed charging/full state and observed 100%. The initial full announcement occurs once per session; the professional and “Power restored” variants are selectable. Repeating reminders default to fifteen minutes and offer five, ten, fifteen or thirty minutes, or disabled. Snooze pauses for fifteen minutes; dismissal lasts until unplugging. Repeated 100% broadcasts do not replay the initial speech. Overdue background work produces one current reminder, not a catch-up burst.

WorkManager schedules inexact one-time checks. Every check reads current battery state before an alert; unplugged, unavailable, dismissed or disabled states suppress it. Full/progress retry exhaustion stops rescheduling that failed item. Android may defer work in Doze or stop the process. This does not guarantee a 1% warning before shutdown or an exact background reminder time.

The same configured Fish Audio output pipeline handles English and Spanish interactions, battery warnings and charging announcements. No substitute voice, API key, private credential or forced volume override is embedded. Existing Silent/DND, quiet hours, safe call-state checks, audio focus, credential privacy and active user interaction take priority. Without the existing optional call-state permission, automatic spoken battery alerts fail closed and show their reason; normal direct battery questions still use the authorized response path.

## Wallpaper, HUD and animation

The actual Sentinel wallpaper has a compact green charging strip below the reactor. It shows current status, level and available ETA/finish time, then the clock/date and captions in portrait; landscape keeps clock/weather to the sides. A compact responsive layout and bounded caption line count prevent strip/clock/caption overlap in the tested geometry cases. Unknown charging data is labeled unavailable rather than presented as stale certainty.

The green layer uses its own accumulated frame-time clock, smooth velocity, fade and pulse. Speaking/listening changes cannot reset its angle. The core also keeps its phase through visible lifecycle changes. Hidden/surface-destroyed rendering pauses its clocks. Percentage colors remain independent: white at 70–100%, yellow at 31–69%, orange at 11–30%, red at 6–10%, and critical red `#FF1744` at 0–5%.

Charging rendering uses a more responsive cadence while retaining power-saver and reduced-motion policies. The native renderer reuses its paints and geometry. Actual frame-start gaps and CPU draw/post times are instrumented in a bounded in-memory buffer, labeled separately from GPU completion. The Power Core also reports actual alert-request-to-audio-start latency when measured on a phone; no fabricated device benchmark is included.

Existing Phantom presentation remains; battery questions update the relevant native panel/card without forcing the full activity open. “Open Power Core” explicitly opens the actual battery/settings interface. Existing charging, widget, speech, app/file, weather, memory, security and local-inference abilities remain.

## Phone steps

The changed component groups are `power/` (battery source, ETA/text, alert policy/store/audio gate, WorkManager/actions, native commands, charging visuals/layout and timing), `titan/BatteryAlerts.kt` and `BatteryPolicy.kt` (existing preferences/alert integration), `TitanCenterActivity.kt` and `VoiceIntelligence.kt` (Power Core and voice controls), `AssistantRuntime.kt` (fast routes, shared speech and cancellation), `HudRepository.kt`/`HudModels.kt` (shared measurements), `WallpaperRenderer.kt`/`JarvisWallpaperService.kt`/`SentinelModels.kt` (wallpaper and lifecycle), `JarvisHud.kt`/`ModuleHud.kt`/`PhantomOverlay.kt`/`RebirthCenterActivity.kt` (existing surfaces), `WakeListenerService.kt`/`PlaybackInterruptPolicy.kt` (narrow playback gate), the manifest/version files, and the battery/playback regression tests. The release commit provides the exact changed-file diff.

1. Install the signed 11.4 ARM64 APK over the matching newer 11.2/11.3 build. The package and signing identity are retained. Preserve the installed app/data if Android rejects the signature; do not uninstall or clear data to force the update.
2. Open **Settings → Rebirth → Power Core**, or say **Open Power Core**. Review battery voice alerts, charging-start speech, full-charge speech, repeat interval and optional progress/disconnection speech.
3. Review **Quiet hours** and use the existing **Optional call-state permission** button if you want safe automatic spoken alerts. Grant notification permission if you want text alerts. Android permission dialogs remain under your control.
4. Verify the English/Spanish Output voice preview with your existing Fish configuration. Missing credentials, quota, connectivity or blocked audio are reported; no alternate voice is silently substituted.
5. Apply the existing Sentinel live wallpaper through Android's wallpaper picker if it is not already selected. Charging information appears on that actual wallpaper when Android reports power connected.
6. Ask **Is my phone charging?**, **How long until full?**, **What time will charging finish?**, or **What's my battery temperature?** These use the native battery path. **Stop charging reminders** and **Remind me later** in battery context control the real reminder state.
7. Check **Hey Jarvis**, then **What time is it?**, a longer question, a pause, and the existing screen-lock countdown. Keep the existing lock-only administrator approval for actual screen locking. Both widget types remain under **Widgets & Jarvis panels**.

## Honest status

| Area | Status |
| --- | --- |
| Shared battery source, Android ETA, formatting, charging/native voice routes | Implemented; JVM policy tests and build checks recorded with release |
| Critical thresholds, retries, durable metadata, full/progress reminders and controls | Implemented; transition/restart tests recorded with release |
| 11.3 speech finalization, narrow playback interruption and visible reactor lifecycle | Implemented/preserved; automated policy regression checks; microphone/acoustic behavior needs phone verification |
| Wallpaper strip and geometry, independent green animation, Phantom battery card | Implemented; geometry/policy tests; handset rendering needs verification |
| Physical Redmi 9, actual charger/ETA, Fish account acoustic playback, background/OEM/Doze behavior | NOT TESTED in this cloud |
| Actual device speech latency, GPU/frame pacing and crash reproduction | Instrumented where applicable; NOT MEASURED on the Redmi 9 here |
| Statistical ETA fallback without reliable observations | Unavailable; no fabricated estimate |
| Perfect voice separation or voice-only Android unlocking | UNSUPPORTED |
| Gmail/Google Account Center | Remains removed from the current build as previously requested; not reintroduced by a preservation list |
| Remote Superdesign project synchronization | NOT TESTED; existing local visual identity retained |
| Full commercial-readiness, every older aspirational capability, universal OEM support | Not certified by this release |

See the APK's VERIFICATION.md, source-input hashes and build-info.json for the final exact test count, lint results, certificate, checksum and verification limits.
