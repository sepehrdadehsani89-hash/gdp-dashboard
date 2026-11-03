# Master directive acceptance status

This matrix distinguishes source implementation from runtime evidence. It is not an acceptance sign-off. See `VERIFICATION.md` for current executed checks; historical delivery reports are separate.

| Directive items | Source status and boundary |
| --- | --- |
| 1 Existing application | Same package ID; additive source changes; no database migration/reset. Phone upgrade blocked by missing original signing key. |
| 2 Local AI | Existing real llama.cpp engine and verified optional profiles retained; ARM64 native compilation checked. No actual weights/inference benchmark in this machine. |
| 3 Persistent memory | Existing Room/FTS/preference namespaces retained; no destructive migration. Current regression evidence is in VERIFICATION.md. |
| 4 Direct apps | Existing installed-app registry, matching and approval path retained; module router explicitly avoids ordinary app-launch requests. |
| 5 Wake word | Existing EN/ES/FA wake and ten-second command/follow-up implementation retained. No physical acoustic verification. |
| 6–12 Wallpaper/reactor/charging/battery/clock | Existing real WallpaperService, visibility control, actual-state colors/levels, charging ring and adjacent weather/clock retained. Physical battery/performance behavior unmeasured. |
| 13 Weather HUD | Implemented current/hourly/daily data, source timestamps/timezone/staleness and bounded condition-driven atmosphere. Mock transport/parser and UI checks do not establish live weather-provider availability. |
| 14 Location HUD | Partial: dedicated panel and fresh permission-gated timed fix. No embedded map, geocoding or saved destinations. |
| 15 Battery HUD | Implemented actual shared battery state and reviewed additional property reads. No capacity-health estimate is invented. |
| 16 Wi-Fi HUD | Implemented shared network evidence and existing real SSID/RSSI/radio-link reader, with permission/redaction handling. |
| 17 Bluetooth HUD | Implemented actual paired/profile/discovery controls, measured RSSI signals and explicit BLE reads; no inferred direction/range or universal accessory connection. Physical accessory tests unavailable. |
| 18 Network HUD | Implemented consented fixed-size transfer, actual byte/time chart samples and complete/partial/error distinctions. Live Cloudflare service and ISP capacity not benchmarked. |
| 19 Music HUD | Unavailable panel only. Licensed fingerprint engine/provider and Spotify authorization are not implemented. No audio recording or fake match. |
| 20 Camera HUD | CameraX visible front/rear preview, explicit JPEG capture/review and opt-in Assistant attachment implemented. General visual-description/comparison engine remains unfinished. |
| 21 Clock/alarm HUD | Partial: real clock dial and existing reviewed Android alarm/timer workflow. External Clock dispatch is awaiting-user, not proof of a saved alarm. No fabricated timer countdown. |
| 22 System telemetry | Actual network/battery/microphone/provider/stage/storage/thermal readings. Normal HUD RAM meters removed. No invented CPU readings. |
| 23 Actual sources | Real Android/provider APIs retained; absent values stay unavailable. Synthetic test fixtures are labelled tests, not real-world measurements. |
| 24 Verified actions | Existing expiring, reviewed tool actions retained. System panels/intents remain pending until externally observable evidence exists. |
| 25 Live captions | Existing actual transcripts/replies retained; module response text is bound to the current request/selection. No invented word-level speech alignment. |
| 26 Languages | Existing EN/ES/FA processing/RTL retained. Some new technical labels are English; complete new-panel localization is unfinished. |
| 27 Six voice profiles | Not implemented; no verified/licensed speaker model or enrollment workflow. Voice never substitutes for Android authentication. |
| 28 Briefings | Existing opt-in WorkManager briefing and private notifications retained. No new phone scheduling guarantee. |
| 29 Websites | Existing ten-site explicit save/edit/delete/read/open workflows retained. |
| 30 Permissions | Contextual camera/location gates and cleanup added; existing Bluetooth/network/photo/provider controls preserved. Hardware permission UX needs handset tests. |
| 31 Android authentication | Existing keyguard/Codex flow retained; private camera refuses locked entry and protects its window. No PIN storage/replay. |
| 32 Background restrictions | Existing permitted activation/notification fallback retained. Camera opens only through explicit foreground interaction; no overlay or background-start bypass added. |
| 33 Redmi 9 performance | Not benchmarked. Existing Eco/Balanced/Cinematic caps and invisible-surface cleanup retained; new atmosphere caps at 20 FPS and respects reduced motion. |
| 34 Tests | See VERIFICATION.md for final counts, failures, skips and unrun checks. |
| 35 Build/delivery | Debug development builds are separate from signed phone delivery. Original release key is missing; release signing/publication blocked. |
