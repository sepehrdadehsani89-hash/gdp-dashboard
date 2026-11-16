# Voice engine milestone

## Existing pipeline

ManagedMicrophone owns one 16 kHz mono PCM recorder with session-bound optional Android AEC/NS/AGC. Vosk is already present for wake/foreground offline ASR; SpeechController contains platform/on-device recognition availability and support probes. sherpa-onnx 1.12.15 was already committed for neural voice/runtime use. Presence is not proof of model compatibility or actual provider availability.

UtteranceAssembler and SpeechInactivityClock implement text/RMS endpointing; they are not Silero neural VAD. No Silero model, RNNoise, WebRTC or new wake library was installed this milestone. Actual noise accuracy, Bluetooth routing, permissions, TTS echo and background behavior remain device-dependent.

## Focused repair: truthful offline playback completion

Before: a ten-second System.currentTimeMillis drain wait exited on timeout and still invoked onDone if not cancelled, without checking that written audio had played.
After: OfflinePlaybackDrain returns COMPLETE only when the observed playback cursor reaches the written mono frame count; CANCELLED never calls completion; TIMED_OUT emits a clear output error. Uses elapsedRealtime, including fail-closed rollback handling; unsigned AudioTrack cursor conversion prevents negative Int interpretation. Existing audio cleanup and generation checks remain.

20 deterministic JVM tests cover empty/already-finished/overshot buffers, progressive and stalled output, exact/overrun deadlines, trickle progress, early/during-read/during-wait cancellation, clock rollback, configurable budgets, invalid counts and interruption/read errors. These tests do not play audio or execute native sherpa inference. Hardware integration remains blocked and no new voice capability is production certified.

Current main conversation route remains configured Fish Audio. Existing system/Piper helpers are not presented as a new active fallback. Provider selection reconciliation remains open; keep configured voices, privacy consent and no-secret-in-APK rules intact.

## Verification target

The active request targets up to 100 meaningful checks per significant feature. This bounded repair defines 20 executed policy cases and 20 pending device acceptance scenarios. Broader voice baseline has 201 distinct cases across subsystems; those are not 201 acoustic trials or a per-feature 100/100 score. Expand device, routing, failure and performance coverage only with meaningful fixtures. The complete 100-check voice acceptance target remains open.
