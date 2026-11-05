# Jarvis11.2 beta3 · Lock countdown and widgets

[**Download signed ARM64 APK**](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/jarvis-next/downloads/rebirth-11.2-beta3/Jarvis-11.2.0-beta3-arm64-reinstall.apk) · [Setup and full scope](../../docs/rebirth-11.2/LOCK-AND-WIDGETS.md) · [Verification](VERIFICATION.md) · [Checksum](SHA256SUMS).

Say **“Lock my screen”** for **“Locking initiated, locking your screen in3,2,1.”**, followed by the actual Android lock after speech finishes. Android's lock-only administrator approval is required. **Cancel lock** or **Stop** prevents a pending lock. Speech failure uses a visible three-second countdown; no substitute voice or fake spoken success. Existing emergency stop-and-lock commands stay immediate.

Say **“Open my widgets”** or open **Settings → Rebirth → Widgets & Jarvis panels**. Every existing Jarvis panel remains available. Installed-app widget copies use Android's own binding/configuration approval. Up to16 approved widgets can be hosted, with provider search and remove/configure controls. Other launchers' private widget instances and hidden/work-profile data are not blanket access. No root, signature permission or automatic widget-content upload is added.

All prior beta2 features remain: live captions/chat, completion cleanup, local session notes, owner enrollment, strict Fish English/Spanish, aliases/memories/files and native panels. Gmail stays removed. Existing private configuration and signing identity are preserved.

**359 JVM tests passed**; lint zero errors/fatals, 43warnings. Android test-source compilation succeeded, no runtime instrumentation executed. Real Redmi9 speech, DPM lock transition, provider/widget bind/configuration/taps and performance are not verified here. This is a development beta, not a world-class accuracy or commercial-readiness certification.

Android10+/ARM64 · package com.jarvis.next · code18 /11.2.0-beta3-reinstall · 84,389,837bytes. SHA-256 `de72cf8947df46318a02389da0c2c526ab212baa7647c4be94c19d33c881dd2b`. Retained certificate `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. No new key or package change. Updates matching newer-certificate betas; original7.0 still needs its original key. Keep the installed app/data if Android rejects the signature.
