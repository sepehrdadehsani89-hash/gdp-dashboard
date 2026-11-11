# Security and privacy review

The existing Android package, retained signer, private vault and prior data remain. All new stores are additive versioned records. No provider key, signing material, private backup, raw microphone sample or screen capture is added to source or APK.

The four engines use bounded typed actions and explicit approval. Learning is off by default, exact reviewed aliases map to actual installed package IDs or a specific paired device. Recipe/profile revision, selected device/session and expiry are checked again before each dispatch. Changed approval cannot redirect later steps. Unsupported shell, destructive files, arbitrary paths and remote monitoring are not actions in this registry. A local OS dispatch is labeled unverified unless an explicitly enabled existing accessibility service observes the requested foreground package; it reads package identity only in that check.

Doctor only claims verified preference readback, not a repaired microphone. Repairs have a reviewed proposal, encrypted baseline, bounded retry history/cooldown, deadline and rollback. OS permissions remain Android-controlled. Unknown diagnostics stay unknown. Metadata sharing uses the user's explicit Android chooser.

Predictive sources require opt-in and current permissions. Calendar titles stay local. The one suggestion is hidden on lock/privacy transitions, expiry, opt-out or suppression. Optional voice announcements use fixed generic public wording and do not start a microphone service. Acceptance produces another reviewed plan rather than executing a significant action. No inferred location, sensitive identity or ambient training collection is introduced.

Mission transport retains private-LAN HTTPS, scoped TLS pin and hostname verification, explicit pairing, hashed random session credentials, nonce/expiry/idempotence, rate/body limits and signed request-bound execution receipts. The capability response is authenticated and nonce-bound. Readiness does not prove execution. The actual Chrome test uses a separate temporary profile; `--no-sandbox` is test-only for the controlled cloud fixture, never a production launcher setting. Desktop changes cannot access the ordinary personal Chrome profile.

Global explicit data erase now clears new in-memory learning/prediction state after vault deletion, avoiding old opt-ins surviving in process. No existing model, profile or dataset is removed during this upgrade. Private storage failures fail closed without overwriting encrypted data, and must not block unrelated deterministic local answers.

Outstanding review gates: physical Android permission/lifecycle/notification/voice tests, native desktop installers and platform adapters, actual browser/account interstitial behavior, strong multi-account product isolation and broader file capability design. This source review and software suite are not a penetration-test or commercial readiness certification.
