# Jarvis development reinstall build

[Download the ARM64 APK](Jarvis-10.0.0-dev-arm64-reinstall.apk) · [SHA-256](SHA256SUMS) · [Build identity](build-info.json) · [Public certificate](signing-certificate.pem)

**This is not an in-place update for the original Jarvis signature.** It retains `com.jarvis.next` but starts a new signing line. Android will reject installation over the original signed app. Do not uninstall until you have saved your private data export and independent private copies of the new signing backup and its separate unlock file.

Version: `10.0.0-dev-reinstall` / code 10. ARM64, Android 10 or later. This is a development checkpoint with tested modules and data import, not the complete OMNISYSTEM specification or an established commercial release. It contains no model weights or private credentials.

Uninstalling deletes app-private data. The old export restores only document text, approved memories, tasks and eligible app aliases. It does not restore conversations, credentials, settings, permission grants, model files or original document binaries. After reinstalling, import the private JSON under Settings → Neural Core → Import memories & approved documents. Review the preview and confirm. Reconfigure other settings and credentials privately.

Future APK updates must use this retained certificate and increasing version codes. An exported public certificate or an installed APK cannot recreate its private key. See [migration and signing-backup instructions](../../docs/omnisystem/REINSTALL.md) and [unfinished features](../../docs/omnisystem/ACCEPTANCE.md).
