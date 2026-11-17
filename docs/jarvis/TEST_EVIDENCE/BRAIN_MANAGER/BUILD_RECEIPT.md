# Final build receipt

Source checkpoint: `2b73bbaf576166f84049969f03bdc9f732c55ba5`.

JDK21.0.13; Gradle8.9; AGP8.7.3; project Kotlin2.0.21; SDK35/build-tools35.0.0; existing NDK26.3.11579264/CMake3.22.1. No dependencies, emulator or real LLM weights installed during this change. Workspace disk remained about51GiB free; heap inspection during lint showed healthy allocation rather than memory exhaustion.

```bash
source /workspace/.jarvis-toolchain/env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestJavaWithJavac :app:assembleDebug --no-daemon --max-workers=1 --console=plain -I scripts/phoenix-resource-limits.gradle -Dorg.gradle.parallel=false '-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process
```

Actual result: BUILD SUCCESSFUL in14m45s;68 tasks,20 executed/48 up-to-date. Main application compilation had already succeeded; final tests, lint, Android-test source compilation and APK packaging completed. The2 terminal-empty-line-only formatting changes are recorded in EOF_FORMAT_ONLY.json; no code changed after compilation.

APK checks use `verify_artifact.py` and existing SDK aapt2/apksigner/zipalign. Per-case JVM and lint reports are original generated results, copied with console trailing whitespace normalized. All live account/device/native-model acceptance scenarios are separate and BLOCKED.
