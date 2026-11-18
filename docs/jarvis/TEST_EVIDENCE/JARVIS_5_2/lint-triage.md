# Lint findings retained

Final lint has zero fatal/error findings, 74 warnings and one information finding. Warnings were not hidden to manufacture a clean report.

The three TrustAllX509TrustManager findings point into a retained BouncyCastle dependency JAR, not a new trust-all application client. The custom trust-manager warning points at the retained narrow companion transport: it rejects a different exact certificate pin, requires a single leaf certificate, checks dates and keeps OkHttp hostname/SAN verification. New actual TLS tests reject bad pin, wrong host, expired and not-yet-valid certificates. No global trust exception was added.

The retained Location HUD WebView JavaScript finding needs its existing map functionality; it is not a new pairing bypass. Existing WebView/content boundaries remain a separate review surface. Other warnings include Compose names, localized text, Gradle version-catalog style, draw allocation, ABI targeting and legacy API/settings patterns. ARM64-only sideload support and unmeasured device rendering are explicitly disclosed. These warnings are not evidence of physical acceptance or production Play readiness.
