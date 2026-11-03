# Current-instance verification

This is a development continuation, not a signed Jarvis 10.0 release. Historical 7.0 results are not counted as current checks.

## Completed on the current source

- Full JVM suite: **209 tests passed; zero failures/errors/skips**, 28 suites. Includes original regressions, actual mock HTTP transfers/cancellation and byte samples, weather timestamp/forecast parsing, location-fix validation, direct-app routing and normalized EN/ES/FA module/context routing.
- Android debug lint: **zero errors, 28 warnings**. Lint still aborts on errors; no test assertions were disabled.
- ARM64 debug and Android-test APK compilation succeeded with the checksum-verified Gradle 8.9 wrapper, full Temurin JDK 21.0.13+9, SDK/build-tools 35, NDK 26.3.11579264 and CMake 3.22.1.
- Matching x86_64 debug/test compilation succeeded for the isolated API 29 software emulator.
- The ARM64 development APK is **104,749,256 bytes**, SHA-256 `b0a0d03ce77e089d4dca3b5bba09190ba6138a7ef0f682b89e3eccaca1689293`. Package `com.jarvis.next`, launcher `Jarvis`, code 10, version `10.0.0-dev`, ARM64-only. ZIP alignment checked. No private signing files or GGUF weights are packaged. This debug artifact **cannot update the installed signed phone release**.
- The retained Jarvis 7.0 APK matches the handoff checksum and public signing-certificate fingerprint. This does not recover its private signing key.
- Release guard refused to proceed without `JARVIS_NEXT_SIGNING_DIR`, as expected. A positive original-key signing check, release build/lint and production upgrade installation remain blocked by missing private material. No replacement release key was created.

## Android functional evidence

The initial 25-case focused run completed with **21 passes, three failures and one optional voice-model assumption skip**. It exercised actual Android UI/memory/settings, battery/network APIs, wallpaper preview/selection, native rendering/audio and model-unavailable behavior. It used the initial development APK, not the later revised camera build.

Initial failures:

1. Camera preview opened and the emulator HAL produced JPEG bytes, but the test's 30-second capture-result wait timed out. This did not establish a complete capture/attachment pass. Camera lifecycle was then strengthened with actual preview-stream readiness, stopped preview during still review, callback generations, a bounded timeout and decode/allocation/storage errors. The recheck still requires actual decoded JPEG pixels and an explicit Assistant attachment; its software-emulator waits are longer.
2. Weather test selected one temperature node, although both header and detailed panel correctly displayed the fixture reading. The assertion now verifies both actual labels and retains its forecast/provider/memory assertions.
3. Wallpaper preview and actual selection passed; home-screen visible-engine observation timed out. Wallpaper application code was not changed to hide the failure. An isolated recheck is required.

The optional neural-voice test was skipped because the three documented voice models are not installed. No acoustic-quality claim is made.

**Resolved results across the 25 selected cases: 24 passed, one optional voice-model assumption skip.** This combines the initial run with targeted rechecks, not a single final-APK sweep of all 48 available Android cases. The other 23 Android cases were not run in this session.

The five-case follow-up passed all three new module/weather tests. Camera then failed in ActivityScenario teardown after its return-to-Main intent changed the tracked launch intent; returning by closing the camera instead of relaunching Main fixed that path. The final camera capture/decode/explicit-attachment test passed completely on the updated application.

The wallpaper follow-up initially aborted when Android killed the app for a wallpaper-service ANR. The emulator also reported CPU-thread hangs and system-service failures; that report did not contain a usable app stack, so no particular application deadlock is asserted. The same retained AVD was restarted with one software-emulated CPU and the earlier SwiftShader backend. The next wallpaper run rendered a genuine preview but timed out awaiting application. An actual UI hierarchy dump showed the pending Home screen/Home screen and lock screen chooser: the test's 2.5-second optional chooser lookup had missed the delayed dialog. The driver now waits for the real chooser or verified application and returns through an ordinary MAIN/LAUNCHER intent. All manager/component, actual frame/battery, no-microphone, sleep/hidden cleanup and launcher-preservation assertions remain. The complete final wallpaper case passed.

The final application differs from the five-case follow-up only in the camera return path. Only the wallpaper test driver changed after the successful final camera case; the application APK was verified byte-for-byte unchanged for the final wallpaper run. Full source/APK hashes and per-case outcomes are in [verification.json](verification.json). These scoped rechecks must not be represented as a new full-suite run.

The emulator restart preserved AVD userdata. Stale locks were removed only after confirming that their process had terminated; no application uninstall or data clear was used.

Initial source/APK provenance and raw reports are retained under `/workspace/android-toolchain/artifacts/initial-sweep/`. Current build logs, APKs and reports remain under `/workspace/android-toolchain/` and the ignored `app/build/` directory.

## Unverified boundaries

No physical Redmi 9, real Bluetooth accessory, live local-model weights, model-language quality/speed, sustained battery/heat measurement, actual-phone acoustic or secure-lock-screen test was available. Live weather, Cloudflare throughput and external provider availability are not established by mock transport tests. No production APK has been signed or published. The known `libvosk.so` 4 KiB ELF alignment limitation remains; ZIP alignment is not universal 16 KiB-page certification.

See [implemented behavior and unfinished work](IMPLEMENTATION.md), [acceptance matrix](ACCEPTANCE.md) and [private key recovery](SIGNING_RECOVERY.md). The complete changed-file inventory is in `CHANGED_FILES.txt`.
