# Source inventory; runtime baseline pending

Android module `app`, namespace com.jarvis.assistant, application ID com.jarvis.next, version 2.11.8/code 25. Kotlin/Compose; min SDK 29, target/compile SDK 35; JDK 17 target; AGP 8.7.3, Kotlin 2.0.21; NDK 26.3.11579264, CMake 3.22.1; default arm64-v8a. Entry MainActivity; native llama.cpp and OCR sources included.

Source exists for HUD/live wallpaper, battery/power ETA, weather, preferences/onboarding, local knowledge/model management, microphone ownership, wake listening, system/offline recognition, speech playback, reminders, contacts, classroom/call-like flows, device actions, privacy/accessibility, paired desktop and mission control. Source existence is not a WORKING runtime classification. Preserve all of these pending actual baseline tests.

Manifest declares microphone foreground service, runtime audio/camera/location and notification permissions, speech/TTS service discovery, wallpaper, screen-capture and accessibility services. Release signing uses externally supplied original signing identity with certificate checks; no credentials inspected or changed. No schema/storage migration changed.

Existing evaluation/feature-ledger.json and original project documents left untouched. No Android UI/assets or source modified. Existing release artifacts are prior artifacts, not newly built/verified in this session.
