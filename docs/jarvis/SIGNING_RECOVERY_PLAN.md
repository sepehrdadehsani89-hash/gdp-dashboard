# Signing recovery boundary

The owner reports the original keystore is unavailable or uncertain. Jarvis is not published on Google Play. No new key, upload key, signing identity, application ID migration or signing-guard bypass is authorized by this checkpoint.

The retained Phoenix debug signer can update the previous Phoenix preview with increasing version code. It cannot update the original production package, because that package has a different certificate. Changing a download URL, version name or APK filename cannot change this Android signature requirement. Uninstalling the original app or clearing its data is not a recovery strategy.

For the original production package:

1. Locate the existing authorized keystore and its credentials in the owner's secure backups/build environment. Do not paste these into chat or commit them.
2. Compare its public certificate fingerprint with the repository's `checkNextSigning` guard before supplying the private signing directory through the existing local environment configuration.
3. Only after that identity is verified, migrate/test target API and native compatibility and produce the signed production AAB/APK. Existing data and install identity must remain intact.
4. If the original app-signing key is permanently lost, stop before changing production identity. A separate, explicitly approved migration/publication plan is needed. Because there is no existing Play listing, an upload-key reset for an enrolled Play App Signing app cannot be assumed to recover this original sideloaded signing identity.

A public certificate in an APK is not the private signing key. The tested debug preview is not a substitute Play production signing configuration. The current signing guard's exact execution result is retained in the stabilization evidence.
