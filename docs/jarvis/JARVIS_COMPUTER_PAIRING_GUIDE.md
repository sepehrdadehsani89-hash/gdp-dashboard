# Jarvis 5.2 computer pairing

The Android preview and companion agent 1.2 use the existing `JARVIS-PAIR-1` invitation and protocol version 1. Ordinary computer control needs the phone, a reachable private LAN and the visible companion process. It does not require a USB cable, Android Wireless Debugging, a computer microphone or computer speakers.

## Preserve the computer's identity

Stop the old companion using its `stop` command. Extract the new **companion source** bundle into a source folder. Keep the existing private state directory intact: normally `%USERPROFILE%\.jarvis-companion` on Windows or `~/.jarvis-companion` on Unix. It holds the TLS certificate/key, pairing database and dedicated Chrome profile. Do not delete or publish it. Missing identity files are not silently replaced. Updating source is distinct from changing this identity.

The source archive is not a Windows EXE or installer. Native Windows and macOS execution have not been performed in this cloud workspace. The controlled Linux protocol, HTTPS and browser tests are reported separately.

## Start on Windows

Use an existing Python 3.11+ installation and installed Google Chrome. The launcher first reuses `%LOCALAPPDATA%\JarvisCompanion\runtime\Scripts\python.exe`; it does not require a working `py` command when that environment already exists. If the environment is missing it looks for an existing `py`, `python` or `python3`. It installs only the pinned requirements that are missing or mismatched, without upgrading Python or pip. An installation failure stops the launcher without an automatic retry.

Find the computer's **current** private IPv4 address in Windows network settings or `ipconfig`. The earlier reported `192.168.100.74` has not been verified and must not be assumed current. In PowerShell, change into the extracted `companion` folder and run:

```powershell
.\start-windows.ps1 -Bind YOUR_ACTUAL_PRIVATE_IPV4 -Port 8765
```

Replace the placeholder with your actual private address. The port 8765 is the launcher's default; use the port actually printed if you changed it. The default loopback bind `127.0.0.1` cannot be reached by the phone. Public addresses and wildcard binding are rejected. Do not expose or forward this service to the internet.

If PowerShell or Python is managed by an organization, follow its normal reviewed procedure. The launcher does not disable security software, change execution policy, silently add firewall rules or install autorun. Allow only the necessary private-network traffic through the normal Windows firewall interface. Guest Wi-Fi, VPNs and access-point isolation can block phone-to-computer traffic even when both devices have internet access.

On Linux/macOS, use Python 3.11+ and a supported Chrome installation:

```sh
./start-unix.sh --bind YOUR_ACTUAL_PRIVATE_IPV4 --port 8765
```

## Scan or transfer the complete invitation

1. Leave the visible companion terminal open. It prints the private state path. Open its private `pairing-qr.svg` on that computer. The full text is in `pairing-invitation.txt`. Each invitation expires after two minutes; enter `invite` for a fresh one.
2. In Jarvis, open Settings → Rebirth → Paired Computers, or the Computers link in Connections/setup. Tap **Scan computer's one-time QR**.
3. Google Code Scanner is the primary real scanner. If its service is unavailable or fails, Jarvis offers/starts the bounded CameraX + bundled ML Kit fallback after Android camera permission. A user cancellation remains a cancellation. The fallback has a real camera frame, Cancel, Retry and flashlight where the camera supports it.
4. Scan the actual displayed QR. Alternatively choose a PNG or JPEG of the QR from the gallery for local decoding (a private screenshot of the displayed SVG works), or use manual entry / Paste invitation with the **complete** `JARVIS-PAIR-1:` payload. Do not paste the SVG filename, HTTPS address, certificate fingerprint, ticket or comparison code in the invitation box. Outer whitespace and ordinary line wrapping are normalized; truncated data is rejected.
5. Review the actual detected name, private address, expiration and SHA-256 certificate pin. The invitation is a private short-lived credential. Never upload it, its QR image, its secret or a permanent session token to chat, GitHub or a public image decoder.

