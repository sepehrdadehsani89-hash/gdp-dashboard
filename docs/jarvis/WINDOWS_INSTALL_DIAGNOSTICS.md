# Capture the actual Jarvis update failure on Windows

This procedure diagnoses the existing rejected APK. It does not present it as a corrected release. Keep the existing Jarvis installed and retain its application data. The normal `adb install -r` operation requests an in-place update; Android still enforces signing, version, SDK and ABI rules. A failed update is not a reason to uninstall or clear Jarvis.

## Prepare the connection

1. Download **SDK Platform-Tools for Windows** from [Google's official page](https://developer.android.com/tools/releases/platform-tools) if you do not already have ADB. Extract the ZIP; no Android Studio or emulator is needed for these commands.
2. Copy the existing rejected `Jarvis-2.11.9-stabilize.1-debug-arm64.apk` into the extracted `platform-tools` folder beside `adb.exe`. This is the APK already supplied, not a new corrected APK. Use its actual filename if Windows added `(1)` or another suffix.
3. On the Redmi 9, enable Developer options by tapping MIUI version in About phone seven times if needed, then enable USB debugging in Additional settings → Developer options. Connect the phone by USB, unlock it and approve its USB-debugging trust prompt for your own computer.
4. Open PowerShell in the `platform-tools` folder. Run:

```powershell
.\adb.exe devices -l
```

Continue only when the intended Redmi 9 is listed as `device`. For `unauthorized`, approve the phone's trust prompt. If there is no device, check the USB cable/connection and the appropriate manufacturer driver. If more than one device is present, add `-s YOUR_REDMI_SERIAL` immediately after `adb.exe` in subsequent commands; do not select another phone accidentally.

## Check the exact candidate and phone metadata

```powershell
Get-FileHash -Algorithm SHA256 ".\Jarvis-2.11.9-stabilize.1-debug-arm64.apk"
.\adb.exe shell getprop ro.product.model
.\adb.exe shell getprop ro.build.version.release
.\adb.exe shell getprop ro.build.version.sdk
.\adb.exe shell getprop ro.product.cpu.abilist
.\adb.exe shell pm path com.jarvis.next.phoenix
.\adb.exe shell dumpsys package com.jarvis.next.phoenix | Select-String 'versionCode=|versionName=|minSdk=|targetSdk='
.\adb.exe shell df -k /data
```

The candidate SHA-256 must be:

```text
ad796f1920f4853ff855acd710d1360bfa32a1e185ca733d4850c071ab7261ed
```

If it differs, stop before installing that file and report the actual hash and file size. A matching cloud copy does not establish that the phone or PC downloaded the same bytes. If `pm path` finds no package, report that result; do not change the APK's application ID or uninstall another Jarvis.

## Optional: preserve the installed APK's public code for comparison

Before attempting the update, these commands copy only its installed base APK to the PC. They do not copy private settings, accounts, provider keys or application data. Use a fresh filename if this output file already exists.

```powershell
$jarvisBase = .\adb.exe shell pm path com.jarvis.next.phoenix | Where-Object { $_.Trim().EndsWith('/base.apk') } | Select-Object -First 1
if ($jarvisBase) {
    $jarvisBase = $jarvisBase.Trim().Substring(8)
    .\adb.exe pull "$jarvisBase" ".\jarvis-installed-base.apk"
    Get-FileHash -Algorithm SHA256 ".\jarvis-installed-base.apk"
}
```

The previous published Brain Manager APK has SHA-256 `ecd1fd92c6360d427a4ddbf7368066a14475ab9b4c7dab4fc846a2bf36f32698`, code 29, package `com.jarvis.next.phoenix`, and signer `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`. If the copied APK hash differs, its signer is still unverified; the filename/icon is not signing evidence. If you already have Android SDK Build-Tools and Java available, use `apksigner.bat verify --verbose --print-certs ".\jarvis-installed-base.apk"` to report its public certificate SHA-256. Installing a complete SDK on Windows is not required just to capture the install error below.

## Make one normal update attempt and retain the exact result

Only after the intended device and APK hash have been checked:

```powershell
$jarvisLog = ".\Jarvis-install-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".txt"
.\adb.exe install -r ".\Jarvis-2.11.9-stabilize.1-debug-arm64.apk" 2>&1 | Tee-Object -FilePath $jarvisLog
```

Send the complete `Failure [...]` line, including the explanation inside the brackets, plus the installed versionCode, Android SDK and ABI output. The log file preserves the actual result; do not convert the generic dialog into a guessed failure code. No uninstall, data-clear, downgrade, signature-bypass or automatic retry command is used.

| Actual result | Evidence needed for the next repair |
| --- | --- |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Installed public signing certificate/lineage and exact package; recover the authorized identity if it differs |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Installed full versionCode; build a genuinely newer version with the same identity |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | Actual OS ABI list and all native dependencies; do not assume hardware model guarantees a 64-bit Android userspace |
| `INSTALL_FAILED_OLDER_SDK` | Actual Android SDK and existing APK requirements; assess code/dependency support before lowering any declaration |
| `INSTALL_FAILED_INVALID_APK` | Entire detailed reason and actual file hash/size; inspect the specifically rejected structure |
| `INSTALL_FAILED_MISSING_SPLIT` | Installed and candidate split metadata; supply a complete compatible package |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Measured internal free space; preserve Jarvis's data/models while addressing storage |
| `INSTALL_FAILED_USER_RESTRICTED`, verification or ownership errors | Exact device policy/permission explanation; use supported owner/system controls |
| `Success` | Android accepted this update through ADB. Then verify actual retained data and runtime behavior before reporting acceptance complete |

If it succeeds, open the matching package explicitly:

```powershell
.\adb.exe shell am start -n com.jarvis.next.phoenix/com.jarvis.assistant.MainActivity
```

Verify existing name/settings, provider connection, models, microphone, TTS, wallpaper, Reduced Motion, device controls and startup behavior. Record real observations using [device acceptance](STABILIZATION_DEVICE_ACCEPTANCE.md). A successful ADB installation alone does not prove those runtime or data-preservation checks.
