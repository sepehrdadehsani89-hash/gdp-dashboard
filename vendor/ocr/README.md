# On-device attachment reading

`AttachmentReader` reads only user-selected `content://` URIs from Android's picker. It does not enumerate storage or the photo library.

- Text: UTF-8 or BOM-marked UTF-16, maximum 1 MiB before decoding.
- PDF: PDFBox Android 2.0.27.0, maximum 20 MiB, first 8 pages; locked PDFs are refused.
- Photo: EXIF-aware downsampling to at most 1600 pixels per edge, then native Tesseract OCR. This extracts text; it does not provide general image/scene understanding.
- Returned content is bounded to 24,000 UTF-16 characters. Important OCR text should be checked against the original.
- Processing stays on the phone. Choosing a cloud conversation provider is a separate user decision controlled by the parent application.

## Native OCR provenance

Official source: https://github.com/adaptech-cz/Tesseract4Android

Tag: `4.8.0`

Commit: `b36053d52d0e5915553074e66d5c51a8a7783c55`

The library is built from this source, rather than using an unofficial artifact mirror. The upstream-supported JitPack artifact could not be obtained through the configured environment network policy. No verification or TLS check was disabled.

The local compatibility changes are recorded in `build-compatibility.patch`: AGP 8.7.3, SDK 35, installed NDK 26.3.11579264, ARM64 and x86_64 only, static C++ runtime, bounded native compilation pools, and explicit 16 KiB ELF linker alignment. The unused JitPack repository declaration was removed. Original license notices remain in the source; copies are in `licenses/`.

Built AAR: `app/libs/tesseract4android-4.8.0.aar`

SHA-256: `8b198743d376db6447bdeefffd34af67e7179bc4b71595b731637ec25b0893f3`

Both ABIs' four native libraries were inspected with the installed NDK's `llvm-readelf -lW`; every LOAD segment alignment is `0x4000`. This is a linker-alignment check, not a claim that the entire app has been tested on a physical 16 KiB-page device.

## Bundled OCR models

Official repository: https://github.com/tesseract-ocr/tessdata_fast

Pinned commit: `87416418657359cb625c412a48b6e1d6d41c29bd`

The following files are bundled in `app/src/main/assets/ocr/` and checked again during extraction into app-private storage:

| Language | File | SHA-256 |
| --- | --- | --- |
| English | eng.traineddata | 7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2 |
| Spanish | spa.traineddata | 6f2e04d02774a18f01bed44b1111f2cd7f3ba7ac9dc4373cd3f898a40ea6b464 |
| Persian | fas.traineddata | db1c0a91208aff00d3cf1ed2c1d23f76419afd5f024688b4f71adc3f2ce4a505 |

Total uncompressed size is approximately 6.6 MiB. These are Apache-2.0 model artifacts obtained from pinned official HTTPS sources. Exact hashes were calculated from those audited downloads. The runtime only has a pinned, checksum-verified HTTPS download fallback if a distributor intentionally omits the bundled model files.

## Rebuilding

Use a JDK 17+ and the pinned Gradle/Android SDK/NDK/CMake versions. In this prepared environment:

```sh
. /workspace/android-toolchain/env.sh
bash vendor/ocr/build.sh
```

The complete upstream sources are retained under `Tesseract4Android/`. Generated `.cxx/`, `.gradle/`, `build/`, and ELF audit extractions are build outputs and need not be included in source archives.

## Verification

The native AAR built successfully for ARM64 and x86_64. `AttachmentLimitsTest` covers byte limits, decoding, Unicode-safe truncation, image sampling, model URL pinning, and hash computation. Run it through the app's existing unit-test task. Real image/PDF ingestion and OCR accuracy require emulator or phone tests; they are not established by the JVM tests alone.
