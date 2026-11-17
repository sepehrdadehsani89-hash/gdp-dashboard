# Phoenix preview package conflict repair

The first Phoenix debug APK used `com.jarvis.next`, the same application ID as the retained 2.11.8 release, but a different signing certificate. Android correctly refuses that combination as an update. A different download URL alone cannot repair this APK identity conflict.

The retained 2.11.8 APK certificate SHA-256 is `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. Phoenix debug uses `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`. Both were independently inspected with `apksigner`. This is evidence about repository artifacts; no installed phone package has been inspected.

Source checkpoint `f60ae0b2a59b695d14f90caf77bdaeef2351c279` isolates debug builds as `com.jarvis.next.phoenix`, labeled **Jarvis Phoenix Test** in the existing English, Spanish and Persian resource configurations. Version is `2.11.9-phoenix.2-debug`, code 27. The existing source, core artwork, wallpaper renderer, database schemas, dependencies and release signing configuration remain in place. Production application ID remains `com.jarvis.next`.

Provider authorities, the generated private receiver permission, screen capture task affinity and Spotify callback use the preview identity. The native OAuth callback accepts only its current application ID; it rejects the sibling app's callback. Existing Spotify OAuth configuration for the release is unchanged. To authorize Spotify in the preview, its owner must register `com.jarvis.next.phoenix://spotify-callback` separately with Spotify.

Instrumentation fixture targets now follow the application under test. The folder fixture validates its target package and provider ownership before granting its synthetic document. No instrumentation has been run on a customer device. Existing accessibility instrumentation changes global secure settings and should only run on an isolated test runtime.

This is a side-by-side preview, not an in-place release update. It has separate Android app storage and starts with its own settings, permissions and account/pairing configuration. The installed original app and its data must be retained. No uninstall, data clearing, migration, signing-key replacement or device installation was performed. Updating the original installation in place still requires its original matching signing material.

Local rollback branch `checkpoint/phoenix-before-package-isolation-1c2e1bb` preserves the previously published checkpoint. Earlier rollback branches and all historical APKs are retained. GitHub `jarvis-next` was checked read-only at `1c2e1bb05301cebaa2598c892eb52acf86164494` during preparation. A new push requires owner confirmation under the Phoenix directive.

## Verification scope

The package-fix regression run executed 678 JVM cases: 677 passed, zero failed, one optional private-export fixture skipped. The new callback-identity case and existing origin/state/PKCE cases passed under the preview ID. Per-case results and original JUnit XML are in `TEST_EVIDENCE/PHOENIX_PACKAGE_FIX/`.

No Android runtime is attached. Device installation, launcher appearance, existing app/data retention on the handset and real core/wallpaper/audio behavior remain unexecuted device checks. Artifact identity inspection does not substitute for them. The wider Phoenix feature and hardware limitations in `PHOENIX_PROGRESS.md` still apply.

## Verified artifact

`downloads/phoenix-2.11.9-preview2/Jarvis-2.11.9-phoenix.2-debug-arm64.apk` is **55,353,313 bytes**, SHA-256 **f49062fb7d2abfe14c208cb2befa222eea19d006eb4c1250ea12f83c3cd852e1**. Its copied download file matches the assembled APK checksum. The previous 2.11.8 and Phoenix.1 APK checksums were recorded again; neither file was replaced.

The build completed **BUILD SUCCESSFUL in 10m 2s**, with 40 tasks executed and 28 up-to-date. It included JVM tests, debug lint, Kotlin and Java Android-test compilation, and APK assembly using the existing toolchain and resource-limit script. Lint reports zero errors/fatals, 48 warnings and one information finding. No dependency upgrades, large installs, emulator or new signing key were introduced.

**20 artifact inspection checks passed**, covering package/version/SDK/labels, launch activity, provider and declared-permission isolation, absence of shared UID, task affinity, callback isolation, wallpaper declaration, signature/certificate, ZIP alignment/CRC, ARM64 ELF architecture, preserved native libraries/assets and download size. All **134 native-library/asset files** from Phoenix.1 were preserved byte for byte. These are packaging checks, not on-device feature tests.

Evidence is in `TEST_EVIDENCE/PHOENIX_PACKAGE_FIX/`, with per-test JVM CSV/XML, lint XML, final manifest, build log, Android-test manifest, output metadata, the inspection script and its JSON result. The first inspection helper reported two parser failures because aapt2 prints `minSdkVersion` and zipalign prints `Verification succesful`. Both tools themselves succeeded. Those original findings are retained; the parser was corrected and inspection completed with 20 PASS / zero FAIL without rebuilding or changing the APK. Raw tool logs remain in `/workspace/work/jarvis-phoenix-package-fix`; the tracked console copy strips trailing whitespace only.

The prepared artifact is not yet published. Under the owner's explicit publication rule, the next step is confirmation of the final normal Git push, then an independent download/checksum check of the new immutable GitHub URL.
