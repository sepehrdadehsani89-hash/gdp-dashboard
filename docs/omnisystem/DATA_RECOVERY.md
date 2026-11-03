# Private data import preparation

The user-provided `jarvis-neural-export-v1` export has been validated privately. Its contents and the export file must never be committed, bundled into the APK, or uploaded to GitHub. A checksum-verified workspace copy is retained outside the repository under `/workspace/private/jarvis-data-backup/` with private permissions. This is not a durable backup outside the cloud environment.

## Import behavior

Neural Core now offers **Import memories & approved documents** beside the existing export control. Android's file picker selects a local or user-authorized document provider file. Parsing completes before any data writes; a confirmation dialog previews collection counts and limitations. Canceling the picker or confirmation imports nothing. Import does not start the microphone, download a model, or send backup contents to a cloud AI provider.

- Only the versioned private-memory format is accepted, with bounded file, collection, field and text sizes. Wrong types, unsupported document kinds, duplicate document identifiers, invalid timestamps and invalid package names are rejected.
- Documents are added with deterministic import identifiers and treated as user-provided references, including documents originally exported from an offline knowledge pack. Existing documents are not overwritten. Reimporting the same backup skips its already imported documents. Original passage text, including legacy export newlines, is retained.
- New tasks are added only if identical text does not already exist, including completed tasks.
- Approved memories retain their exported timestamps and are deduplicated against stored memory text.
- Aliases are restored only for currently visible launchable applications, within the existing alias limit. Existing alias names are preserved; unavailable applications and conflicting names are skipped and counted.
- Knowledge writes are transactional together; memory writes are transactional separately; alias preferences are committed separately. This is not one transaction across all three stores. An interrupted restore can leave completed entries saved; retrying the same backup does not duplicate them.

## Limits and delivery blocker

The old export is not a full application backup. It excludes conversation history, API credentials, settings, permission grants, model files, signing identity and original document binary files. Imported document text is not automatically sent to an AI provider; existing user-controlled knowledge-sharing settings continue to apply.

No original signing key has been recovered. This source change cannot update the original signed application until that key is restored. A separate reinstall signing line was subsequently selected; see [reinstall requirements and retained identity](REINSTALL.md). No package ID change, phone uninstall or data clearing has been performed. A reinstall loses old data outside this export and must wait until backup transfer and import checks are complete.

## Validation of this development change

- Full JVM suite: 220 passed, zero failures, errors or skips. Eleven importer tests cover parsing, malformed data, size limits, deduplication, identifier stability, legacy passage newlines and the privately supplied user export. The user export is passed through a local test environment variable, not checked into test fixtures. Without that optional input, its specific test reports an assumption skip.
- Debug lint: zero errors/fatal issues, 28 warnings. ARM64 debug APK assembly succeeded. This artifact is a development test build and cannot update the installed release.
- Four API 29 x86_64 emulator instrumentation tests passed using synthetic fixtures and actual SQLite, Room and preferences: additive/retry-safe knowledge restore with full text preservation, capacity failure rollback, alias conflict/missing-app handling, and memory timestamp/deduplication. The tests did not import the user's private content into the emulator or access their phone.
- Interactive file-picker selection, consent cancellation and the full import UI have not been exercised end to end. No physical Redmi 9 reinstall/upgrade has been attempted. These results do not establish that all historical application data can be recovered.

Local evidence is retained under `/workspace/android-toolchain/`: `import-validation-final.log`, `import-emulator-build.log`, `import-emulator-install.log`, and `import-instrumentation.log`/`.json`. The ARM64 debug artifact contains no private backup/key entries; SHA-256 is `9f1597973b26657caa182bbd916cbeaf8cc799e225bdd1d00dee004a93a30127` (104,784,256 bytes). No production APK has been signed or published.
