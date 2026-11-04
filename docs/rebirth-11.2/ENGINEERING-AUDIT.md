# 11.2 Rebirth source investigation

Source baseline: published Phantom 11.1 commit `4100cbe2479fe482693ded1055e88bde938003d4`, clean before this task. Package remains `com.jarvis.next`, retained reinstall signing certificate, code15 /11.2.0-candidate1. No verified 11.0 source foundation is assumed. No signing key is generated. Original7.0 remains a different signing line; no uninstall/data-clear workaround is part of this work.

The directive explicitly forbids a release until foundational repairs and real account workflows have been addressed. A compiled/signed local candidate is build evidence, not release approval or proof of the directive's physical-device cases. No 11.2 APK is to be published while the release gates remain blocked. Existing 11.1 download remains the public release.

## Findings established from source

* Foreground offline recognition can post its final callback before its worker's resource-finally completes. Runtime pause/stop does not itself prove the old AudioRecord is released. Wake, foreground Android/Vosk STT and visible music/enrollment captures had separate ownership implementations. Added a common lease held until resource release, bounded busy recovery, and stale-release protection.
* Android recognition's ready/begin/end callbacks lacked consistent generation/completion guards. A stale ready callback could replace the current AudioSignals owner. Guards now cover those callbacks; final/result callbacks already had partial protection.
* Recorder construction/state checks and recording callbacks could fail before entering the old cleanup block. Managed construction now releases partial resources and leases on failure, and unregisters recording callbacks before closing.
* Wake capture previously stopped its service after the first uncaught reader/recognizer exception. Added a small retry budget only inside an already-authorized foreground service, with resource cleanup and visible recovery. Permission, native-library and memory failures require user action; no boot/background permission bypass is introduced.
* Every foreground Vosk capture loaded another Model. A single reference-counted cache avoids repeated loading and permits safe idle release; it does not claim a measured Redmi speedup.
* Ordinary history was inserted into Room and reloaded into the process by default. New turns now go only to a bounded RAM store. Old rows are preserved and not displayed; explicit history deletion remains separate from memories/settings. Automatic archive expiry is disabled because deletion must remain user-approved.
* App matching already had edit/phonetic scores, but did not inspect available alternatives or context-dependent cloud/Claude spelling. Added intent-constrained, actual-entity correction and preserved sensitive numbers/recipient text. Ambiguity asks rather than launching a different app.
* Generic launch calls used startActivity and reported an opening request without distinguishing background restrictions. Added structured dispatch/notification outcomes and explicit saved-package lookup when launcher discovery alone omits an approved app. Dispatch still is not proof of foreground completion.
* Speech queued unlimited sentence strings. The queue is now bounded; overflow reports readable text rather than silently substituting a different voice.

These findings establish source defects/risks and intended repairs. They do **not** establish the physical cause of the user's reported microphone failures, one-to-two-minute delays, crashes, TV recognition errors or app-launch failures. No handset trace, logcat, heap dump, ANR/tombstone or real provider account has been supplied or reproduced here.

## External verification gates

* User confirms Google Cloud/Gmail API/Android OAuth registration is **not set up yet**. Official Google authorization SDK21.3.0 is integrated in source; real Google consent, token grant, mailbox queries and revocation remain blocked pending registration and phone tests. No Google password or developer secret belongs in this app or chat.
* No physical Redmi9, headset/accessory or connected provider account is available. No real recognition/latency/memory/crash success is claimed. Prior preserved API29 emulator stalled in system_server/package commands; it is not reset to fabricate passing tests.
* Fish IDs remain English `05b36da8574341d0803391491850db20` (editable) and exclusive Spanish `ae74d1059517440aa6e5d2d50598d179`. Provider ownership is user-confirmed; authorized metadata, quota and audible playback need the user's private phone setup.
* Superdesign is outside this environment's configured network destinations. Existing native artwork is preserved; no inaccessible asset content is invented.

## Additional reviewed repairs

* Approved aliases now outrank conflicting installed labels. A missing saved target cannot fall through to a different same-named app. Voice correction carries the resolved package through dispatch instead of resolving the display label again.
* A successful or failed Fish turn retains the requested follow-up where permitted. Foreground-only follow-up works without enabling background wake; stale generation, permission, camera and private-system-UI checks still apply. Synthesis/playback state reflects real callbacks.
* Native speech is segmented; AI sentence buffering is bounded. Temporary files are deleted per segment. Startup cleanup selects older owned speech files only, avoiding newly generated audio from the current process.
* Google metadata needs Android authentication and a secure lock; mailbox reading requires a separate, bounded authenticated session. Late or abandoned OAuth results do not reconnect a stale request or silently restart its microphone.
* Folder browsing uses a bounded child cursor rather than listing every child before taking the first100. Search traverses a limited authorized subtree. Personal-content cloud consent applies to the next selected question only.
* Diagnostics record failure class and OS exit/memory metadata without raw messages, stacks, emails or transcripts. These facilities help later reproduction and do not establish a physical cause by themselves.

Final implementation boundaries and release gates are in IMPLEMENTATION-REPORT.md and RELEASE-GATES.md. Final host build evidence is in VERIFICATION.md; all required phone acceptance cases remain explicitly unexecuted in ACCEPTANCE-MATRIX.md.
