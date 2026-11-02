# Jarvis 7.0 — Infinity update

This updates the existing `com.jarvis.next` application, named **Jarvis**, with its retained signing key. It does not replace the original `com.jarvis.assistant` installation. No uninstall or data reset is part of this upgrade. Work is on `jarvis-next`; the original `main` and `jarvis-android-download` branches are outside this change.

The implemented upgrade prioritizes the requested optional near-1 GB local brain, removal of RAM-based refusals, and supported Infinity device controls. It does **not** claim every feature in the supplied Infinity document is complete. The current audited starting point was Jarvis 4.0; no existing 5.0/6.0 speaker-identification or music-fingerprinting engine was found.

## Optional real local brain

Open **Settings → Neural Core → Install optional near-1 GB brain**. Jarvis fetches official model metadata and shows the exact bytes, additional storage budget, immutable revision, SHA-256 and Apache-2.0 metadata before requesting approval. Nothing is automatically downloaded.

The larger candidate is **Qwen2.5-1.5B-Instruct, GGUF Q4_K_M**, from `Qwen/Qwen2.5-1.5B-Instruct-GGUF`, file `qwen2.5-1.5b-instruct-q4_k_m.gguf`. The accepted download range is **900,000,000–1,300,000,000 bytes**. This is a bound for a near-1 GB candidate, not a measurement of the live repository. If the expected file, license, revision, size or digest cannot be verified, installation does not proceed. No dummy data, padding, vocabulary-only fixture or reference-note pack substitutes for model weights. No weights are bundled in the APK.

Keep Settings open while downloading; use Wi-Fi if you want to avoid mobile-data charges. **Cancel operation** retains a revision-bound partial file. A retry validates the HTTP byte range and rehashes retained bytes. The final size and SHA-256 must match before the weights and verification record become installed. Other profiles are retained. A failed download does not switch your current brain or cloud provider.

After installation, choose the profile and press **Use selected installed profile**. This separately activates local inference and turns off cloud voice. Lite remains available in its original directory. Only one inference handle is loaded at a time; activation, removal and inference are serialized. The removal button removes only an unselected profile's model/partial files, not memories, documents or other brains. A cold-load integrity failure invalidates the stale verification record so a repaired download can proceed while retaining the old weight bytes until replacement.

Lite uses the existing official Qwen2.5-0.5B Q4_K_M model. Balanced uses a 2,048-token configured runtime context; Lite uses 1,536 tokens. The native llama.cpp CPU backend, tokenizer and prompt/context handling remain in place. Eco mode uses one inference thread, otherwise two. Qwen's training context is not the runtime context. Actual tokenizer/model header details and inference measurements are recorded by the existing on-phone evaluation only when real weights have been loaded.

The memory figures shown in Settings are estimates, not measured requirements or gates. At the user's request, **minimum-total-RAM checks, available-RAM checks, the Android low-memory admission check and automatic low-memory cancellation have been removed**. ARM64 compatibility, installed-model integrity, storage checks and severe thermal protection remain. Android and the native allocator can still fail allocations or terminate the process; removing a refusal does not create physical memory. Idle unloading and telemetry do not refuse requests.

This cloud cannot reach the model host under its enforced destination policy. The full official download and real model inference have **not** been benchmarked here. The installer is tested with deliberately small test streams; those bytes are not distributed as AI weights. Redmi 9 latency, RAM usage, heat, battery draw, English/Spanish/Persian quality and comparative intelligence remain unmeasured. Run the existing phone evaluation after installing a model. A larger download does not guarantee more accurate answers, and neither profile is an offline encyclopedia.

## Device controls and commands

Open **Settings → Neural Core → Infinity · Wi-Fi, Bluetooth, battery & websites**. This is a private, screenshot-protected foreground activity. Missing measurements and permission denial are reported as unavailable.

| Capability | Implemented behavior |
| --- | --- |
| Wi-Fi | On-demand active transport, Android internet validation, captive portal, available SSID/RSSI/frequency/link rate/IP/DNS. SSID redaction is explicit; optional fine location access is requested only from its button. Link rate is labelled separately from internet throughput. |
| Internet test | Explicit confirmation starts a Cloudflare HTTP transfer: 4 MiB down, 1 MiB generated random data up, plus three empty HTTP probes and overhead. Displays observed transfer rates, HTTP response time and successive timing variation. No packet-loss or guaranteed-ISP-speed claim. Cancellation and leaving the screen cancel the test. No document, memory or microphone content is uploaded. |
| Bluetooth | Fresh paired-device list, names/types and queried A2DP/headset/GATT connection results. A separate opt-in foreground discovery scan lasts at most 12 seconds and reports actual discovery RSSI. No connection result is inferred from pairing or discovery. Missing profile results stay unknown. |
| Accessory battery | A separate approved read temporarily connects to a paired BLE/dual accessory's standard Battery Service and closes GATT afterwards. Unsupported Classic devices, missing services, errors and timeouts return unavailable. Earbud/case percentages are never fabricated. |
| Device matching | Normalized names, English/Spanish/Persian spoken digits, actual candidate matching, list ordinals and explicitly saved/deletable nicknames. Ambiguous matches ask for a complete name. Context expires after two minutes and clears on screen-off or conversation deletion. |
| Connections/settings | Android controls for Bluetooth, Wi-Fi/internet, volume, display, location, battery saver, airplane mode and DND. Protected changes stay in Android's interface. Dispatch reports `AWAITING_USER`, never a verified connection/change. Refresh readings to verify available state. No hidden APIs or forced connections. |
| Battery | Actual level/charging, Android health classification, temperature, voltage and available current/charge-counter properties. Remaining-capacity health percentage and charging-completion time are not invented. |
| Alarms | Explicit weekday, tomorrow, supported ISO date and explicit recurrence parsing in the phone's configured timezone. One-off dates are never converted to weekly `EXTRA_DAYS`. Portable Clock intents cannot enforce a one-off date: the requested date is shown for manual review in Clock. Only explicit recurrence sends weekday extras. Ambiguous/unsupported/past dates ask for clarification. Dispatch does not claim the alarm was saved. |
| Websites | Ten explicitly saved HTTPS websites with name/description, edit/delete and voice `open NAME` / `read NAME`. Opening uses the existing reviewed browser proposal; reading uses the bounded public-source reader. Retrieved page content cannot authorize a tool. |

