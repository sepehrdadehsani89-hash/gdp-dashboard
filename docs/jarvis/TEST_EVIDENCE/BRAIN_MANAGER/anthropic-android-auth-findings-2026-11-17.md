# Anthropic and Android authentication findings

Retrieved 2026-11-17 UTC using the available web research tool. The adjacent JSON evidence files contain exact tool requests, returned source text, canonical URLs, and retrieval time. No account authentication, credential access, API inference, source editing, build, or install occurred.

## Supported integration and prohibition

Source: https://code.claude.com/docs/en/legal-and-compliance
The current Authentication and credential use section explicitly excludes third-party Claude.ai login within a developer's own application, routing through subscriber credentials, and collecting or intermediating Claude.ai credentials or session tokens. Developers should use API keys or supported cloud providers. The separate exception for running the unmodified Claude Code binary does not authorize a custom subscription OAuth implementation in Android.

## Current direct API contract

Sources:
- https://platform.claude.com/docs/en/api/overview
- https://platform.claude.com/docs/en/manage-claude/authentication
- https://platform.claude.com/docs/en/api/messages/create

POST https://api.anthropic.com/v1/messages

Headers:
- Authorization: Bearer <API_KEY>
- anthropic-version: 2026-10-21
- content-type: application/json
- anthropic-workspace-id: <WORKSPACE_ID> when the API key is not scoped to one workspace

A single-workspace key can omit the workspace header. The legacy x-api-key: <API_KEY> header is still supported instead of Authorization. Model, max_tokens, and messages are the core request fields; each message includes role and content. stream:true selects SSE.

The overview calls anthropic-version required while the generated Messages reference labels that parameter optional. Explicitly sending the documented version is the conservative implementation; do not silently rely on a default.

Current auth choices are static API keys, Workload Identity Federation (WIF), and Apple App Attest. WIF exchanges organization-configured workload identity JWTs at POST /v1/oauth/token; it does not grant consumer Claude subscription authorization. App Attest is documented for iOS and macOS only, bills the configured workspace, and issues hour-lived tokens for Messages calls. The inspected current overview/authentication material does not document an Android counterpart.

## Owner configuration and billing

Sources:
- https://platform.claude.com/docs/en/get-api-key
- https://support.claude.com/en/articles/9876003-why-is-claude-api-usage-billed-separately-from-my-paid-claude-plan
- https://platform.claude.com/docs/en/about-claude/api-credits-for-subscribers

Owner creates a Console account and personal key for own tooling, or a service-account key for a shared service, at https://platform.claude.com/settings/keys. Set expiration and a workspace scope. Key creation may require organization role changes.

Console/API and subscriber chat are separate products. Current eligible subscriber plans can supply API credits after linking the plan to a Console organization: Max $100 per month, Ultra $200, Team pooled up to $500. Free, Pro, and Enterprise are ineligible for this particular credit program. Credits require an active eligible plan; new subscribers wait seven days. App-store subscriptions can qualify, but claiming happens on claude.ai in a web browser. API credits do not create third-party Claude.ai OAuth permission.

## Android storage recommendation and limitation

Sources:
- https://developer.android.com/privacy-and-security/security-tips
- https://developer.android.com/privacy-and-security/keystore
- https://platform.claude.com/docs/en/manage-claude/authentication

The Android security guidance recommends Keystore-backed storage and encryption for retained API secrets, excluding API keys from source control, and separate environment keys. Android Keystore protects cryptographic key material from extraction and can constrain its uses; it does not turn a bundled shared vendor API key into a safe public-client credential. A BYOK secret must still appear in application memory to send it over TLS.

For a public JARVIS distribution, prefer an authenticated owner-controlled HTTPS backend/companion holding the workspace-scoped Anthropic API key in a secrets manager. Android authenticates to that service independently. For optional personal BYOK, accept only the user's explicitly supplied Anthropic Console API key, retain it using a Keystore-generated AES-GCM key and private encrypted payload, exclude backups and logs, support clear/disconnect, and report configured versus API-verified states truthfully. No Firebase dependency is required for either architecture.
