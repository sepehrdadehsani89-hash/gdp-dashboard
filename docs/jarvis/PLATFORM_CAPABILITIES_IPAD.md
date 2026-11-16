# iPad companion: NOT_STARTED

No frontend/backend deployed or device paired in this checkpoint. Existing companion/ is a private-LAN Python desktop companion with pinned TLS, two-sided pairing and narrowly scoped Chrome actions. Preserve it.

| Requested capability | Contract classification | Project verification |
|---|---|---|
| Own-page chat/notes | SUPPORTED_FOREGROUND | NOT_IMPLEMENTED |
| Open approved URL card | SUPPORTED_WITH_USER_GESTURE | NOT_IMPLEMENTED |
| Service-specific account actions | SUPPORTED_WITH_ACCOUNT_OAUTH | ACCOUNT_PENDING |
| Closed-page push handoff | SUPPORTED_NOTIFICATION_HANDOFF, subject to supported Home Screen setup | UNVERIFIED |
| User-configured shortcut | OPTIONAL_SHORTCUT_WITH_USER_SETUP | UNVERIFIED |
| Remote unlock, global brightness/Wi-Fi, arbitrary app control while closed | UNSUPPORTED_BY_IPADOS_WEB | PLATFORM_RESTRICTED |
| Generic iPad battery reading from Safari | UNVERIFIED; do not substitute Android data | NOT_IMPLEMENTED |

These classifications come from Part 24, not fresh platform tests. Verify current official docs and real target Safari/iPadOS before implementation claims. No deployment URL exists for a new Safari companion.
