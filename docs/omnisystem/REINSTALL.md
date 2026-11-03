> This records the earlier code-10 artifact and retained signing/migration process. The same private key is reused for the later [code-11 beta](../../downloads/beta1/README.md); see [current verification](BETA_VERIFICATION.md) for its actual evidence.

# Reinstall signing line

This development build keeps the application ID `com.jarvis.next`, but uses a new retained signing identity because the original private key remains unavailable. It cannot install over the original signed Jarvis. The original `release` build still requires the original certificate; its guard has not been weakened. The separate `reinstall` build type is non-debuggable and unminified for the existing JNI ABI, with version label `10.0.0-dev-reinstall` and version code 10.

This is a development checkpoint, not the completed OMNISYSTEM specification or a commercially validated release. See `ACCEPTANCE.md` for unfinished features and physical-device limitations.

## Artifact verification

The ARM64 APK is 83,020,316 bytes, SHA-256 `5fe47b1e37785bc212ae0ad251b17371af78ec4889c8c6cac49e41c808b511ad`. APK Signature Scheme v2 verification and four-byte ZIP alignment passed. The package/version and ARM64-only native libraries were inspected; no private export, keystore, signing backup or GGUF weights are included. Reinstall lint passed with zero errors and 28 warnings. The unchanged importer application source passed 220 JVM tests and four actual Android database/preferences restore tests before the signing variant was added.

The complete file-picker/confirmation/cancel/retry UI check has not passed. Earlier attempts encountered a System UI ANR and test-driver scrolling/navigation issues. The driver was corrected and compiled, but a separate disposable emulator's Android package service crashed during APK installation, preventing the final recheck (`/workspace/android-toolchain/reinstall-single-install.log`). Parser and database checks do not establish end-to-end UI correctness. Do not treat this blocked check as a pass or uninstall the phone app before migration preparation is complete. No physical Redmi 9 reinstall or update has been tested.

## Preserve data before migration

Do not uninstall until the signed APK, the private export and an independent signing backup have been saved and the import checks reviewed. The existing private export preserves document text, approved memories, tasks and aliases only. It does not preserve conversation history, credentials, settings, permission grants, model files or original document binaries. An alias is restored only if its target app is available. Uninstalling removes old app-private data; this cannot be undone by the partial export.

After an explicitly chosen reinstall, open Settings → Neural Core → Import memories & approved documents, select the saved JSON, review counts and confirm. Re-enter credentials privately, reconfigure settings, and reinstall optional models separately. Do not publish the export.

## Retained identity for future updates

Public certificate SHA-256:

`1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`

The new private directory is `/workspace/private/jarvis-reinstall/`; the PKCS12 file is `jarvis-reinstall.p12`, alias `jarvis-reinstall`, and the adjacent `store-password` holds its password. Directory permissions are 0700 and private files 0600. These files must never enter Git or an APK.

The encrypted backup is `/workspace/private/jarvis-reinstall-signing.tar.enc`; its separate unlock file is `/workspace/private/jarvis-reinstall-backup-password`. An initial decrypt-and-byte-compare check verified both archive members against the retained files. Store independent private copies outside this cloud workspace, with the unlock file kept separately. A workspace copy alone is not a durable backup. Never paste passwords into chat or publish backup links.

For recovery, decrypt privately with AES-256-CBC, PBKDF2 and 200000 iterations using OpenSSL `-pass file:/private/path/to/unlock-file`. Validate archive members before extraction, restore only `jarvis-reinstall.p12` and `store-password`, and verify the public certificate fingerprint. Do not generate another key for future updates.

Build future updates with an increasing version code, the same package ID and this retained identity:

```sh
JARVIS_REINSTALL_SIGNING_DIR=/private/path/to/jarvis-reinstall ./gradlew :app:assembleReinstall
```

The dedicated `checkReinstallSigning` task checks the key alias and pins the certificate fingerprint. The original `checkNextSigning` task continues to reject this new identity for original-line releases. Existing customers with the old signature cannot accept this new line as an in-place APK update.
