# Jarvis 3.0 — Sentinel

Sentinel adds a genuine native Android live wallpaper and coordinated command center to the existing app. The launcher name remains **Jarvis**. The package remains `com.jarvis.next`, version 3.0.0 / code 3, signed with the retained Jarvis Next key. It updates Jarvis Next 0.1.0 and Jarvis 2.0 normally. Do not uninstall either the separate installation or the original Jarvis (`com.jarvis.assistant`). Original Jarvis and its private data remain separate.

## The 10-second conversation rule

Enable wake listening explicitly and install the speech model for your selected language. Say **Hey Jarvis**, **Oye Jarvis**, or **هی جارویس** once. A command window opens for at most **10 seconds**. Repeated wake detections and partial speech do not extend it. Results arriving after expiry cannot submit a command. Long commands that have not been accepted by the deadline must be repeated after a new wake phrase or entered with the keyboard.

After each completed spoken answer, a fresh 10-second follow-up window opens once the microphone and short playback echo guard are ready. Ask your next question directly: **another wake phrase is not required inside that window**. Each accepted question can lead to another reply and another 10-second window. An accepted AI request or spoken answer may take longer than 10 seconds; its processing/playback time does not consume the next follow-up window.

When a window expires, Jarvis returns to **wake-word-only detection**. The explicitly enabled foreground microphone service continues local wake detection so it can hear the next wake phrase; ordinary background conversation is not accepted as commands. Its notification remains visible as Android permits. Stop disables that service and closes its command gate immediately. Cancellation, calls and recognition/playback errors do not silently reopen a command window. Manual mic capture is also capped at 10 seconds.

Recognition uses one selected-language model, with existing aliases. It does not run three language models simultaneously or pretend an English model understands every language. Compatible models, permission and a suitable Android audio route are required. Spoken Codex is only a control trigger, never proof of identity.

## Apply the wallpaper

Open **Settings → Sentinel · wallpaper & sound → Preview / apply Jarvis wallpaper**. Android displays its own live-wallpaper preview and confirmation. Choose the destination offered by the phone. Lock-screen selection depends on Android/Xiaomi support. Previewing or selecting the wallpaper never enables the microphone.

Normal launcher icons and navigation remain available. The wallpaper receives no touch events that could steal launcher gestures. Tap the reactor, battery, weather, connectivity and transcript inside the **interactive assistant activity** for voice, details and conversation controls. Complex controls and protected actions stay inside the activity.

The wallpaper is rendered by `WallpaperService.Engine` using native Canvas, with a hardware Canvas when supported and a software fallback. It is not a video. It shares real telemetry, audio state, settings, geometry and activation timing with the Compose HUD. Android wallpaper and activity windows are separate surfaces; this uses matching artwork/timing rather than claiming an unsupported cross-window shared-element transition.

## Dormant, awakened and returning

Dormant mode uses a black background, faint cyan grid, restrained rings/particles, optional phone clock/date, actual battery and optional cached weather. Charging adds a gentle inward flow; full charge calms it again. Cyan, blue and purple accents, size, speed, density, detail, wallpaper brightness, glow and minimal/cinematic styling are adjustable.

A wake event starts an approximately 2.1-second sequence: core ignition, sequential rings, radar/particle motion, panel reveal and a finishing pulse. Command capture is independent and is never delayed until the animation finishes. Actual listening, processing, speaking, authorization and error states drive the visuals. A wake-opened HUD can return to the wallpaper after inactivity; manually browsing its controls keeps it open. Dismissal retains saved history and settings.

Automatic opening is attempted only on the home screen where Android permits it, without moving another app's live-control target. Otherwise, tap the activation notification. Accessibility access is not requested merely to bypass background-launch restrictions. The app does not unlock the phone, replace its launcher or draw an unrestricted overlay over other apps.

## Real data and audio

- Battery percentage/status, charging sources, temperature and health come from Android. Missing readings remain unavailable.
- Time/date use the phone's clock, timezone and format, with minute/time/date/locale observation rather than frame-by-frame clock polling.
- Weather uses Open-Meteo current conditions, observation timestamps and cache/stale/error states. Set a city or explicitly request approximate location once. The wallpaper never requests location by itself. Wallpaper weather is off by default; enabling it opts into the existing reasonable refresh interval while visible. Weather questions can use your configured city and update the same displayed reading. Explanatory questions about weather models still go to the AI.
- Network transport/validation, system memory, this app's PSS and local-model availability remain actual readings. A saved key is not described as a verified provider connection. CPU load and bandwidth are not invented.
- Input waveforms use actual PCM RMS or recognizer RMS callbacks. Neural output uses submitted PCM bins selected by the real AudioTrack playback cursor. When playback measurements are unavailable, the speaking animation is explicitly labelled decorative. It does not use queued future synthesis as a current output meter.
- Voice selection, pitch, rate and volume remain configurable. Only installed voices/models are used. System pitch defaults to 0.90; neural model pitch follows the chosen model. Concise versus detailed response settings change trusted model instructions; they cannot guarantee a model's exact word count.

