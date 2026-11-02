# Source and license notices

Jarvis application code is distributed under GPL-3.0-or-later; see LICENSE. Third-party components retain their own copyright and license terms. This is an Android development preview, not an official Marvel or commercial assistant product.

The source bundle accompanies the APK. Keep it available with any redistribution. It includes the original sherpa-onnx source and exact eSpeak-NG/piper-phonemize dependency source archives under third_party/sources, their upstream build instructions, the modified OCR source, and the complete vendored llama.cpp source. See docs/VOICE_MODELS.md for matching source revisions, native build recipes, checksums, and GPL phonemizer obligations. We use the unchanged upstream sherpa AAR; its original native SDK was not independently rebuilt here.

| Component | License / retained material |
| --- | --- |
| Kotlin, AndroidX, Compose, Room, Vosk | Apache-2.0; upstream Maven notices and source links |
| OkHttp / Okio | Apache-2.0 |
| kotlinx.coroutines | Apache-2.0 |
| llama.cpp / ggml | MIT; full source and LICENSE under app/src/main/cpp/vendor/llama.cpp |
| Qwen2.5 0.5B model | Apache-2.0; optional official model download with revision and digest recorded at installation |
| sherpa-onnx | Apache-2.0; original SDK and matching source archive included |
| eSpeak-NG for Piper | GPL-3.0; exact corresponding source, COPYING, build metadata included in archive |
| piper-phonemize | MIT; exact source and license included in archive |
| Piper voice datasets | English LJ Speech public domain; Spanish DaveFX and Persian Amir CC0; model cards retained in downloaded models |
| Tesseract4Android / Tesseract | Apache-2.0; corresponding source and build patch under vendor/ocr |
| Leptonica | BSD-style; source notices under vendor/ocr |
| tessdata_fast English, Spanish, Persian | Apache-2.0; pinned model assets and provenance under app/src/main/assets/ocr and docs/OCR.md |
| PDFBox Android | Apache-2.0; PDFBox/FontBox and bundled dependencies retain upstream notices |
| Bouncy Castle | MIT-style Bouncy Castle license |
| Apache Commons Compress / IO | Apache-2.0 |
| JNA | LGPL-2.1-or-later OR Apache-2.0; used under Apache-2.0 |
| ONNX Runtime | MIT; upstream SDK notices retained |

Gradle resolves additional transitive components with their own notices. Original AAR/JAR metadata and upstream licenses remain authoritative. Build scripts do not strip native license obligations. No API keys, private signing keys, model weights requiring user credentials, or personal phone data are included in this source distribution.

Neural Core optional model profiles retain official Qwen2.5-0.5B Instruct GGUF and add the independently verified official Qwen2.5-1.5B Instruct GGUF candidate. Downloads require Apache-2.0 metadata from their respective official repositories. No weights are bundled; model variants must not be assumed to share terms. The small optional Neural Core reference packs are original CC0 1.0 prose; their license accompanies `downloads/neural-packs`. WorkManager is an AndroidX Apache-2.0 library. The frozen 3.0 prompt adapter is this project's own prior source, retained solely for labelled evaluation.
