> Historical checkpoint. The later beta implementation and evidence are described in [ACCEPTANCE.md](ACCEPTANCE.md), [CURRENT_UPGRADE.md](CURRENT_UPGRADE.md) and [BETA_VERIFICATION.md](BETA_VERIFICATION.md). Earlier unavailable-feature statements are not the current source inventory.

# OMNISYSTEM integration audit

Baseline: jarvis-next f110e56d30dfcc3c1a6153f19888be10fa2bb79b (Jarvis 7.0).

## Dependency map

- MainActivity → JarvisAppUi / JarvisUiState → AssistantRuntime. Activity owns permission and external UI launch workflows.
- AssistantRuntime → NeuralOrchestrator, PhoneContext, DeviceTools → InfinityTools / ConnectivityRepository. Only trusted current user input reaches tool dispatch.
- AssistantRuntime → LocalBrain → JNI llama.cpp; NeuralPreferences selects verified Lite/Balanced files. Model installation and activation are separate. No RAM admission checks may be added.
- AssistantRuntime → SpeechController / WakeListenerService → ConversationWindow; 10-second capture and post-speech follow-up windows must remain.
- HudRepository → Android battery/network/time observations + WeatherRepository / WeatherClient. Main HUD and WallpaperRenderer consume the same actual state.
- JarvisWallpaperService → WallpaperRenderer; lifecycle/visibility controls rendering. ReactorPalette and AudioSignals/PlaybackLevels drive real colors/levels.
- JarvisStore → existing Room schema, preferences and Keystore credentials. KnowledgeIndex is additive FTS storage. No destructive migration is authorized.
- InfinityControlsActivity owns explicit discovery, BLE reads and consented speed transfers. External Clock/SystemPanels intents are awaiting-user requests, not verified changes.

## Existing capabilities

Native animated wallpaper, real battery/network/weather, captions, approved memories/retrieval, app matching/aliases, bounded compound planning, reviewed live controls, selected file/photo OCR/sharing, read-only connected folders, optional providers and briefings are present in source. Historical test reports are not fresh verification.

## Gaps and external prerequisites

Dedicated module presentation is missing; the existing Infinity screen combines controls. User-facing RAM telemetry is still present in JarvisHud and must be removed without restoring refusal checks. CameraX preview, licensed music fingerprinting, Spotify authorization and speaker embeddings/enrollment are absent. Real model benchmarks require downloaded verified weights and physical hardware.

The original release key and private encrypted backup are absent from the current workspace. Never generate a replacement key. Signed upgrade publication is blocked pending private recovery and certificate verification (56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719). JDK 21 exists; Android SDK/NDK/Gradle caches must be restored. main and jarvis-android-download must remain unchanged.
