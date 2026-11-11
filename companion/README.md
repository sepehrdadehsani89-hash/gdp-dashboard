# Jarvis local companion — 2.11.6 development milestone

This is the actual narrowly scoped companion used by the Android paired-computer screen. It opens Chrome in a **separate Jarvis browser profile** and can perform a Google search. The computer needs no microphone or speakers. Android captures the request and speaks the verified receipt. It does not control other apps, files, passwords, arbitrary terminals, existing browser profiles or remote internet desktops.

## Start on your own Windows computer

1. Install Python 3.11 or newer from [python.org](https://www.python.org/downloads/windows/) if it is not installed. Chrome must also be installed.
2. Extract the companion source ZIP. In Windows Settings → Network & Internet → your connection's properties, note the private IPv4 address, such as `192.168.1.8`. The phone must be on a network that can reach that computer; guest Wi-Fi may isolate devices.
3. Open PowerShell in the extracted companion folder and run `./start-windows.ps1 -Bind 192.168.1.8`. Substitute the computer's actual address. The launcher creates a private Python environment and installs the pinned requirements. It does not configure autorun, disable security software or silently add a firewall rule. Follow Windows' normal private-network permission prompt if one appears.
4. Leave the visible companion terminal open. Open its **private `pairing-qr.svg` file** from the path printed in that terminal. The QR expires in two minutes. Never upload that file, the text invitation or the private state folder to GitHub/chat.
5. On Android: Settings → Rebirth → Paired computer → Scan computer's one-time QR. Alternatively paste the private invitation text directly into that screen. Review the displayed computer address and certificate, then request pairing.
6. Compare the six-character code on the phone with the computer: enter `pending` in the companion terminal. Enter `approve TICKET`, substituting the matching ticket printed there. Tap **Finish after approving on the computer** on Android.
7. Tap **Check actual connection**. Then say “Hey Jarvis, open Chrome on my computer and search for ChatGPT.” Review and confirm the requested action. The phone reports success only after authenticated receipt validation and a verified loaded browser page.

If Python/PowerShell is managed by an organization, use its reviewed installation procedure. No administrative shell endpoint is part of this companion. The default bind is loopback and cannot be reached from the phone; explicitly select the computer's private LAN address. A public IP or `0.0.0.0` is rejected. Do not port-forward it to the internet.

## Linux/macOS

With Python 3.11+ and a supported Chrome/Chromium installation, run `./start-unix.sh --bind 192.168.1.8` using the computer's actual private IPv4 address. macOS's standard Google Chrome path and common Linux Chrome/Chromium executable names are detected. `--chrome-path` is an operator-only option for an installed trusted browser. Native Windows/macOS execution has not been verified in the cloud build; Linux was tested with a controlled local Chromium page.

## Controls and failures

- `pending`: list waiting phone requests and comparison codes.
- `approve TICKET`: explicitly approve the matching phone.
- `invite`: generate a fresh two-minute invitation.
- `revoke-all`: revoke all phone sessions and request cancellation of in-flight work.
- `stop` or closing the terminal: close the server and its own Jarvis Chrome instance. Ordinary Chrome profiles are not touched.

On the phone, Unpair revokes the remote session before forgetting it. If the computer is offline, Forget locally removes the phone credentials; use `revoke-all` on the computer as well. Stop/cancellation cannot undo a page that has already opened. Request IDs prevent duplicate execution of the same request; after a lost response, check the desktop before approving a new command.

Missing Chrome, expired pairing, wrong certificate, wrong session/device, replay, timeout, non-2xx page, consent/interstitial redirect, and unsupported actions are actual errors. Merely dispatching a browser process is not success. Google can require you to review consent on the computer; this agent never clicks through account consent or accesses your Google account. Health checks establish only protocol connectivity.

## Privacy, identity and packaging

Pairing uses private-LAN HTTPS with an explicitly scanned SHA-256 certificate pin, normal certificate validity/SAN checks on Android, proof of possession, approval on both ends and random session credentials. Signed receipts bind the exact command bytes, session, client and target device. Only two actions are allowed; no arbitrary code or shell request is accepted. Requests are bounded and rate-limited; the server permits at most eight concurrent connections.

The Android token is encrypted in the existing Android Keystore-backed vault. The desktop stores its TLS identity and a database of hashed tokens and minimal receipt metadata under its private state directory, normally `~/.jarvis-companion`. Unix permissions are restricted; Windows relies on the current user's profile ACLs. Desktop OS keychain integration/hardening still needs native verification. Search text is not stored in the receipt audit; Chrome's dedicated profile can retain its ordinary browsing history. No old browser cookies/profile are imported. Back up the entire private companion state separately if needed; do not publish it. Missing identity files are not silently replaced. A changed LAN IP can require an explicit re-pairing/reconfiguration because certificate host verification remains enabled.

The included GitHub Actions workflow **Build Jarvis companion on native hosts** is manually triggered. It tests the protocol and packages a CLI binary with PyInstaller on Windows, macOS and Linux, then records checksums. This cloud release supplies the source bundle and tested Linux protocol/browser fixture; it does not claim that Windows EXE, macOS signing/notarization, iOS distribution, or a production native installer has already been built or approved. Native installers and platform UI/permissions remain a later milestone.

Tests: `python -m unittest discover -s tests -v`. The actual-browser fixture is Linux-only and uses a local test page, not Google or a personal browser profile. The `--no-sandbox` argument exists only in that isolated cloud test fixture; production launchers retain the browser's sandbox.

## Mission Control readiness update

The current companion adds an authenticated, nonce-bound capability manifest for Android Mission Control. It checks Chrome presence without opening a browser and separately reports unsupported file/app/screen/shell capabilities as unavailable. Each action still needs review and its own verified browser receipt; a health/capability check is never completion. Revocation immediately disables capability access too.

Stop the old visible process and replace its source files, preserving the existing private state directory and IP/TLS identity, then restart normally. Do not delete pairing data or generate a new identity to update it. The new Android client keeps the legacy Chrome command path working when an older agent lacks readiness discovery, while clearly marking that readiness unknown.

Current controlled verification: 36 Python tests, including local HTTPS and an actual isolated Chromium fixture, passed. Windows/macOS native runs, installers, Google results pages and Android audio round trips remain unverified. See [Mission Control implementation and capability matrix](../docs/finish-polish/Mission.md).
