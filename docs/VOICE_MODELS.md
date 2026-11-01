# Voice engines, model provenance, and verification

This build offers real local speech recognition and speech synthesis. Recognition and audible playback on the user's physical Redmi 9 remain unverified until installation; successful desktop model synthesis is a different evidence category.

## Recognition and wake activation

`WakeListenerService` runs Vosk locally over 16 kHz, mono, signed-16-bit microphone PCM. It recognizes speech, then matches an explicit wake prefix in recognized text. It is **not** a purpose-trained wake-word classifier. Wake accuracy, battery consumption, speaker echo, and performance must be measured on a real phone.

The user chooses one listening language at a time:

| Language | Wake prefix | Official model | Approximate download | Model license |
| --- | --- | --- | --- | --- |
| English | Hey Jarvis / Hello Jarvis | `vosk-model-small-en-us-0.15` | 40 MB | Apache-2.0 |
| Spanish | Hola Jarvis / Oye Jarvis | `vosk-model-small-es-0.42` | 39 MB | Apache-2.0 |
| Persian | سلام جارویس / هی جارویس | `vosk-model-small-fa-0.42` | 53 MB | Apache-2.0 |

Catalog source: <https://github.com/alphacep/vosk-space/blob/master/models.md>, checked 2026-10-31. Model downloads use the catalog's `https://alphacephei.com/vosk/models/…zip` URLs. The build environment's destination policy blocks that host, so actual Vosk-model downloads were not tested here. The upstream catalog does not supply expected digests. The app records a downloaded artifact's SHA-256 for diagnostics; it does not misrepresent this as independent upstream authentication. TLS verification, compressed/expanded-size limits, path traversal protection, required-file validation, cancellation, and atomic installation remain enabled.

Once installed, the same language model supports push-to-talk recognition on Android 10/11, including Redmi 9. Without a downloaded model, Android's on-device recognition is used only where the OS actually offers it. Network-backed Android recognition requires the explicit setting; no speech is silently sent to it.

Only a valid user-visible app flow starts microphone foreground listening. There is no boot receiver, alarm restart, sticky resurrection, or hidden recording. The service pauses capture after delivering a command and resumes only when the runtime requests it. A visible notification distinguishes listening from paused processing and provides Stop everything.

## On-device neural voices

Installed offline Android TTS voices are preferred. If the selected language has no offline system voice, the app can use a user-downloaded Piper VITS voice through sherpa-onnx. Neural synthesis and AudioTrack playback happen on the phone; no API key or speech upload is involved.

Pinned runtime: [sherpa-onnx Android SDK v1.12.15](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.12.15).

Android AAR:

```text
https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.12.15/sherpa-onnx-1.12.15.aar
SHA-256 6994c53380846107b0363fee02fa9db6176c61c746a2315893c6b01e7f6d4c1d
```

Model URL prefix: `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/`.

| Language | Archive | SHA-256 | Dataset provenance in included MODEL_CARD |
| --- | --- | --- | --- |
| English | `vits-piper-en_US-ljspeech-medium.tar.bz2` | `3dfb4b759d8be032a4903a9538d128b0fda2a06ab1de6cbc2d93a97e2dd83dba` | LJ Speech, public domain |
| Spanish | `vits-piper-es_ES-davefx-medium.tar.bz2` | `a3f6beb54a9cb893279f72978a22f807a4d9fc9c7848157b524d5cc7b7f58b22` | Nabu Casa/DaveFX, CC0 |
| Persian | `vits-piper-fa_IR-amir-medium.tar.bz2` | `46af65c1cdd86ec300db3998f2bf31a68d9e38da930c0655c1ff6f610e84f864` | Datacula/Amir, CC0 |

All three complete archives were downloaded from the official release over verified HTTPS, hashed, and inspected. Each contains the matching `.onnx` file, `tokens.txt`, native phonemizer data, model configuration, and `MODEL_CARD`. The app compares future downloads to these pinned hashes before installation. An upstream replacement must fail closed until the catalog is deliberately updated and reviewed.

