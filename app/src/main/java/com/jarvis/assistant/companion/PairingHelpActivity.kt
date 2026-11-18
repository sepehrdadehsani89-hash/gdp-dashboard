package com.jarvis.assistant.companion

import android.os.Bundle
import androidx.compose.material3.Text
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

class PairingHelpActivity:PrivateModuleActivity(){
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        screen("JARVIS · COMPUTER PAIRING SETUP"){
            Text("1. On your own computer, keep the existing companion state directory, certificates and database. Stop the old visible process before replacing its source. Never delete ~/.jarvis-companion to upgrade.")
            Text("2. In the extracted companion folder on Windows, run ./start-windows.ps1 -Bind YOUR_ACTUAL_PRIVATE_IPV4. The existing port is 8765 unless you explicitly configured another. Do not use a public address or 0.0.0.0. The previously observed address is not assumed current.")
            Text("3. Keep the visible terminal open. Type invite to create a fresh private two-minute invitation. Display the pairing-qr.svg path it prints, or copy the entire pairing-invitation.txt payload. Never send either to chat, a website or a public repository.")
            Text("4. Tap Scan computer's one-time QR. Google Code Scanner requires working Play services and its scanner module. If unavailable, use the bundled camera scanner, grant Camera normally and retry. Gallery import decodes an actual PNG/JPEG locally; it does not upload images. A QR filename or the HTTPS address alone is not an invitation.")
            Text("5. Review the computer address and SHA-256 certificate fingerprint, then Request pairing. Complete Android's legitimate authentication if requested. A progress state or error must appear.")
            Text("6. On the computer type pending. It must list an actual ticket and six-character comparison code. If none exists, the phone request has not created an outstanding ticket: inspect its real error, endpoint and network. Compare both codes, then type approve ACTUAL_TICKET. Use deny ACTUAL_TICKET to reject.")
            Text("7. On Android check desktop approval, then Finish after approving. Older companions may lack the status route; approve there and finish here directly. Saved pairing is separate from authenticated live connectivity. Check the actual authenticated connection, then review one Chrome command and inspect the actual computer result.")
            Text("8. A certificate warning in a normal browser is expected for a private self-signed identity. Android instead validates the explicitly scanned certificate pin, dates and hostname. Never disable TLS checks. If an address changed, preserve the old identity and diagnose its SAN/host configuration rather than regenerating certificates silently.")
            Text("Network checks: use a reachable private LAN, avoid guest/client isolation, keep the companion running and review Windows' normal private-network firewall permission. No port forwarding, ADB or Android Wireless Debugging is needed for ordinary companion use.")
            Text("Implemented commands remain Chrome open/search only. No arbitrary shell, Windows shutdown/files/media, internet relay or private Google account access is granted by pairing. Phone-to-Windows physical acceptance has not been performed in the cloud.")
        }
    }
}
