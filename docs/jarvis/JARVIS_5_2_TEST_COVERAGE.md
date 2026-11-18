# Jarvis 5.2 automated coverage

| Feature / boundary | Passed | Suite |
|---|---:|---|
| Invitation schema / expiry / normalization | 32 | `PairingInvitationTest` |
| Actual local gallery QR decoding | 22 | `QrImageDecoderTest` |
| Pairing lifecycle / busy / cancellation / diagnostics | 34 | `PairingCoordinatorTest` |
| Production Kotlin-client and real Python HTTPS interoperability | 28 | `CompanionTlsIntegrationTest` |
| Discovery untrusted-hint boundaries | 22 | `NearbyCompanionPolicyTest` |
| Saved registry / names / selection | 26 | `DesktopRegistryTest` |
| Current signed connection proof | 20 | `DesktopConnectionProofTest` |
| Unsupported computer actions do not become fictional execution | 24 | `DesktopUnsupportedActionsTest` |
| Encrypted Google Cloud configuration | 22 | `GoogleCloudConfigurationTest` |
| Public API-key HTTP response/security policy (mock server) | 24 | `GooglePublicKeyHttpTest` |
| One-read private AI summary consent/account/provider policy | 24 | `GoogleSummaryPolicyTest` |
| Real Gmail count observation/privacy boundaries | 20 | `GmailWidgetPolicyTest` |
| Google widget freshness/account/grant/lock boundaries | 20 | `GoogleWidgetBoundaryTest` |
| Silent / Deep / wake / reminder / presentation policy | 37 | `SystemModePolicyTest` |
| Mode persistence/corruption/write-failure boundaries | 20 | `SystemModesStoreTest` |
| Studio full-spectrum color transformations | 28 | `StudioColorTest` |
| Studio profiles/layers/transforms/privacy persistence | 36 | `WallpaperStudioStoreTest` |
| Optional setup review/skip never equals live connection | 22 | `NexusSetupPolicyTest` |
| Typed voice navigation never equals setting mutation | 22 | `NexusNavigationPolicyTest` |

Counts above come from the final raw JVM XML, not source annotation counts. Each group has at least 20 cases; tests are not multiplied into hardware claims. New Studio Android bitmap cases: 24. Bundled ML Kit Android cases: 20 parameterized scenarios. Both Android groups were compiled only and executed zero times. See the separate physical acceptance checklist.

The companion suite includes 22 additional browser-navigation cases, with separate checks for incorrect frame, loader, response type and URL, partial loads, failed subscriptions, cancellation and reordering. Its actual Chromium fixture exercised opening Chrome twice, a successful local search document and a 404 response. All 103 Python tests passed.
