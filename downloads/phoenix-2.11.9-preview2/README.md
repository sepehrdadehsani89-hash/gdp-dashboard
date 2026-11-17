# Jarvis Phoenix Test — side-by-side Android preview

Version **2.11.9-phoenix.2-debug**, code **27**. Package **com.jarvis.next.phoenix**, ARM64, Android 10+ (API29), target API35.

[APK](Jarvis-2.11.9-phoenix.2-debug-arm64.apk) · [SHA-256](SHA256SUMS) · [Build receipt](build-info.json) · [Package-conflict repair](../../docs/jarvis/PHOENIX_PACKAGE_CONFLICT.md)

This preview fixes the previous evaluation APK's package/signature collision by using a separate debug application ID. It appears as **Jarvis Phoenix Test**. Keep your existing J.A.R.V.I.S. installation and its data. No uninstall or data clearing is required to accommodate this separate package.

The preview starts with its own Android app storage, settings, permissions and connections. Existing account or companion pairings are not automatically transferred. An in-place update of the original app still requires its original matching signing key, which is unavailable in this workspace. No existing signing material was replaced.

Provider authorities, the generated private receiver permission and the Spotify OAuth callback follow the preview's identity. Spotify's owner must separately register `com.jarvis.next.phoenix://spotify-callback` to authorize Spotify in this preview. Production OAuth behavior and package ID remain unchanged.

Contains the existing Phoenix core cadence, wallpaper surface recovery, optional debug counters and prior offline TTS helper repairs. Existing UI/artwork and unfinished-feature boundaries are documented in [the Phoenix milestone](../../docs/jarvis/PHOENIX_PROGRESS.md). This packaging repair does not complete Google sign-in, Silero VAD or the broader master specification.

The JVM regression run reports **677 passed, zero failures, one optional private-export fixture skipped**. No physical Android installation or emulator test has been performed. On-device installation and the reported rendering/audio behavior still require real device verification.

Build: **successful**. Debug lint: zero errors/fatals, 48 warnings, one information finding. Android-test Kotlin/Java compilation succeeded. **20 APK inspection checks passed**, including distinct package/provider/permission names, v2 signature, ZIP alignment/integrity and ARM64 architecture. All 134 native-library/asset files from the previous Phoenix APK are preserved byte for byte.

Size: **55,353,313 bytes**. SHA-256: `f49062fb7d2abfe14c208cb2befa222eea19d006eb4c1250ea12f83c3cd852e1`.

Source checkpoint: `f60ae0b2a59b695d14f90caf77bdaeef2351c279`. Previous APKs and rollback branches are preserved. Public download verification must follow an owner-approved GitHub push; these relative links describe the prepared artifact folder.
