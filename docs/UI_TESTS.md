# UI instrumentation checks

`JarvisFlowTest` exercises the real MainActivity, Compose UI, runtime, Room store, and deterministic keyless tools. These are not mock provider tests. Run on a dedicated test installation: the suite clears saved app settings, conversation, memories, and keys before every test.

```sh
./gradlew connectedDebugAndroidTest
```

For the prepared x86_64 emulator variant, select the project's documented ABI override when building/running tests. A software emulator without KVM can be much slower than a physical test device; increase infrastructure timeouts if the emulator itself has not finished booting.

The five scenarios check keyless onboarding without optional permissions, a typed time reply, explicit memory persistence and display, a YouTube proposal that waits for confirmation and can be cancelled, and Persian-localized navigation. No scenario approves an external action, downloads models, requires a provider key, or claims to verify real audio.

## Persian RTL visual review

On a fresh installation, select فارسی in onboarding or Settings. Check the Home, Memory, Settings, and approval dialog in portrait and landscape, including mixed English model names/URLs and Persian text. Verify start/end alignment, logical focus, directional icons, font scaling, and clipped labels. Use Android's screenshot tooling on the actual emulator/device; a localization assertion alone does not establish visual RTL quality.

Example capture after navigating to the relevant screen on a dedicated connected device:

```sh
adb shell screencap -p /sdcard/jarvis-fa.png
adb pull /sdcard/jarvis-fa.png
adb shell rm /sdcard/jarvis-fa.png
```

Record device/API, selected language, screenshot path, and observed issues in verification evidence. Do not label these scenarios passed until the instrumentation runner actually completes and reports the result.

## Recorded execution

Final API29 x86_64 software-emulator UI run: five executed, five passed, zero failures/skips. NativeEngineTest separately passed actual OCR and three-language non-silent speech synthesis with verified voice models preloaded in the isolated test app. The neural voice test explicitly skips when those optional models are absent; a skip does not validate synthesis. No physical-phone/audio-playback/battery claim is made.
