# Build compatibility

The project uses pinned, mutually compatible versions. These are deliberate versions for this source build, not a claim that they are the newest available SDKs.

| Component | Version/requirement |
| --- | --- |
| Android minimum | API 29 / Android 10 |
| Compile and target SDK | API 35 |
| Android Build Tools | 35.0.0 |
| Android Gradle Plugin | 8.7.3 |
| Gradle Wrapper | 8.9 |
| Kotlin and Compose compiler plugin | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| Compose BOM | 2024.12.01 |
| JDK | Full JDK 17 or 21 recommended; source/JVM target 17 |
| Android NDK | 26.3.11579264 (r26d, Clang 17) |
| CMake | 3.22.1 |
| Native device ABI | arm64-v8a by default |

The Redmi 9 is the intended first physical test device. Its actual Android version, usable RAM, free storage, speech-engine availability, and background behavior must be checked on the phone. There is no measured Redmi 9 latency/battery/voice-quality claim from a source build.

If the build exposes `-PjarvisAbis=x86_64`, use it only for the corresponding emulator artifact; it does not turn that APK into an ARM64 phone build. Keep the final phone artifact and emulator artifact clearly identified.

## Host setup

Use Android Studio's SDK Manager or the official Android command-line tools. Install:

```text
platform-tools
platforms;android-35
build-tools;35.0.0
ndk;26.3.11579264
cmake;3.22.1
```

Set `JAVA_HOME` to the JDK root and `ANDROID_HOME` to the SDK root. Do not commit machine-specific `local.properties`. Android Studio may generate it locally. Do not change global user/home paths to make tools work.

With an installed `sdkmanager`, inspect and accept applicable SDK licenses, then install the required packages:

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "ndk;26.3.11579264" "cmake;3.22.1"
```

The scripts deliberately do not install host tools, bypass license prompts, disable TLS, or change package checksums. SDK Manager installations verify Google's package metadata. Preserve Gradle distribution checksum verification and existing dependency integrity checks.

## Build-time network

Required hosts normally include `dl.google.com`, `repo.maven.apache.org`, the Gradle distribution endpoints, and applicable GitHub release/CDN endpoints. The vendored native source is built locally. The packaged sherpa runtime and its corresponding source/license material must remain part of the complete source distribution.

Behind a managed HTTPS proxy, Java/Gradle may require explicit proxy properties and a truststore containing the organization's proxy CA. Use supported trust configuration and keep certificate verification enabled. Do not put proxy credentials or API keys into committed Gradle files. Host-specific setup belongs in the user's Gradle home or environment configuration.

## Runtime network and model downloads

- Vosk selected-language models: official `alphacephei.com` model distribution.
- Optional neural voice archives: official sherpa-onnx GitHub releases and redirects to their release CDN.
- Optional local language model: official Qwen Hugging Face repository/API and its download CDN; immutable revision and SHA-256 metadata are checked by the app.
- Live news/weather/search and optional cloud providers: the configured public services shown by those features.

Build-time access does not prove the phone's runtime services work. Download errors must leave a visible, recoverable state rather than an apparently installed model.

## Validation boundaries

Compilation proves that source and native libraries build for the selected ABI. JVM tests prove the checked logic. An emulator can demonstrate installation and UI/lifecycle behavior for its OS image. Only physical-device tests establish real microphone recognition, audible speech, Bluetooth routing, screen-off behavior, battery use, thermals, and Redmi 9 memory survival.

Reference sources:

- [AGP 8.7 release notes](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes)
- [Gradle 8.9 compatibility](https://docs.gradle.org/8.9/userguide/compatibility.html)
- [Android SDK tools](https://developer.android.com/tools)
- [NDK downloads and revisions](https://developer.android.com/ndk/downloads)
- [Android foreground services](https://developer.android.com/develop/background-work/services/fgs)
