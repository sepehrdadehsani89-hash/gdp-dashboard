# Jarvis 5.2 troubleshooting

Keep the installed application and its data. A failed installation or connection is not a reason to uninstall, clear storage, change package identity or regenerate signing/TLS keys.

## Android says “App not installed”

The preview is `com.jarvis.next.phoenix`, versionCode 33, `5.2.0-preview.1-debug`, ARM64, minimum API 29. Its signer must match the retained Phoenix certificate shown in [setup](JARVIS_SETUP_GUIDE.md). Android 12 meets the minimum SDK. Those static facts do not establish what is installed on your particular phone.

This APK can be a normal update only when the installed package has a compatible signing identity and version. It cannot update the original production `com.jarvis.next` identity with a missing different key. Do not generate a random replacement certificate or uninstall the old application to conceal a mismatch. The cloud workspace has no connected Redmi and has not measured its installed package/certificate or obtained its exact Package Installer rejection code.

Download the complete published APK and compare its SHA-256 with `SHA256SUMS`. Check free phone storage and the actual installer message. Do not use an old cached link as the new version. Retain the downloaded APK while diagnosing it.

If you have Windows Android platform-tools, Android 12 can optionally use **Wireless debugging** without USB on a mutually reachable private Wi-Fi network. This is development diagnosis, not a companion requirement. Enable Developer options and Wireless debugging, choose **Pair device with pairing code**, and run locally:

```powershell
adb pair PHONE_IP:PAIRING_PORT
adb connect PHONE_IP:DEBUGGING_PORT
adb devices -l
adb install -r .\Jarvis-5.2.0-preview.1-debug-arm64.apk
```

Use the currently displayed separate pairing/debugging ports. Enter the short-lived pairing code only into your local ADB prompt; old codes expire. Do not post it publicly or expose ADB to the internet. The `install -r` command requests an update preserving storage; do not add uninstall, clear-data or downgrade switches. Keep the complete actual `Failure [...]` line. Disable Wireless debugging afterwards. If the two networks cannot reach each other, cloud ADB cannot establish a connection merely from a code.

Interpret actual errors: `UPDATE_INCOMPATIBLE` needs the installed authorized signer; `VERSION_DOWNGRADE` needs actual installed version measurement; `NO_MATCHING_ABIS` needs compatible native libraries; `OLDER_SDK` needs a supported OS; `INVALID_APK`/`MISSING_SPLIT` needs complete valid packaging; `INSUFFICIENT_STORAGE` needs normal user-managed free space. A generic installer dialog alone does not identify one of these causes.

## Scanner fails or permission is denied

Use Scan on the real pairing screen and inspect its scanner trace. Google Code Scanner needs working Google Play services. A failure/unavailability permits the actual CameraX/bundled ML Kit fallback, which needs Android Camera permission; cancellation is not silently retried. If denied, use the offered normal app-permission settings and return deliberately. No permission is granted automatically. Retry initializes the fallback camera again; flashlight appears only where supported. Gallery/manual entry remains available. A gallery fixture test cannot prove a physical camera works on MIUI.

## Invitation is invalid or expires

Use `invite` in the companion for a fresh two-minute invitation. Transfer the entire private `JARVIS-PAIR-1:` text, not its filename/address/pin. Do not edit the embedded endpoint, secret, protocol or certificate. ASCII line wrapping is accepted; malformed UTF-8/base64/schema, public hosts and wrong pins are rejected. Check both device clocks. Expired invitations/challenges require a new deliberate pairing, not extended validity.

## Request creates no pending ticket

Leave the intended companion terminal running and confirm its actual private IPv4/port. Start with `-Bind` on that private address rather than loopback. Check private-network firewall permission, guest isolation and VPN routing. Run Android's real diagnostics and retain its exact category (network, timeout, TLS, malformed protocol, rejection). A reachable generic HTTPS page or browser warning does not prove the correct companion identity or create a ticket. `pending` now explicitly reports no pending requests. Approve only the real matching ticket and comparison code. A denied/expired ticket is not success.

