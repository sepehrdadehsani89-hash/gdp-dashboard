# Animation diagnosis

No renderer source change this milestone; reported freeze remains unconfirmed on a render surface. See existing ANIMATION_CORE_DIAGNOSIS.md and original 20-case BLOCKED matrix.

Activity: ui/JarvisHud.kt uses Compose Canvas, STARTED repeatOnLifecycle and withFrameNanos with state-backed angle/time. Wallpaper: sentinel/JarvisWallpaperService.kt uses a dedicated HandlerThread, visibility/surface/screen gates, renderer pause and state observation. Frame clocks and power/thermal policies have JVM coverage, not measured handset FPS evidence.

Further source finding: both paths gate movement on ValueAnimator.areAnimatorsEnabled(). Android officially documents that this can be false either for explicit zero animator scale or Battery Saver. CoreMotionPolicy checks this zero-motion gate before its lower-FPS power-saver branch. This explains a possible intentional stationary state; it does not identify the owner's actual freeze. Do not remove Android reduced-motion/accessibility preferences based on this hypothesis. Source: https://developer.android.com/reference/android/animation/ValueAnimator#areAnimatorsEnabled().

Next runtime evidence: identify Activity vs wallpaper, capture actual settings/visibility/frame counters and privacy-safe errors, compare consecutive frames, reproduce background/foreground and power-saver transitions. No fabricated Logcat or screenshot is supplied. UI geometry, colors, wallpaper, particles and saved motion settings remain unchanged.