Desktop synthesis was also executed successfully using the official sherpa-onnx **1.12.15 Linux x64 CLI**, matching the Android SDK version. It produced nonempty 22,050 Hz mono PCM WAVs for the English, Spanish, and Persian Voice Check sentences. Evidence is at `/workspace/artifacts/jarvis-voice-check/verification.json` and its three WAVs. This verifies real model/phonemizer synthesis on Linux; it does not verify Android JNI playback, human pronunciation quality, Redmi performance, Bluetooth, or microphone recognition.

The Spanish fallback voice is from Spain; the app must not label that particular voice Latin American. Installed Android voices may offer additional accents. Each model is about 65 MB compressed and needs extra temporary extraction space. Downloads are user initiated, show progress, are cancellable, reject symbolic/hard links and unsafe paths, and have compressed/expanded-size and entry-count limits.

## Corresponding sources and license obligations

The runtime is Apache-2.0, with the original upstream license at <https://github.com/k2-fsa/sherpa-onnx/blob/v1.12.15/LICENSE>. It integrates components with their own terms. In particular, its eSpeak NG phonemizer is GPLv3; app/source distribution must preserve those obligations and notices. Do not relabel the whole native dependency graph Apache-only.

Exact source files inspected in sherpa-onnx v1.12.15:

- `cmake/espeak-ng-for-piper.cmake`: eSpeak NG fork commit `f6fed6c58b5e0998b8e68c6610125e2d07d595a7`.
- `cmake/piper-phonemize.cmake`: piper-phonemize commit `78a788e0b719013401572d70fef372e77bff8e43`.
- `build-android-arm64-v8a.sh` and the corresponding other-ABI scripts: Android build recipe and ONNX Runtime 1.17.1 selection.
- `.github/workflows/android.yaml`: upstream native SDK packaging recipe.
- `sherpa-onnx/kotlin-api/Tts.kt`: exact Kotlin/JNI API used by this app.

Corresponding source archives downloaded for inclusion with the source distribution:

```text
https://codeload.github.com/k2-fsa/sherpa-onnx/tar.gz/refs/tags/v1.12.15

https://github.com/csukuangfj/espeak-ng/archive/f6fed6c58b5e0998b8e68c6610125e2d07d595a7.zip
SHA-256 70cbf4050e7a014aae19140b05e57249da4720f56128459fbe3a93beaf971ae6
License: GPL-3.0; full COPYING is in the source archive.

https://github.com/csukuangfj/piper-phonemize/archive/78a788e0b719013401572d70fef372e77bff8e43.zip
SHA-256 89641a46489a4898754643ce57bda9c9b54b4ca46485fdc02bf0dc84b866645d
License: MIT; full LICENSE and copyright notice are in the source archive.
```

Both dependency source ZIPs were verified against the expected digests recorded by sherpa's own pinned CMake files. To rebuild, unpack sherpa's source, provide a compatible Android NDK, set `ANDROID_NDK`, and use its ABI-specific build scripts with TTS and JNI enabled. Follow the exact tagged workflow for packaging the native libraries and Kotlin API. A source recipe is provided for reproducibility; this project uses the unchanged upstream AAR and does not claim its Android native SDK was independently rebuilt here.

Keep the app's GPL-compatible source bundle and these corresponding sources/notices with any distributed APK. ONNX Runtime and other permissive native components retain their original notices. Model cards describe dataset provenance and are retained in downloaded model directories; they are not a replacement for native-library notices.

## Device acceptance checklist

- Install the selected recognition model and test the localized wake phrase at normal and quiet volumes, screen on/off, and with speaker playback.
- Test microphone contention, permission revocation, Stop everything, foreground notification, and user-initiated restart after process/OS shutdown.
- Test Voice Check for English, Spanish, and Persian; verify actual audibility and pronunciation, not merely engine routing.
- Remove/disable the system Persian voice, download Amir, and verify a Persian reply with network disabled afterward.
- Verify Silent/DND behavior, Bluetooth routing, audio-focus interruption, and cancellation while native inference is active.
- Measure model-load delay, recognition accuracy, first audio latency, peak memory, temperature, and idle battery consumption on the named phone. Do not infer these from compilation or desktop synthesis.
