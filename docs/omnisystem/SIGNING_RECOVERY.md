# Private recovery of the existing Jarvis update identity

The current tools cannot attach or restore a different environment by reference. The old environment reference in the handoff is not proof that its filesystem is retained. No current private signing files were found. The existing signed APK contains only the public certificate; it cannot recover the private key.

If the product still lets you open the original environment, copy these files through a private workspace/file transfer:

- `/workspace/private/jarvis-next-signing.tar.enc`
- `/workspace/private/jarvis-next-backup-password`

Otherwise privately transfer your saved copies to those same paths in the current workspace. Use the product's private file upload/workspace transfer when available. Do not paste either file's contents into chat, commit them, put them under downloads, or publish a link. Keep the unlock file separate in long-term storage. An encrypted archive still needs private handling.

Set the private parent directory to mode 0700 and the two files to 0600. Decrypt with OpenSSL using the password **file**, never a command-line password:

```sh
umask 077
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -in /workspace/private/jarvis-next-signing.tar.enc \
  -out /workspace/private/signing.tar.gz \
  -pass file:/workspace/private/jarvis-next-backup-password
```

Before extraction, inspect archive paths locally. Refuse absolute paths, traversal, symlinks/hardlinks or unexpected files. Extract only the original `jarvis-next.p12` and `store-password` into `/workspace/private/jarvis-next/`. Do not overwrite an existing key. Remove the temporary plaintext archive after successful recovery. Never print the password files.

Verify with `keytool -list -v -keystore /workspace/private/jarvis-next/jarvis-next.p12 -storetype PKCS12 -storepass:file /workspace/private/jarvis-next/store-password -alias jarvis-next`. The public certificate SHA-256 must be:

`56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`

The release Gradle guard also rejects missing files, missing alias or a different certificate. Set `JARVIS_NEXT_SIGNING_DIR=/workspace/private/jarvis-next` only after this verification. Do not generate a replacement key. Debug/emulator artifacts cannot update the installed release and must not be presented as a phone upgrade. Do not uninstall the installed app or clear its data to work around a missing key.
