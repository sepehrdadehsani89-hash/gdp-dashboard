# Current OMNISYSTEM integration map

Baseline: jarvis-next 1ed3365, application com.jarvis.next. The phone remains on original 7.0. The retained reinstall identity is 1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda; do not generate another key or change the package. Original-line in-place updates still require the unavailable original key. No phone uninstall is part of source implementation.

## Existing dependency graph

MainActivity -> JarvisAppUi/ModuleHud -> AssistantRuntime. Runtime owns existing Room memory, settings, microphone/speech lifecycle, native LocalBrain, module selection, reviewed device tools and HUD state. WallpaperService and activity consume the same HudRepository; they must not duplicate battery/network observers. PhoneContext gates private information by keyguard and permission; LocationFix owns a bounded current fix. CameraHudActivity leases the runtime voice pause and CameraX lifecycle. NeuralOrchestrator/ToolRegistry validate current-user authority and suspend plans behind approvals. Existing VoiceWindow preserves ten-second command/follow-up deadlines.

## Additive integrations

- Shared private feature vault: new Android Keystore alias and encrypted preferences, isolated from existing credentials. No destructive schema or namespace change.
- Shared microphone capture lease: extend the existing camera voice-pause ownership for visible music/profile recordings; release on stop/cancel and preserve wake preference. Never start background recording.
- Music: explicit short PCM capture -> bounded WAV -> genuine AudD response. Spotify uses native authorization-code PKCE, exact redirect/state validation and verified library actions. Credentials are entered privately in app; recognition and Spotify live validation require the user's provider accounts and commercial approval.
- Location: process-owned fix repository shared with PhoneContext and a private location screen. Android address lookup and real embedded map are explicit network actions. Saved destinations require explicit save; no automatic location history.
- Vision: existing explicit image/camera selection -> bounded stripped images -> optional selected-provider multimodal analysis, with per-request consent. Existing local OCR stays available; no unsupported offline visual claims or tool execution from image/provider content.
- Speaker personalization: actual compatible speaker embeddings with consented private enrollment, maximum six profiles. Model provenance/license/integrity must be verified before enabling a bundled download. Voice similarity never authorizes protected actions.
- Presentation: existing shared reactor/captions remain; module transitions and original cues use actual module/action states. EN/ES/FA labels and RTL extend the existing language system. Existing independent voice, activation/effects settings and Android notification-channel controls remain authoritative.

## Validation boundaries

The earlier checkpoint passed 220 JVM tests and four Android data-restore tests. Its end-to-end import picker check was blocked by emulator failures; do not reuse that as a pass. New code requires new targeted tests, compile/lint and a new version-code signed artifact. Physical Redmi 9 performance/acoustics/accessory behavior cannot be verified without handset measurements. Provider credentials, model licenses and Android restrictions are reported as actual prerequisites, never simulated success.

## Integrated beta source inventory

The integration described above is now implemented in the new `omnisystem` Kotlin files: private vault/base activity/transport/audio, AudD/Spotify/music screen, location repository/map screen, selected-image reader/vision screen, speaker repository/buffer/profile screen and owned timers/clock screen. Existing Runtime, MainActivity, PhoneContext, BrainProtocol/LlmClient, CameraHudActivity, WakeListenerService and UI connect those modules to process-owned state and actual Android/provider actions. No new database migration or package rename was introduced.

Forecast/ConversationState now resolve bounded contextual follow-ups and validate provider forecast dates in the source timezone. ToolRegistry/NeuralOrchestrator add deterministic user-bound Bluetooth/timer/alarm clauses and explicitly report steps not executed behind approvals. New original WAV cues are reproducible with `scripts/generate_module_cues.py`; existing voice/activation/effects preferences and Android timer notification sound controls remain authoritative. Private model/service prerequisites and license provenance are in OPTIONAL_SERVICES.md.

The retained signing line uses version code 11 for this beta. The user's original 7.0 app is still installed, so the original certificate mismatch remains a real delivery boundary. No uninstall, key generation, original-key replacement or phone data clearing was performed here. See ACCEPTANCE.md for unfinished localization, live account/hardware tests, automatic plan continuation and export coverage limitations.