## Saved computer appears offline

Saved pairing and current authenticated proof are separate. Run **Check actual connection**, keep the companion running and verify its unchanged endpoint/certificate. The 60-second proof expires deliberately. Discovery is an untrusted hint and may be blocked by multicast policy; it cannot silently rewrite trust or select a different computer. IP changes may require explicit reconfiguration/pairing. Do not delete the companion identity to repair discovery.

## Google account does not connect

Follow [Google Cloud setup](JARVIS_GOOGLE_CLOUD_GUIDE.md) for the actual package and signing SHA-1. You have not yet provided a configured Google Cloud project. Saving project/client fields or an email does not establish authorization. Google status 10 commonly requires checking package/certificate/client/consent registration. A private API 401 needs reauthorization; 403 needs scope, enabled API, eligibility or administrator policy review. API-key restrictions/quota affect only the permitted key endpoint; a key cannot read private Gmail.

The native Google account chooser and per-service permissions must complete. Test Gmail/Calendar/Drive independently and inspect actual data or a truthful empty response. Account switch, revoked permission, lock and stale grant callbacks invalidate current proof. Private summaries require a new one-read review and selected-provider consent. No Gmail watch/Pub/Sub/backend has been deployed; notification previews are separate Android data, not real Gmail unread counts.

## AI is silent or says connected without answering

Verify Android TTS output with the existing preview and confirm that you really hear it. Check media volume, selected output/voice, local microphone permission and the actual recognition transcript. Select the intended provider/model, save its authorized API credential privately in the application and run an actual model test. ChatGPT subscription/login alone does not grant OpenAI API usage. API credits on your phone were not available to this cloud test process, so no live inference result is claimed here. Never paste keys into chat or logs.

Provider 401/403, model access, billing/quota, network and inference failures must remain distinguishable. A local unit HTTP fixture is not live provider verification. Local Brain requires its explicit existing model installation, separate from offline STT/wake packs. There is no silent paid-provider fallback. A genuine farewell finishes speech before conversation capture stops; optional wake listening is separate. Use **Stop All Listening** to stop both. Verify actual microphone release on the phone.

## Mode or wallpaper seems stuck

Silent Mode and Deep Shutdown are intentional persistent modes. Manual main-app opening reactivates Deep without automatically granting/restarting microphone access. Mode presentation is temporary and does not overwrite saved wallpaper settings. Explicit Reduced Motion can intentionally stop animation; hidden wallpapers pause expensive rendering. Android DND is a separate permission-controlled OS setting and is not silently restored when Jarvis changes mode.

Wallpaper Studio is opt-in. Restore Original composition to use the preserved renderer while retaining custom profiles. Apply/select the actual live wallpaper through Android's chooser. Profiles do not replace the phone's system UI. Unknown widget sources show unavailable. Private widgets need an unlocked active device, profile opt-in and current source permission; Google read snapshots expire after five minutes. Wallpaper FPS, thermal behavior and home-screen persistence still need Redmi testing.

Corrupt mode storage fails closed in Deep; the explicit authenticated recovery keeps the prior raw value privately and does not erase accounts/layouts. Do not delete the application's storage as a workaround. Retain a redacted diagnostic report and the exact operation that failed.

## Windows launcher or browser fails

Use the existing private Python environment where available and preserve the private state directory. A failed dependency install stops without repeated automatic retries. Share only the actual redacted error, not pairing material or keys. Native Windows launcher execution is not verified by Linux tests. Chrome missing, actual page-load timeout, consent/interstitial redirects, denied command and wrong signed receipt are failures, not completed actions. Only Chrome opening and Google search are implemented; Windows power/media/files/shell actions are unsupported.

When reporting a problem, include APK version/package, Android version, exact redacted failure, last completed pairing stage and whether the test used a real phone/Windows computer or a software fixture. Never claim a physical test passed from a screenshot, compiled test or mocked response.
