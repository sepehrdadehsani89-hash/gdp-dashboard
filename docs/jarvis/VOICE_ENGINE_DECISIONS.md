# Voice inventory and pending decisions

Existing dependencies: Vosk Android 0.3.47, JNA 5.13.0, local sherpa-onnx-1.12.15.aar. These were already committed, not installed this session.
SpeechController uses availability checks and system/on-device SpeechRecognizer. The actual Google provider is device-dependent and unverified. OfflineSpeechSession and wake service exist. MicrophoneProcessing attaches Android AEC, NoiseSuppressor and AGC only when requested/available, reporting enablement per session. This source presence is not measured acoustic effectiveness.
No Silero VAD integration was found by targeted source search in voice/local. sherpa AAR availability is not proof that VAD, keyword spotting or offline ASR engines are configured. No new model/runtime downloaded. No RNNoise, WebRTC or proprietary wake package added.

Decisions deferred until animation reproduction and device audio baseline: inspect existing ManagedMicrophone ownership and native ABI/model support; verify official versions/licenses; reuse compatible existing sherpa runtime if appropriate; one PCM owner; no simultaneous system recognizer capture or unsupported PCM injection; test suppressors separately and preserve fallback switches. Device/provider, model licenses, Google speech installation, real audio tests and latency/WER remain pending.
