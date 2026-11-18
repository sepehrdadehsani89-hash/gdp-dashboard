# Jarvis 5.2 preview for ARM64 Android

[Download the Phoenix preview APK](Jarvis-5.2.0-preview.1-debug-arm64.apk) · [Download computer companion 1.2 source](Jarvis-Companion-1.2-source.zip) · [Checksums](SHA256SUMS)

Download the APK on your phone, open it in Downloads and tap **Update** if Android recognizes your installation. If the installer rejects it, keep your existing app and storage intact. Follow the steps to capture the Package Manager error in the [troubleshooting guide](../../docs/jarvis/JARVIS_TROUBLESHOOTING.md).

| APK property | Verified value |
|---|---|
| Version | `5.2.0-preview.1-debug` (code 33) |
| Package | `com.jarvis.next.phoenix` |
| Architecture | ARM64 |
| Minimum Android | Android 10 (API 29) |
| Size | 60,514,307 bytes |
| APK SHA-256 | `cd1a5f9718763e0f2cf6d35402d5f9e503c41b100fa80fb402fb95175c832c76` |
| Signing certificate SHA-256 | `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7` |
| Source commit | `f042c211b0a0da351dc70116dfad01dd9f53553e` |

The package and signing certificate match the known 4.1 Phoenix preview. Version code increased from 32 to 33. Installation over the copy on your phone, settings retention, real camera operation and native Windows pairing still require an on-device check.

For pairing, extract the companion ZIP and open PowerShell in its `companion` folder. Use the existing Windows private state directory and the computer's current private IPv4 address. Follow the included `docs/jarvis/JARVIS_COMPUTER_PAIRING_GUIDE.md` through the compare, approve and finish steps.

The Android build passed. The JVM reported 2,569 passing tests and one optional skip. All 103 Linux companion tests and 35 APK checks passed. Physical Redmi and native Windows runs are pending. For live Google setup, voice, supported controls and limitations, see the [implementation report](../../docs/jarvis/JARVIS_5_2_REPORT.md) and [setup guide](../../docs/jarvis/JARVIS_SETUP_GUIDE.md).
