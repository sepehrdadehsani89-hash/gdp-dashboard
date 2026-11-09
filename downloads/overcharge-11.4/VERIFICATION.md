# Jarvis 11.4 verification

The final frozen-source pipeline completed successfully in 4m 51s:

`:app:testReinstallUnitTest :app:lintReinstall :app:assembleReinstall :app:compileDebugAndroidTestKotlin`

## Automated evidence

**429 JVM tests passed**, zero failures/errors/skips. The prior 376 tests remain. Forty-eight new Overcharge cases cover every observed low-battery threshold with explicit 3%/2%/1% tests, skipped readings, failed playback, durable cooldowns, retry exhaustion, restart restoration, unknown data, charging/full distinctions, initial/repeat/full snooze/dismissal, unplugging, overdue work, disabled controls, optional progress, unavailable/changing/stale/outlier estimates, cross-midnight local formatting, quiet/call/silent/DND policy, native commands, responsive strip geometry, independent charging motion and bounded timing diagnostics. Five more tests cover narrow playback controls, device-action rejection, self-echo text exclusion, already-enabled-service/voice/privacy gating, and obsolete or completed callbacks.

These are policy and transition tests. They do not count simulated values as real Android charger, microphone, sound, screen-lock or GPU results. Android test sources compiled, but **zero Android runtime/instrumentation cases executed**. Lint: zero errors/fatals, 44 warnings. No new lint baseline or suppression was used to force acceptance.

## Artifact checks

Package `com.jarvis.next`, code 20, name `11.4.0-reinstall`, non-debuggable ARM64, min SDK 29 / target 35. V2 signature and four-byte ZIP alignment passed. Retained certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. APK SHA-256 `ae3a9297fbef1111cd7798e39ec8f3eb7dee4a8b2367167a7b007eec7a6305f0`, 84,509,889 bytes. Added merged permissions against 11.3: []. The new notification-action receiver is internal/non-exported. No new microphone/foreground service, broad file/account grant, replacement key or embedded credential was added.

APK inspection excludes private signing/password archives, private memory exports, model weights, unit-test classes and removed Gmail/auth integration. The optional local model and speech-model download/inference abilities remain; their weights are not silently embedded. The 11.3 APK remains byte-identical (`b88dea0ec09abab410ca87fdf47b45e4e211e2e6c527802fdc00a6c23bfc4878`), and older artifacts are preserved.

All 1560 final production/test/build input hashes match: [source manifest](source-inputs.sha256), SHA-256 `1a8d4530f18fa9aca7f36ecb3af943bb92c497da7881dfad80ad90629ab58ff6`. Source base: `a91679338f4ea1af7681125455ad8344426a24f3`. The release commit contains the frozen source and APK. Publication is followed by a complete immutable public URL GET with independent size/SHA-256 verification; protected main/download branch tips are checked separately.

## Source/lifecycle review

The previous low-battery code reserved a level before speech completion. The new policy separates durable attempts from actual completed playback, retains only the latest observed critical level, and bounds retries. A replaced session cancels its battery-alert watchdog; stale callbacks cannot stop a newer answer. System alerts wait for a ready interaction and respect existing safe call-state, quiet-hours, Silent/DND, audio focus and credential-privacy checks.

One shared battery observer feeds alerts, HUD, wallpaper and native questions. Only valid platform ETA measurements are used; unknown or expired values are not shown as exact certainty. WorkManager reminders always re-read actual state and can be delayed by Android. Repeated broadcasts, skipped thresholds and process recreation do not create announcement bursts. No raw conversation/audio is persisted for alerts.

Playback interruption reuses only the already-enabled authorized wake microphone. Its local recognizer accepts a narrow set of final speech controls after confidence filtering, excludes phrases present in the current output segment, and binds the callback to the currently speaking generation. Ordinary commands cannot dispatch from that mode, and voice is not Android authentication. Existing context/follow-up rules remain after interruption. This is not a claim of perfect separation of simultaneous voices or loudspeaker echo.

The green layer accumulates actual visible frame deltas, fades/accelerates without phase resets, and remains separate from the reactor's speech state and percentage color. Surface/visibility changes pause clocks. Responsive geometry reserves charging/clock/caption space. Real frame-start gaps and CPU draw/post durations are recorded only when native frames actually render; the report explicitly does not equate these to GPU completion.

## Not tested or unsupported

Actual Redmi 9 charger measurements, platform ETA accuracy, Fish account acoustic playback, background/locked-screen microphone routing, owner accuracy, interruption under real echo/TV/music, device-admin lock transitions, widget-provider behavior, launcher layout, Doze delivery and device performance remain **NOT TESTED** here. No physical phone or available Android runtime was attached. Existing 4 KiB native ELF/Android 16 KiB-page limitations remain; commercial or universal OEM readiness is not certified.

A statistical ETA fallback was not fabricated from insufficient evidence. Voice-only Android unlocking and perfect speaker isolation remain unsupported. Gmail/Google Account Center stay removed under the earlier explicit request; a preservation list did not restore a removed integration. The remote Superdesign project was not accessed through a connected design tool; existing local artwork was retained. Source audit inspected the app's non-sensitive crash-diagnostic implementation, but no phone crash history was available to establish an OEM-specific root cause.

See [implementation, changed component groups and phone checks](../../docs/overcharge-11.4/IMPLEMENTATION.md).