## Original sound design

There are 26 original, deterministic FM/sine sound cues for activation, reactor boot, ready/listening, recognition/processing/response, speaking, buttons/tabs/panels, task completion, permissions, Codex/identity/results, network changes, battery/charging, error and sleep. They are pre-rendered mono PCM assets loaded once into SoundPool, with bounded concurrent playback and no endless ambient loop.

Effects have an independent volume, activation/shutdown choices, on/off control and previews. Silent mode, Do Not Disturb, calls, audio focus and command capture take priority. Effects can be ducked or skipped rather than covering speech. Spoken playback suppresses wake processing for its full duration plus a short echo tail; visual effects never grant microphone access. Global Stop cancels queued activation cues and stops output.

The assets and generator are original and CC0, without movie recordings or sampled effects. See `scripts/generate-sentinel-sounds.py` and `assets/legal/SENTINEL_SOUND_LICENSE.txt` (also included in the app's licenses dialog). Physical sound quality still needs listening on the phone.

## Performance and lifecycle

Cinematic and balanced active rendering are capped at 30 and 20 FPS; dormant rendering at 3–6 FPS. Eco/power-saving/low-battery modes reduce this further. Reduced motion, Android animator disable and zero animation speed suspend decorative loops. Actual data changes can still redraw a static surface. These are caps, not promises of measured Redmi 9 performance.

A hidden, destroyed or screen-off wallpaper surface stops drawing. Its visible-state collectors and telemetry/weather interests are removed. Multiple preview/home/activity owners share one telemetry repository rather than creating duplicate battery/network observers. Native paints, geometry and bounded shaped-text caches are reused; there is no full-screen blur or video decoder. Playback sampling has its own short-lived worker, which is closed with its AudioTrack.

## Preserved features and security

Existing cloud/local AI, streaming responses, encrypted provider keys, Room history/memories, document/photo analysis, OCR, screen capture, connected read-only folders, app opening and target-bound taps/draft edits remain. Send remains manual. Memory storage/schema and model paths are unchanged; Sentinel preferences are additive. Local Qwen still requires its explicit optional model download and does not reset Google's quota.

Explicit play/pause/next/previous media requests use Android's supported media-key dispatch after approval. The result reports what Android exposes and does not pretend a track changed when that cannot be verified. Calendar reminders open a reviewed draft for the user to complete. Existing Codex device-credential/biometric confirmation and permission/approval mechanisms remain; the spoken word alone authorizes nothing.

New Android components are `JarvisWallpaperService` and the thin system wallpaper-settings entry activity. The service is exported for Android discovery and protected by `android.permission.BIND_WALLPAPER`. No new dangerous runtime permission is added. The app does not request silent wallpaper-setting privileges.

## Files and delivery

New implementation is under `app/src/main/java/com/jarvis/assistant/sentinel/` (shared models/geometry/controller, native renderer/service, sounds, playback sampling/queue, weather-question policy), `ui/SentinelSettingsUi.kt`, and `voice/ConversationWindow.kt` / `RecognitionConfidence.kt`. New resources include wallpaper metadata, translated picker descriptions, 26 WAVs and their license. Existing runtime, activity, HUD/settings/actions, speech/wake modules, weather/preferences, AI protocol/prompt builders and device tools are updated. Unit and Android instrumentation coverage is extended.

The signed ARM64 release uses the same retained certificate:
`56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`.

The private keystore stays at `/workspace/private/jarvis-next/`, outside Git. Its encrypted backup is `/workspace/private/jarvis-next-signing.tar.enc`, with separate unlock file `/workspace/private/jarvis-next-backup-password`. Copy both privately outside this cloud environment and store separately. Recovery uses AES-256-CBC with PBKDF2, **200000 iterations**, and the gzip tar archive; see the retained [recovery instructions](JARVIS_2.md#signing-and-private-recovery). Never generate a replacement key for an installed `com.jarvis.next` app.

The [Sentinel verification record](docs/JARVIS_SENTINEL_VERIFICATION.md), [build identity](downloads/sentinel/build-info.json) and [SHA-256 checksum](downloads/sentinel/SHA256SUMS) accompany the signed [ARM64 APK](downloads/sentinel/Jarvis-3.0.0-arm64.apk), with tested and untested hardware cases. Neither app naming nor these changes establish Play Protect approval or alter Google's HTTP 429 quota.
