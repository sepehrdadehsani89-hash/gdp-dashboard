# Unreleased milestone

Code repair: 36f2c271bad44b73e6d4d9b0fb9a9b75b0eda94b. Base: a7f2c83 (backed up on GitHub). Local pre-change checkpoint: checkpoint/jarvis-next-before-voice-repair.

- Repair offline TTS helper completion: undrained output times out with error instead of false success; monotonic deadline and cancellation/cursor guards.
- Add 20 deterministic regression cases and a separate device acceptance matrix.
- Run baseline and companion tests; broader regression/lint evidence in TEST_RESULTS.md.
- Add active directive provenance, incremental source/feature inventory and explicit requirements-to-code mappings.
- No UI/assets/models/dependencies/permissions/signing/storage changes. No remote push, hosting or deployment performed.

Rollback: revert the repair commit after it is created, or review its focused source diff against checkpoint/jarvis-next-before-voice-repair. Preserve later user changes and stored app data. Never reset the working tree or regenerate release keys. If rolling back an installed future APK, use a higher version code with the retained matching signer rather than uninstalling or downgrading over user data.

## Phoenix evaluation 2.11.9-phoenix.1-debug

Core cadence repair and debug counters (a2bfd33); bounded wallpaper surface recovery (c16699a); version/debug packaging and truthful setup text (51b5153). New APK actually generated and verified, original release artifacts preserved. Full details, tests and rollback limits: PHOENIX_PROGRESS.md. No full-product/device verification claim.