Examples: `show my Bluetooth devices`, `connect to Pro Seven`, `is it connected?`, `what's its battery percentage?`, `what's my Wi-Fi speed?`, `what's my battery health?`, `open Wi-Fi settings`, `alarm Friday at 6:35 AM`, `alarm every Friday at 06:35`. Bluetooth connection checks use exposed profiles, not universal accessory control. News and weather still require live sources or explicitly labelled cached readings; local AI does not invent them.

## HUD, speech and preservation

Both renderers share the actual-state palette: purple listening, cyan speaking, blue-violet thinking, restrained amber error and an independent green charging ring. Charging can coexist with speech/listening colors. Wallpaper clock/weather occupy bounded adjacent top columns. The app's clock/weather are adjacent, with a compact scrolling user/assistant caption area directly beneath the reactor and tap-to-expand conversation. User partials replace the current transcript; generated answer text has its own state. Captions show received text, not invented word-level audio alignment.

Activation volume is separate from general effects volume and the existing reply volume. Existing original sound assets, Android silent/DND/call rules, audio focus, reduced motion, quality modes, visibility/screen-off rendering controls and all three navigation tabs remain.

The existing ten-second wake-command and post-spoken-response follow-up windows, approved memories/FTS retrieval, bounded tool proposals, app discovery/aliases, screen/live controls, photo/file picker and sharing, read-only connected folders, providers and encrypted provider keys remain. Room and existing preference namespaces are not replaced. The scheduled briefing remains opt-in and preserves existing schedules; select Ecuador/Quito topics, 09:00 and the appropriate phone timezone explicitly if desired. No new background microphone, automatic speech, overlay or exact-alarm entitlement was added.

## Scope limits

The supplied document's full acceptance list is not all marked complete. This update does not implement a licensed song-fingerprinting provider, authorized Spotify library saving, six-person speaker embeddings/enrollment, a new native front-camera preview/comparison engine or an incoming-call-style Jarvis session workflow. Existing explicit photo selection/OCR/sharing and ordinary private briefing notifications remain. No fake song result, voice identity, biometric authorization or private camera recording is used to fill those gaps. New diagnostic controls include some English technical labels; full localization of every new control is not claimed.

Actual Bluetooth accessories, BLE battery reads, Xiaomi background restrictions, physical microphone/acoustic performance, secure lock-screen activation, Android 12+ Nearby Devices permission UX and sustained Redmi 9 operation require handset testing. The emulator cannot establish those results. ZIP alignment does not remove the pre-existing `libvosk.so` 4 KiB ELF alignment limitation on some 16 KiB page devices.

App identity does not establish Play Protect approval. Local AI is an alternative processing option; it does not reset Google's HTTP 429 quota. Keep the existing Jarvis installation and data.

## Signing and private recovery

The retained signing directory is `/workspace/private/jarvis-next/`, outside this repository, mode `0700`; the PKCS12 keystore and password file are `0600`. The encrypted backup is `/workspace/private/jarvis-next-signing.tar.enc`, with its separate unlock file `/workspace/private/jarvis-next-backup-password`. Download/copy these two files privately and store them separately in trusted encrypted storage; cloud workspace retention is not a backup guarantee. Never attach them to GitHub, the APK, a public release or a public issue.

The backup uses AES-256-CBC, PBKDF2, 200,000 iterations, containing a gzip tar archive. Restore privately using OpenSSL with `-pass file:/private/path/to/unlock-file`, then extract into a private directory and set `JARVIS_NEXT_SIGNING_DIR` there. Do not generate a replacement key. Certificate SHA-256 must remain `56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`. The unlock file and inner keystore password are distinct credentials. Full recovery commands are in the earlier [private recovery instructions](JARVIS_2.md#signing-and-private-recovery).

Final APK identity, checksum, build results and individual Android test results are recorded with the delivery artifact. Only a successfully verified artifact is a downloadable release; earlier successful 4.0 results are not represented as 7.0 verification.
