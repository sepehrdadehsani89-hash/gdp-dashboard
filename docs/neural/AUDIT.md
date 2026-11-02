# Neural Core baseline audit — 2026-11-02

Source baseline: Jarvis 3.0, `1df3528`, package `com.jarvis.next`. The original installation, signing key, model path and Room schema are preserved.

| Item | Observed source baseline |
|---|---|
| Family | Official Qwen2.5-0.5B-Instruct-GGUF |
| Format/quantization | GGUF, Q4_K_M; mixed K-block quantization, not uniformly four bits per weight |
| Runtime | Vendored llama.cpp b5046 / 74d4f5b041ad837153b0e90fc864b8290e01d8d5; MIT; CPU-only JNI |
| Prompt/tokenizer | Qwen ChatML; tokenizer is read from the downloaded GGUF; qwen2 vocabulary fixture exists in vendor source |
| Actual runtime context | 1536 tokens, batch/ubatch 128, two CPU threads, mmap, no GPU/KV offload |
| Output | Native cap 256 tokens even when Kotlin requests 512; UTF-8-safe streaming |
| Model storage | Private `files/local-brain/qwen2.5-0.5b-instruct-q4_k_m.gguf` plus verified-model.json |
| File size | Approximately 400 MB in existing UI; exact bytes are obtained from official LFS metadata, not available to this audit |
| License | Existing download requires official Apache-2.0 metadata; no weights redistributed in APK |
| Language | Manual EN/ES/FA selection and separate speech models. Model quality in each language is unmeasured; naming a language does not establish quality |
| History | General cloud turns retain a short buffer; local generate passes empty history, losing turn context |
| Retrieval | Bounded attachment/memory excerpt ranking; no persistent document index |
| Tools | Deterministic routing with registered single-use action approvals, target-bound live control and Android authentication; LLM text does not execute |
| Lifecycle | Mutex serializes inference; cancellation and release exist; downloads restart from zero |

Baseline measurement attempt: no actual weight GGUF exists in the workspace (vendor vocabulary fixtures are not model weights). HTTPS to the official Hugging Face model metadata endpoint returned proxy HTTP 403 under the enforced network policy. This prevents weight-based comparisons here. No download size, tokens/sec, first-token latency, hallucination score, RAM, thermal or battery benchmark is invented. The Redmi 9 is not connected. Use the app's explicit benchmark and the repeatable evaluation corpus on the configured phone. Desktop/emulator measurements, if made, must be separately labelled.

Model selection: retain the existing Lite model. Balanced is an explicit, optional Qwen2.5-1.5B Q4_K_M candidate using the same runtime/ChatML and independently checked Apache-2.0 repository metadata. It is experimental until evaluated on the phone; it is not claimed superior from unperformed tests. Advanced stays unavailable on unverified hardware. Download metadata must display actual bytes before confirmation. Larger models cannot silently replace Lite or share a loaded context.

The vendored `ggml-vocab-qwen2.gguf` is a GGUF v3 **zero-tensor vocabulary fixture**, not usable model weights. Direct header inspection finds architecture `qwen2`, tokenizer model `gpt2`, pre-tokenizer `qwen2`, 151,936 token entries and 151,387 merge entries. This establishes what the fixture contains, not the exact header of a downloaded weight file. Neural Core records training context, vocabulary count, parameter count and tensor bytes from the actual loaded model in its phone evaluation; it never substitutes fixture statistics for a weight benchmark.
