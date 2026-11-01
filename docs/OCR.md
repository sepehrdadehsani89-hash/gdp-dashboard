# Private file and photo reading

Jarvis reads files and photos that the user explicitly selects through Android's Storage Access Framework. It does not scan the phone's storage or entire photo library.

Up to three attachments are processed per question. Text documents are limited to 1 MiB and decoded as UTF-8 or BOM-marked UTF-16. PDFs are limited to 20 MiB and the first eight pages; a longer PDF is labeled as partial. Photo text is read locally with Tesseract. The selected image is bounded to 20 MiB, downsampled to at most 1600 pixels per edge, and corrected for EXIF orientation. Extracted output is bounded to 24,000 UTF-16 characters.

Photo reading extracts printed text. It does not identify people, objects, or scenes, and OCR can make mistakes. Check important details against the source image. Images without legible text report that limitation. Locked PDFs are refused; scanned PDFs need a separately selected screenshot for OCR.

## Dependencies and provenance

| Component | Version/source | License |
| --- | --- | --- |
| Tesseract4Android | Official tag 4.8.0, commit `b36053d52d0e5915553074e66d5c51a8a7783c55` | Apache-2.0 |
| PDFBox Android | `com.tom-roush:pdfbox-android:2.0.27.0`, Maven Central | Apache-2.0 |
| AndroidX ExifInterface | `androidx.exifinterface:exifinterface:1.3.7`, Google Maven | Apache-2.0 |
| OCR language data | Official `tesseract-ocr/tessdata_fast`, commit `87416418657359cb625c412a48b6e1d6d41c29bd` | Apache-2.0 |

Official upstream: <https://github.com/adaptech-cz/Tesseract4Android>. Complete OCR source, original notices, and copied dependency licenses remain under `vendor/ocr/`. Additional native libraries are Leptonica, libpng, and libjpeg; their notices are in `vendor/ocr/licenses/`.

The upstream-supported binary distribution uses JitPack, which was unavailable under the environment's configured network policy. Jarvis therefore builds the official source directly; no unofficial binary mirror or disabled verification was used.

The recorded compatibility patch is `vendor/ocr/build-compatibility.patch` (SHA-256 `77fb99183e65b04fc1eee9e24904323d75abcff930001c721cb5edf80f492424`). It selects the prepared AGP 8.7.3, SDK 35, NDK 26.3.11579264, ARM64/x86_64 ABIs, static C++ runtime, bounded compile pools, and explicit 16 KiB ELF linker alignment. Source commit/hash provenance is retained even when nested Git metadata is excluded from the distributable source tree.

Built artifact: `app/libs/tesseract4android-4.8.0.aar`

SHA-256: `8b198743d376db6447bdeefffd34af67e7179bc4b71595b731637ec25b0893f3`

The four shared libraries for each ABI were inspected with `llvm-readelf -lW`; every LOAD segment alignment is `0x4000`. This verifies ELF alignment, not full physical-device compatibility or OCR accuracy.

## Bundled models

All three verified files are included under `app/src/main/assets/ocr/` (about 6.6 MiB total), so photo text reading does not require a first-use download or an API key. Checked extraction installs them into private app storage. The runtime fallback, if a distributor intentionally omits assets, uses only pinned official HTTPS URLs and the same expected hashes.

| File | SHA-256 |
| --- | --- |
| `eng.traineddata` | `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2` |
| `spa.traineddata` | `6f2e04d02774a18f01bed44b1111f2cd7f3ba7ac9dc4373cd3f898a40ea6b464` |
| `fas.traineddata` | `db1c0a91208aff00d3cf1ed2c1d23f76419afd5f024688b4f71adc3f2ce4a505` |

English is included alongside Spanish or Persian for mixed-script text. No API request is made by the OCR pipeline. The parent conversation may send extracted text only through a user-selected cloud provider; selecting the on-device model keeps inference on the phone.

## Build and validation

Rebuild the retained source using a compatible prepared Android toolchain:

```sh
. /workspace/android-toolchain/env.sh
bash vendor/ocr/build.sh
```

The native AAR compiled successfully for both ARM64 and x86_64. `AttachmentLimitsTest` covers byte limits, multilingual decoding, invalid/binary input rejection, Unicode-safe truncation, bounded image sampling, hash calculation, and pinned model URLs. JVM checks validate these boundaries; they do not establish real OCR accuracy. Emulator/device tests must exercise actual image/PDF selection and native inference separately.
