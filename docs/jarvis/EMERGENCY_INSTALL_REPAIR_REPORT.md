# Emergency update-installation audit — blocked on the Redmi 9 result

Baseline/source: `83adccdb8cee8d28b9a942438783c229c2188662`, branch `jarvis-next`. Local rollback: `checkpoint/install-repair-start-83adccd`.

The owner reports Android recognizes Jarvis, offers **Update**, then displays **App not installed**. This is a real reported failure. The earlier software/artifact checks did not establish that the installed phone package would accept an update. The actual rejection cause remains **unknown**, not an assumed certificate or version mismatch.

## Fresh repository APK comparison

Both preview archives were independently re-inspected using Android SDK `apksigner verify --verbose --print-certs`, `aapt2 dump badging`, `aapt2 dump xmltree`, ZIP integrity checks and native ELF inspection. These are repository APKs; neither is proven to be the precise version currently installed on the customer's phone.

| Field | Rejected candidate | Previous published preview |
| --- | --- | --- |
| Package | `com.jarvis.next.phoenix` | `com.jarvis.next.phoenix` |
| Version name | `2.11.9-stabilize.1-debug` | `2.11.9-brain.1-debug` |
| Version code | 30 | 29 |
| Signing certificate SHA-256 | `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7` | Same fingerprint |
| Signature verification | PASS | PASS |
| Minimum / target SDK | 29 / 35 | 29 / 35 |
| Native ABI | `arm64-v8a`, 14 ELF64 AArch64 libraries | Same ABI/count |
| ZIP CRC | PASS | PASS |
| sharedUserId | Absent | Absent |
| Required split declarations | Absent; standalone APK | Absent; standalone APK |
| testOnly attribute | Absent | Absent |

The packaged manifest diff changes only versionCode and versionName. The candidate's resources.arsc is uncompressed and four-byte aligned. All 18 candidate DEX files pass file-size, Adler-32 and SHA-1 integrity checks, and the SDK `dexdump -c` parser exits successfully for all 18. The candidate hash is `ad796f1920f4853ff855acd710d1360bfa32a1e185ca733d4850c071ab7261ed`, size 56,199,459 bytes. No physical or emulator installation is inferred from these results.

The historical `2.11.8` archive is a different package, `com.jarvis.next`, code 25, with certificate `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. It is not interchangeable with the Phoenix identity. No package change or replacement signing identity is proposed as an installation workaround.

## Missing device evidence

No accessible Android device session is available to this task. Cloud ADB aborts before connecting with `Cannot mkdir '/home/agent/.android': Read-only file system`. No `adb install`, phone-specific PackageManager error, installed-phone certificate/version inspection, emulator installation, data-retention test or Redmi 9 runtime test occurred.

The required next evidence is the actual PackageManager rejection from the existing device and its installed version/SDK/ABI. The **Update** dialog does not expose signing lineage, installed versionCode or the reason installation failed. A certificate match against the previous repository archive does not prove a match against the installed phone package. VersionCode 30 is higher than the archived 29; its relationship to the actual installed version is unverified.

[Windows collection instructions](WINDOWS_INSTALL_DIAGNOSTICS.md) use normal `adb install -r`, with one explicit owner-run attempt, an exact candidate hash, device checks and retained error output. They do not uninstall, clear Jarvis data, change its package ID, force a downgrade, bypass verification or silently grant application permissions.

## Required report status

| Requested finding | Actual status |
| --- | --- |
| Why Android rejected the update | Unknown until the actual PackageManager result is obtained |
| Evidence establishing the rejection cause | Not yet available; static archive checks above exclude several malformed-artifact conditions but do not identify the phone failure |
| Files changed | New audit/report/instructions and evidence only; application files, APK bytes, package/signing identity and version untouched |
| Certificate matches installed version | Unverified; it matches the previous published preview archive |
| VersionCode suitable | Higher than archived code29; installed device value needed |
| Installation actually tested | No physical or emulator installation attempted |
| Existing data preserved | No device data was accessed, cleared or migrated. Normal accepted same-identity `-r` updates are intended to retain data; actual retention still requires device verification |
| Corrected APK ready | No corrected APK or new fixed-link claim. Choose the correction only after the rejection is known |
| AAB ready for Google Play | No. Original authorized production signing configuration is unavailable; existing SDK/native/publisher readiness blockers also remain |
| Owner involvement | Follow the Windows steps and supply the exact Failure line, installed version, SDK/ABI and, if needed, installed APK certificate/hash |

No new build, dependency/toolchain installation, emulator, signing-key generation, application-ID change or GitHub publication was performed during this audit. Existing working app/data and published artifacts are retained. The task remains blocked on the device-specific evidence; it is not marked as a successfully repaired installation.

Original tool output, APK comparison, manifest diff and per-DEX results are in `TEST_EVIDENCE/INSTALL_REPAIR/`. The full previous JVM and artifact results remain separate in `TEST_EVIDENCE/PRODUCTION_STABILIZATION/` and are not recast as device update tests.

## Connection constraint reported by the owner

The owner has no USB cable and is unsure of the Android version. The Windows guide now includes supported wireless pairing conditionally for Android 11+ with an available Wireless debugging setting. This is a possible diagnostic connection, not an assertion that this Redmi 9 supports it or that a connection occurred. The immediate owner step is reading the Android version under About phone → All specs. Actual installation error, installed signer/version and data-retention verification remain unavailable.
