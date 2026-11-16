# Frozen core: source audit, not a verified repair

Status: reported defect; runtime reproduction BLOCKED. No source fix applied.

## Actual rendering paths
- `app/src/main/java/com/jarvis/assistant/ui/JarvisHud.kt`: HudCore uses Compose Canvas; LaunchedEffect keyed by lifecycle and FPS, repeatOnLifecycle STARTED, withFrameNanos, elapsedRealtime and shared ReactorFrameClock. Updated speed/charging settings are read via rememberUpdatedState. Canvas observes angle/frame state.
- `app/src/main/java/com/jarvis/assistant/sentinel/JarvisWallpaperService.kt`: separate HandlerThread renders only with visible, ready, interactive surface. Observes runtime state, requests frames, uses hardware Canvas with software fallback, reschedules based on WallpaperRenderPolicy. Surface loss, screen state and visibility explicitly gate drawing. Exceptions/OOM have recovery counters/backoff.
- `WallpaperRenderer.kt` and `SentinelModels.kt`: shared geometry, accumulated rotation, pause clears previous timestamp, delta clamped to 250ms; no whole renderer replacement indicated by source.
- `CoreMotionPolicy.kt`: intentional zero FPS for Jarvis reduced motion, disabled Android animators, zero animation speed, nonfinite/zero intensity. Power saver/thermal/low battery lower the cap rather than inherently disable it. Native wallpaper has additional surface/visibility/screen gates.

## Evidence needed to distinguish causes
1. Record whether the freeze affects Activity, wallpaper or both, and capture real settings plus Android animator setting.
2. Use existing WallpaperRepairActivity frame counts, timing summary, pause reason and recovered errors before/after backgrounding and surface recreation.
3. Trace frame callbacks, observed state and render thread while frozen. Check renderer errors, system motion changes and effect restart/cancellation.
4. Capture original geometry/screenshots and consecutive frame differences. Measure FPS/drops on target hardware.
5. Repeat after any minimal patch with battery ETA/weather and live speech states preserved.

A frame loop existing in source does not prove on-device movement. No screenshot, video, Logcat, connected device, measured FPS, or runtime root cause is available. The hypotheses above are not confirmed defects. All 20 cases in TEST_EVIDENCE/ANIMATION_20_CASES.csv are BLOCKED, zero executed.
