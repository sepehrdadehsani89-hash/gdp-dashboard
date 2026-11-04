# Jarvis11.2 Rebirth beta1

[**Download signed ARM64 APK**](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/jarvis-next/downloads/rebirth-11.2/Jarvis-11.2.0-beta1-arm64-reinstall.apk) · [Phone setup and changes](../../docs/rebirth-11.2/RELEASE.md) · [Verification](VERIFICATION.md) · [Checksum](SHA256SUMS) · [Build properties](build-info.json).

Gmail account integration is removed. This beta repairs the locked-screen wake-greeting follow-up policy, guards stale playback/focus callbacks, bounds audio start/playback and uses the existing limited recovery budget for a transient silenced recorder. It retains live-only ordinary chats, saved settings/aliases/memories and strict Fish speech.

**338 JVM tests passed**; lint zero errors/fatals, 42warnings. Android test sources compiled; no runtime instrumentation or physical Redmi9 test ran. No real Fish provider sound is verified here. This is a development beta, not a promise that every reported speech fault is resolved or the full master directive is complete.

Android10+/ARM64, package com.jarvis.next, code16 /11.2.0-beta1-reinstall. 84,281,789 bytes. SHA-256 `7b05e85ed65c769b70584a32f9b1fed6d25911d9f453885b50a85178290401e0`. Existing certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`; no new key.

Updates compatible retained-certificate installations only. Original Jarvis7.0 has a different signer and cannot accept this APK in place without its original private key. Keep the installed app/data if Android reports a signature conflict; do not uninstall or clear data as a workaround.

For ongoing public locked-screen conversation, review **Settings → Memory & locked-phone replies → General chat while locked** and save your choice. The update preserves that setting. The wake greeting opens one bounded command window even with that option off, but general/private requests then explain the unlock requirement. Phone actions and personal data still require Android unlock. Start wake listening while Jarvis is visible and confirm its microphone notification. No Google/OAuth/Gmail setup is required.
