# Official ChatGPT sign-in asset provenance

Retrieved 2026-11-17 using the GitHub connector. Direct requests to openai.com and cdn.openai.com failed at the workspace proxy with `Tunnel connection failed: 403 Forbidden`; the official OpenAI GitHub repository was accessible through the connector.

Official source repository: https://github.com/openai/sign-in-with-chatgpt-devkit

Pinned upstream commit: `0a36fefeb913055c8c7a1a29b63d82b96b2e841a` (2026-10-07).

Assets saved unchanged in this research directory:

| File | Bytes | Git blob SHA-1 | SHA-256 |
| --- | ---: | --- | --- |
| chatgpt-button-white-21.svg | 4214 | b40a3611a51bdf954e66b6ded784ea32a4ea8e6c | 568ce2e8701934856aaafc0ed1c3d0300f6c2ff384d0ae743dd1dea08570e12c |
| chatgpt-button-black-21.svg | 4214 | b732129ec9c1ce2430982b5fc3533c9a6f7d596e | b8b9aba7ba591f54bf1805f422bc23f32c44ee993643cc378cc77f766061f7a1 |

Pinned white asset: https://github.com/openai/sign-in-with-chatgpt-devkit/blob/0a36fefeb913055c8c7a1a29b63d82b96b2e841a/assets/brand/chatgpt-button-white-21.svg

Pinned black asset: https://github.com/openai/sign-in-with-chatgpt-devkit/blob/0a36fefeb913055c8c7a1a29b63d82b96b2e841a/assets/brand/chatgpt-button-black-21.svg

The reconstructed Git blob hashes of both downloaded files match the official GitHub contents records. Each file has viewBox `0 0 21 21`, one unchanged path, a white or black fill, and no scripts, external references or event handlers. A format-only Android vector conversion can preserve the original path, 21-by-21 viewport, proportions and exact white/black fill. No invented or approximated path is needed. This report does not itself perform that conversion.

The repository's `assets/README.md` specifically identifies `brand/` SVGs as the ChatGPT variants for buttons, cards, callouts, dialogs and usage UI, subject to the OpenAI brand guidelines. The README was preserved as `openai-siwc-design-assets-README.md`. The repository license was preserved as `openai-siwc-devkit-LICENSE.txt`. Its code license is noncommercial and explicitly excludes OpenAI branded visual assets and trademarks; do not represent the logo as GPL/MIT or as licensed by that code license. Brand permission and requirements are instead governed by https://openai.com/brand/.

Usage requirements from current official brand and SIWC documentation, summarized:

- Use only to identify the actual OpenAI/ChatGPT integration; retain the mark unchanged, acknowledge OpenAI ownership, and follow its Marks usage terms.
- Preserve the supplied monochrome format, proportions and clear space. Do not recolor cyan, distort, decorate, combine into the J.A.R.V.I.S. logo, or make it the app's primary branding.
- Keep J.A.R.V.I.S. branding more prominent and avoid suggesting OpenAI endorsement or a brand partnership.
- SIWC approves white/black button formats with a clear sign-in label such as `Continue with ChatGPT`.
- Current SIWC UI guidance additionally asks for a first-sign-in plan-use acknowledgement and a `Manage usage` link to https://chatgpt.com/settings/usage. Display actual authorized plan/probe state; supplied gallery states are simulated examples, not real account eligibility.

Official guidance URLs:

- https://openai.com/brand/
- https://developers.openai.com/siwc/website.md
- https://developers.openai.com/siwc/ui-ux-guidelines.md
- https://github.com/openai/sign-in-with-chatgpt-devkit/blob/0a36fefeb913055c8c7a1a29b63d82b96b2e841a/assets/README.md

No main application files changed during this research. No Gradle task or dependency installation was run.
