# Jarvis 2.11.6 — AI calls, voice focus and moving core

[**Download the signed Android APK**](Jarvis-2.11.6-arm64-update.apk) · [Phone guide](../../docs/v2/PHONE-GUIDE.md) · [Implementation and limits](../../docs/v2/REPORT.md) · [Verification](VERIFICATION.md) · [Checksums](SHA256SUMS).

Say “Hey Jarvis,” then “call me now,” “let's do a call,” or “call me.” A ringing call banner/screen and notification offer **Answer** and **Decline**. Answer hears the connection greeting, then ask questions or supported tasks without another wake word. **End call** stops that session and restores the prior wake-listening choice. This is an in-app AI conversation, without a phone number or carrier call. General answers need a configured secure AI backend or installed local model.

Also adds editable dated check-ins and 20% battery call invitations, temporary first-speaker focus using the optional speaker model, fixed public-greeting warmup/cache, safer output-error reporting and a real live-wallpaper inspection/repair screen. The existing core design moves with actual assistant/audio states and adaptive frame targets. Earlier charging/widgets/panels/privacy/owner training/Fish voice controls remain. Gmail remains removed. First-speaker filtering is not authentication or perfect separation of overlapping voices.

Install over a matching newer Jarvis build: package `com.jarvis.next`, displayed version **2.11.6**, version code **23**, Android 10+ / ARM64, 85,300,261 bytes. The retained newer signing certificate is unchanged. **If Android rejects the signature, preserve the installed app and data; do not uninstall or clear data to force it.** No replacement signing key was generated.

Software checks: **527 JVM tests passed**, **30 companion tests** and **4 catalog-integrity tests** passed; lint **0 errors/fatals**, 48 warnings. Android instrumentation compiled only. **No phone tests ran**: Redmi 9 microphone, noise/voice accuracy, Fish acoustic output/latency, locked/background pickup, actual alarms/battery thresholds and measured FPS still need hardware acceptance. The 1,500-feature master remains a staged backlog, not a completed commercial product.

Optional paired computer milestone: [download companion source ZIP](JarvisCompanion-source-2.11.6.zip) and follow [Windows/Linux/macOS setup](../../companion/README.md). Explicit private-LAN pairing permits Chrome open/search only, with verified receipts. The real Linux HTTPS/Chromium fixture passed; no Windows EXE, macOS installer or Google/phone round-trip is claimed.

APK SHA-256: `c2be710b6e605661a276eca67ff7c44297ba51b3631ff2e2b14c9304bf7ab5e8`.
