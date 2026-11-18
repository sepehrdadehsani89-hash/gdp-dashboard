# Jarvis 4.1 debug preview

This is a locally built, signed debug preview. GitHub publication and verification of a new public download URL are pending owner approval. Redmi 9 update acceptance and live account operation have not been tested here.

- APK: [Jarvis-4.1.0-preview.1-debug-arm64.apk](Jarvis-4.1.0-preview.1-debug-arm64.apk)
- Source commit: `f7e384e920508fe291848e7d37ea5b81f116b60b`
- Package: `com.jarvis.next.phoenix`
- Version: `4.1.0-preview.1-debug`; versionCode **32** (known previous Phoenix preview: 31)
- Minimum Android: **10 / API 29**; target API **35**
- Architecture: **arm64-v8a only**
- Size: **56,644,226 bytes**
- APK SHA-256: `361872bb26f569aabb9bb13a686de97c6dbdc97fec839f625f552dd2abc68152`
- Signing certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`
- Signing certificate SHA-1: `64:71:1C:61:B2:AC:55:D4:78:E5:F9:22:80:F5:40:61:36:9E:E3:1B`

See [machine-readable metadata](artifact.json), [checksum](SHA256SUMS), [engineering report](../../docs/jarvis/JARVIS_4_1_REPORT.md), [Google setup instructions](../../docs/jarvis/GOOGLE_SETUP.md) and [raw verification evidence](../../docs/jarvis/TEST_EVIDENCE/JARVIS_4_1/).

## Actual verification

The final Android gate passed JVM tests, lint, APK assembly and Android-test compilation. **2,108 JVM cases passed, 0 failed and 1 optional private-export fixture was skipped** (2,109 total). Lint reported **0 errors, 56 warnings and 1 informational item**. The final companion suite passed **36 tests**, including a controlled Linux Chromium/local HTTPS fixture. All **30 APK SDK/ZIP/binary checks passed**. Earlier failures and interrupted gates remain in the evidence history.

Android instrumented tests executed: **0**. Physical installation/update, microphone, TTS, screen encoding, live Google/API accounts and Windows/macOS round trips were **NOT RUN**. A controlled fixture is not a real Google service or Redmi test.

All **134 previous native/asset files**, including all **14 native libraries**, are byte-identical to the known previous Phoenix APK. Original rendering sources, interface and application database schema were retained; handlers and settings entries were added. Details and unsupported capabilities are in the report.

## Updating without losing existing data

This APK retains the known previous Phoenix package and certificate and increments its versionCode. It is a candidate update for that same signing identity. The package and certificate actually installed on the owner's Redmi 9 have **not** been measured, so installation acceptance and data preservation are **not yet verified**.

The retained original production APK has a different package/signing identity. This preview cannot replace that application. Do not uninstall the existing app, clear storage or change package identity to work around a rejection. If Android rejects an update, preserve the existing installation and obtain its actual package-manager error and certificate before choosing a fix.

Google sign-in requires the owner's Google Cloud Android OAuth registration and service consent described in the setup instructions. Native Android permission dialogs remain mandatory; this APK does not silently grant microphone or private-data permissions.

## Google Play boundary

No production AAB is prepared. Original production signing access, release hardening, the retained Vosk library's 4 KB ELF alignment and current Google Play requirements remain unresolved. This debug preview is not represented as a verified production or Play-ready release.