## Request and approve on both devices

1. Tap **Request pairing** on Android. The screen enters a visible requesting state, then shows the real short-lived challenge or an explicit error.
2. On the computer, enter `pending`. A real waiting request prints its ticket, six-character comparison code, age, expiry and phone client-ID suffix. Empty output is explicitly **No pending pairing requests**. Public HTTPS reachability alone does not create a ticket.
3. Compare the code on both devices. Approve only your matching request:

   ```text
   approve ACTUAL_TICKET
   ```

   Substitute the exact ticket from `pending`. Do not substitute the URL, certificate pin or comparison code. To refuse it, use `deny ACTUAL_TICKET`.
4. Return to Android, check approval status and finish after approval. Saved Computers should increase to 1/20 for the first new computer. Existing names/aliases and previous saved computers are preserved. A legitimate lost-response retry reconciles the same request/session during its original lifetime; it does not extend the invitation or authorize a different requester.
5. Tap **Check actual connection**. Only a valid authenticated, signed, nonce-bound capability response establishes current connection proof. Proof expires after 60 seconds. A saved pairing is persistent trust, not a perpetual online indicator. Select the intended saved computer explicitly.
6. Ask to open Chrome on the selected computer or perform the supported Google search. Review the typed action and confirm it. Inspect the real computer result. An authenticated capability check is not proof that a browser command completed.

## What this companion can do

Implemented actions remain **open Chrome** (`desktop.open_app`) and **search Google in Chrome** (`desktop.search_web`), in a separate Jarvis Chrome profile. Signed receipts bind command bytes, device, client and session; page-load evidence is checked separately. Normal Chrome profiles/cookies are not imported. Search can encounter Google consent or network errors; the agent does not click account consent on your behalf.

Native Windows media, volume, file/folder access, arbitrary application launch, lock/sleep/restart/shutdown and unrestricted shell execution are **UNSUPPORTED** here. Unsupported commands are rejected rather than passed to AI as invented execution. No AI-generated shell server is enabled.

## Diagnostics and recovery

**Run Pairing Diagnostics** uses the actual invited pinned HTTPS health endpoint for an unpaired invitation, or the actual authenticated capability check for a saved computer. It reports scanner availability/events, permission state, validation, expiration, TLS and protocol progress separately. Public health success is explicitly unauthenticated. **Export Redacted Logs** excludes the invitation secret, full invitation, proof, ticket, comparison code and session token. Review any exported address/device metadata before sharing it.

Cancellation/backgrounding clears temporary pairing work; rotation keeps the current in-memory coordinator without putting secrets into Android saved state. Process death clears the in-memory invitation/challenge. Generate a fresh invitation if its lifetime has elapsed. Saved pairings survive process restart, but need a new actual authenticated check.

mDNS/Android NSD discovery advertises `_jarvis-pc._tcp.local.` with public device/protocol hints. Discovery is best effort, bounded and foreground-only on Android. It cannot authorize a device, change a certificate, rewrite a saved address or silently change the selected computer. QR/manual pairing remains available if multicast is blocked. A changed LAN IP may need explicit re-pairing because certificate hostname validation stays enabled.

If the certificate mismatches or is expired, stop and inspect the computer identity/clock. Never bypass TLS validation or install a browser certificate exception as a pairing fix. A browser self-signed warning is expected for a private certificate but does not prove that Jarvis has authenticated it. If no ticket appears, use the phone's exact typed request failure rather than repeatedly approving unrelated values.

Unpair attempts remote revocation before forgetting credentials. If offline, **Forget locally** removes only phone trust: also use `revoke-all` on the computer when available. `revoke-all` invalidates current phone sessions, invitations and pending requests. `stop` closes the server and its owned Chrome instance, without deleting the private state. Cancellation cannot undo a page already opened.

The [physical acceptance checklist](JARVIS_5_2_ACCEPTANCE.md) records the complete phone-to-Windows sequence separately from the cloud fixtures. No physical Redmi/Windows pairing has yet been verified for this preview.
