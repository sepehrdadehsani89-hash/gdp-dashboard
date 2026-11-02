# Neural Core 4.0 model profiles

See the [current model, privacy, download and evaluation guide](../JARVIS_NEURAL_CORE.md) and [source baseline audit](neural/AUDIT.md). The following documents the original Lite bridge baseline; newer profile/context/output controls are described in the current guide.

# On-device conversation without an API key

Jarvis includes a real llama.cpp CPU inference bridge. After an explicit model download, `LocalBrain.answer` generates text on the phone. Prompts and conversation text are not sent to a provider by this implementation. Other features such as live news still need the Internet, and speech recognition/TTS privacy depends on the selected voice engines.

## Runtime and model provenance

- llama.cpp: MIT, tag `b5046`, commit `74d4f5b041ad837153b0e90fc864b8290e01d8d5` from <https://github.com/ggml-org/llama.cpp>. Complete source and upstream license are retained in `app/src/main/cpp/vendor/llama.cpp`; no runtime code download is used.
- Official model: [Qwen/Qwen2.5-0.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF), quantization `qwen2.5-0.5b-instruct-q4_k_m.gguf`, approximately 400 MB. The Qwen2.5-0.5B model is distributed under Apache-2.0; verify the model repository license when changing model variants. Do not assume larger Qwen model variants use identical terms.
- Setup requests the official Hugging Face model metadata with blob records, obtains its immutable revision and LFS SHA-256/size, and downloads exactly that revision over HTTPS. It validates size and SHA-256 before installing the file atomically in private app storage. The verified revision/hash/size are saved alongside the model. Missing integrity metadata fails closed.
- No model weights are bundled in the small APK. The first explicit download therefore needs Internet and sufficient phone storage. The download can be canceled; partial files are removed. Wi-Fi is recommended. Removing the app removes its private model file.

## Redmi 9 and other constrained devices

The Android build targets baseline ARM64 (`armv8-a`) rather than requiring newer dot-product or i8mm extensions. Inference is CPU-only with two threads, a 1536-token context and at most 256 newly generated tokens. Input is bounded, history is deliberately short, and model weights are memory-mapped. Close other apps before loading the model on a low-RAM phone.

This is a **small offline assistant**, not a frontier cloud model. Expect weaker reasoning, more factual mistakes, and potentially uneven Persian/Spanish quality. A reply can take substantially longer than cloud inference, especially on a Redmi 9. Model download size, available RAM, Android memory pressure, thermals, and actual phone performance must be checked on the user's device. There is no claimed latency, battery, or multilingual quality benchmark from physical hardware.

The local model has no direct tools. It does not inspect apps, photos, documents, or live news by itself. Trusted app routing handles available tool requests separately; the model's text never authorizes a tool action. Its prompt explicitly prohibits invented tool results and current facts, but the small model can still hallucinate. Confirm important information independently.

## Build integration

Use Android NDK `26.3.11579264` and CMake `3.22.1`. In `app/build.gradle.kts` configure `externalNativeBuild.cmake.path` to `src/main/cpp/CMakeLists.txt`, `externalNativeBuild.cmake.version` to `3.22.1`, and `ndk.abiFilters` to `arm64-v8a`. The CMake project statically links llama/ggml into `libjarvis-local.so` and disables GPU backends, network helpers, examples, tests, native-host architecture detection, and OpenMP. It requests 16 KB-compatible link alignment for its JNI shared library.

The complete vendored upstream source makes a clean source build independent of the original workspace clone. The normal Gradle build compiles the native library; do not ship a detached `.so` without its source/license provenance.

JNI receives UTF-8 bytes instead of relying on modified-UTF-8 JNI strings for multilingual prompts and output. Streaming only emits complete UTF-8 code points. Generation is bounded and cancelable; the native decode abort callback stops app-owned CPU work. Local model memory is releasable when the app no longer needs it.

## Verification limits

Prompt formatting/role-boundary tests and ARM64 compilation are automated checks. They do not establish real-phone inference speed, audible speech, Persian quality, RAM survival, or power use. The build environment's network proxy blocks `huggingface.co` (CONNECT 403); weights cannot be fetched here without an authorized network-policy change. The phone's explicit download workflow must be tested against the official model service. Do not describe model-weight inference as verified until an actual model has been loaded and generated output.
