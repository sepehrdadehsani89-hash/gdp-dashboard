# Google Play readiness — 17 November 2026

User account status: Jarvis is not published anywhere. Play Console enrollment, account type, legal publisher identity, privacy-policy URL and support/reporting endpoint are not available in this workspace. The user answered that the original signing keystore is unavailable or uncertain. Do not submit, replace the signing identity or invent account information.

## Actual blockers

| Check | Evidence | Status |
| --- | --- | --- |
| Original signing | Release uses `next`, requires JARVIS_NEXT_SIGNING_DIR and expected retained certificate; not configured | Blocked; never create a substitute key |
| Target API | compile/target 35; current new app/update submission requirement is API 36 | Migration and Android 16 behavior tests needed |
| Native compatibility | ELF inspection of all 14 baseline ARM64 libraries: 13 compatible with 16 KB alignment; `libvosk.so` uses 4 KB | Compatible Vosk native build/vendor update plus device tests needed |
| Installation | Retained Phoenix preview signer and isolated application ID | APK artifact checks and phone update test required |
| Microphone | RECORD_AUDIO + microphone foreground service declared; one-time visible opt-in, no silent grant/boot start | Actual foreground/background/OEM tests pending |
| Accessibility | Existing optional service uses BIND_ACCESSIBILITY_SERVICE | Owner must complete applicable declaration, prominent disclosure/consent and permitted-use review; do not label Jarvis an accessibility tool without eligibility |
| AI content | Real model connections and provider errors remain; no dedicated developer report service configured | In-app offensive-content reporting and processing endpoint needed before publication |
| Data Safety | Encrypted credentials, explicit private-speech consent, camera/location/contact permissions | Publisher must review actual SDK/network data flows, retention, consent and current Play form |
| Account deletion | Local data wipe/disconnect exists; no Jarvis-hosted app account service | If app account creation is introduced, add required in-app and web deletion request paths |
| Privacy policy | No owner-supplied public URL/legal details | Required public policy and in-app access pending |
| Crash reporting | Local diagnostics; no crash-reporting SDK added | Do not claim remote crash telemetry or fabricate results |
| Testing eligibility | Developer account type/creation date unknown | If new personal-account rule applies, 12 continuously opted-in testers for 14 days and production-access application are required |
| Physical quality | No connected phone, emulator, camera, microphone, GPU or live provider accounts | All physical/release acceptance scenarios remain unexecuted |

The current NDK/AGP/Gradle stack and vendor binaries are preserved for the working test build. API 36 migration and Vosk replacement must be evaluated together rather than setting an unsupported target or removing working offline recognition. No Play-ready AAB or Google approval is claimed. A five-day production-publication promise would be unsupported, particularly if mandatory closed testing applies.

## Official references reviewed

- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878): new apps and updates require API 36 from 31 August 2026; relevant extension ended 1 November.
- [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes): inspect every native library and test on a 16 KB runtime. Current guidance states updated enforcement on 1 February 2027; native incompatibility is still a release risk now. ELF checks alone are not device verification.
- [Microphone foreground services](https://developer.android.com/develop/background-work/services/fgs/service-types): microphone type/permission and while-in-use startup restrictions apply.
- [AI-generated content](https://support.google.com/googleplay/android-developer/answer/13985936): relevant content protections and in-app reporting to the developer are required.
- [New personal account testing](https://support.google.com/googleplay/android-developer/answer/14151465): conditional 12-testers/14-consecutive-days requirement; no account-type assumption.
- [Account deletion](https://support.google.com/googleplay/android-developer/answer/13327111): applies where the app enables app-account creation.
- [Accessibility API](https://support.google.com/googleplay/android-developer/answer/10964491), [User Data](https://support.google.com/googleplay/android-developer/answer/10144311) and [Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469): publisher declarations and disclosures must describe actual behavior.

These references guide an engineering readiness review, not a Play policy approval. No user data or application code was sent to the reference websites.
