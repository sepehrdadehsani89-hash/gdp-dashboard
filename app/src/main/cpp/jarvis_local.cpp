#include <jni.h>
#include <chrono>
#include "llama.h"
#include <algorithm>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>

namespace {
struct Engine {
    explicit Engine(std::string file) : path(std::move(file)) {}
    ~Engine() {
        if (context) llama_free(context);
        if (model) llama_model_free(model);
    }
    std::string path;
    int contextTokens=1536, threads=2;
    int64_t stats[5]={0,0,0,0,0};
    llama_model *model = nullptr;
    llama_context *context = nullptr;
    std::atomic<bool> cancelled{false};
    std::mutex generationMutex;
};

std::mutex registryMutex;
std::unordered_map<jlong, std::shared_ptr<Engine>> engines;
std::atomic<jlong> nextId{1};
std::once_flag backendOnce;

std::shared_ptr<Engine> lookup(jlong id) {
    std::lock_guard<std::mutex> lock(registryMutex);
    auto it = engines.find(id);
    if (it == engines.end()) throw std::runtime_error("Local model session is closed.");
    return it->second;
}

void throwJava(JNIEnv *env, const char *type, const std::string &message) {
    if (!env->ExceptionCheck()) {
        jclass cls = env->FindClass(type);
        if (cls) env->ThrowNew(cls, message.c_str());
    }
}

void checkCancelled(const Engine &engine) {
    if (engine.cancelled.load()) throw std::runtime_error("__CANCELLED__");
}

void load(Engine &engine) {
    if (engine.model && engine.context) return;
    checkCancelled(engine);
    if (engine.model) {
        llama_model_free(engine.model);
        engine.model = nullptr;
    }
    std::call_once(backendOnce, [] {
        // llama's default diagnostics may include prompts/model metadata. Keep them out of logcat.
        llama_log_set([](ggml_log_level, const char *, void *) {}, nullptr);
        llama_backend_init();
    });
    auto modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;
    modelParams.use_mmap = true;
    modelParams.use_mlock = false;
    modelParams.progress_callback = [](float, void *data) {
        return !static_cast<Engine *>(data)->cancelled.load();
    };
    modelParams.progress_callback_user_data = &engine;
    engine.model = llama_model_load_from_file(engine.path.c_str(), modelParams);
    if (engine.cancelled.load()) {
        if (engine.model) llama_model_free(engine.model);
        engine.model = nullptr;
        checkCancelled(engine);
    }
    if (!engine.model) throw std::runtime_error("Could not load the verified model. Free memory and try again.");
    auto contextParams = llama_context_default_params();
    contextParams.n_ctx = engine.contextTokens;
    contextParams.n_batch = 128;
    contextParams.n_ubatch = 128;
    contextParams.n_threads = engine.threads;
    contextParams.n_threads_batch = engine.threads;
    contextParams.offload_kqv = false;
    contextParams.no_perf = true;
    contextParams.abort_callback = [](void *data) {
        return static_cast<Engine *>(data)->cancelled.load();
    };
    contextParams.abort_callback_data = &engine;
    engine.context = llama_init_from_model(engine.model, contextParams);
    if (!engine.context) {
        llama_model_free(engine.model);
        engine.model = nullptr;
        throw std::runtime_error("Not enough memory for the local AI. Close other apps and try again.");
    }
}

jbyteArray bytes(JNIEnv *env, const std::string &value) {
    auto result = env->NewByteArray(static_cast<jsize>(value.size()));
    if (result && !value.empty()) env->SetByteArrayRegion(result, 0, static_cast<jsize>(value.size()), reinterpret_cast<const jbyte *>(value.data()));
    return result;
}

// Tokens can split UTF-8 code points. Only emit complete code points to the Kotlin callback.
size_t completeUtf8Prefix(const std::string &value) {
    if (value.empty()) return 0;
    size_t start = value.size() - 1;
    while (start > 0 && (static_cast<unsigned char>(value[start]) & 0xc0) == 0x80) --start;
    unsigned char lead = static_cast<unsigned char>(value[start]);
    size_t required = (lead & 0x80) == 0 ? 1 : (lead & 0xe0) == 0xc0 ? 2 : (lead & 0xf0) == 0xe0 ? 3 : (lead & 0xf8) == 0xf0 ? 4 : 1;
    return value.size() - start < required ? start : value.size();
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_jarvis_assistant_local_LocalBrain_nativeCreate(JNIEnv *env, jobject, jstring path, jint contextTokens, jint threads) {
    try {
        const char *raw = env->GetStringUTFChars(path, nullptr);
        if (!raw) return 0;
        std::string file(raw);
        env->ReleaseStringUTFChars(path, raw);
        auto engine = std::make_shared<Engine>(file);
        engine->contextTokens=std::clamp(static_cast<int>(contextTokens),1024,2048);engine->threads=std::clamp(static_cast<int>(threads),1,2);
        auto id = nextId.fetch_add(1);
        std::lock_guard<std::mutex> lock(registryMutex);
        engines.emplace(id, std::move(engine));
        return id;
    } catch (const std::exception &error) {
        throwJava(env, "java/lang/IllegalStateException", error.what());
        return 0;
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_jarvis_assistant_local_LocalBrain_nativeGenerate(JNIEnv *env, jobject, jlong id, jbyteArray input, jint requestedTokens, jobject receiver) {
    try {
        auto engine = lookup(id);
        std::lock_guard<std::mutex> generationLock(engine->generationMutex);
        checkCancelled(*engine);
        const auto inputSize = env->GetArrayLength(input);
        if (inputSize < 1 || inputSize > 32768) throw std::runtime_error("The question exceeds the local input limit.");
        std::string prompt(static_cast<size_t>(inputSize), '\0');
        env->GetByteArrayRegion(input, 0, inputSize, reinterpret_cast<jbyte *>(prompt.data()));
        if (env->ExceptionCheck()) return nullptr;
        auto clock=[](){return std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();};
        auto loadStarted=clock();load(*engine);engine->stats[2]=clock()-loadStarted;engine->stats[1]=0;
        checkCancelled(*engine);
        const llama_vocab *vocab = llama_model_get_vocab(engine->model);
        int count = -llama_tokenize(vocab, prompt.data(), static_cast<int32_t>(prompt.size()), nullptr, 0, true, true);
        int maximumNew = std::clamp(static_cast<int>(requestedTokens), 1, 512);
        // Drop only complete oldest history pairs; never truncate the current user or system rules.
        const std::string boundary="<|im_end|>\n";
        while(count>engine->contextTokens-maximumNew-32){
            auto systemEnd=prompt.find(boundary);
            if(systemEnd==std::string::npos)break;
            auto first=systemEnd+boundary.size();
            auto userEnd=prompt.find(boundary,first);
            if(userEnd==std::string::npos)break;
            auto assistantStart=userEnd+boundary.size();
            if(prompt.compare(assistantStart,std::string("<|im_start|>assistant\n").size(),"<|im_start|>assistant\n")!=0)break;
            auto assistantEnd=prompt.find(boundary,assistantStart);
            if(assistantEnd==std::string::npos)break;
            prompt.erase(first,assistantEnd+boundary.size()-first);
            count=-llama_tokenize(vocab,prompt.data(),static_cast<int32_t>(prompt.size()),nullptr,0,true,true);
        }
        if (count <= 0 || count > engine->contextTokens-maximumNew-32 || count + maximumNew >= engine->contextTokens) {
            throw std::runtime_error("This question is too long for the small offline model. Shorten it or start a new conversation.");
        }
        std::vector<llama_token> tokens(static_cast<size_t>(count));
        count = llama_tokenize(vocab, prompt.data(), static_cast<int32_t>(prompt.size()), tokens.data(), count, true, true);
        if (count <= 0) throw std::runtime_error("Could not tokenize the question.");
        engine->stats[0]=count;auto promptStarted=clock();
        llama_kv_self_clear(engine->context);
        for (int offset = 0; offset < count; offset += 128) {
            checkCancelled(*engine);
            auto batch = llama_batch_get_one(tokens.data() + offset, std::min(128, count - offset));
            int status = llama_decode(engine->context, batch);
            checkCancelled(*engine);
            if (status != 0) throw std::runtime_error("Local AI prompt evaluation failed. Free memory and retry.");
        }
        engine->stats[3]=clock()-promptStarted;auto generationStarted=clock();
        auto params = llama_sampler_chain_default_params();
        params.no_perf = true;
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(llama_sampler_chain_init(params), llama_sampler_free);
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(0.6f));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        jclass callbackClass = env->GetObjectClass(receiver);
        jmethodID callback = env->GetMethodID(callbackClass, "onToken", "([B)V");
        if (!callback || env->ExceptionCheck()) return nullptr;
        std::string output;
        std::string pending;
        for (int index = 0; index < maximumNew; ++index) {
            checkCancelled(*engine);
            llama_token token = llama_sampler_sample(sampler.get(), engine->context, -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            std::vector<char> piece(256);
            int size = llama_token_to_piece(vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, false);
            if (size < 0) {
                piece.resize(static_cast<size_t>(-size));
                size = llama_token_to_piece(vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, false);
            }
            if (size < 0) throw std::runtime_error("Could not decode local AI output.");
            engine->stats[1]++;
            output.append(piece.data(), static_cast<size_t>(size));
            pending.append(piece.data(), static_cast<size_t>(size));
            size_t complete = completeUtf8Prefix(pending);
            if (complete > 0) {
                auto chunk = bytes(env, pending.substr(0, complete));
                if (!chunk) return nullptr;
                env->CallVoidMethod(receiver, callback, chunk);
                env->DeleteLocalRef(chunk);
                if (env->ExceptionCheck()) { engine->cancelled.store(true); return nullptr; }
                pending.erase(0, complete);
            }
            auto batch = llama_batch_get_one(&token, 1);
            int status = llama_decode(engine->context, batch);
            checkCancelled(*engine);
            if (status != 0) throw std::runtime_error("Local AI generation failed. Please retry.");
        }
        engine->stats[4]=clock()-generationStarted;
        env->DeleteLocalRef(callbackClass);
        if (!pending.empty()) output.resize(output.size() - pending.size());
        return bytes(env, output);
    } catch (const std::exception &error) {
        if (std::string(error.what()) == "__CANCELLED__") {
            throwJava(env, "java/util/concurrent/CancellationException", "Local AI cancelled");
        } else {
            throwJava(env, "java/lang/IllegalStateException", error.what());
        }
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_assistant_local_LocalBrain_nativePrepare(JNIEnv *env, jobject, jlong id) {
    try {
        lookup(id)->cancelled.store(false);
    } catch (const std::exception &error) {
        throwJava(env, "java/lang/IllegalStateException", error.what());
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_assistant_local_LocalBrain_nativeCancel(JNIEnv *, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(registryMutex);
    auto found = engines.find(id);
    if (found != engines.end()) found->second->cancelled.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_assistant_local_LocalBrain_nativeDestroy(JNIEnv *, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(registryMutex);
    auto found = engines.find(id);
    if (found != engines.end()) {
        found->second->cancelled.store(true);
        engines.erase(found);
    }
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_jarvis_assistant_local_LocalBrain_nativeStatistics(JNIEnv *env,jobject,jlong id){
    try{auto engine=lookup(id);std::lock_guard<std::mutex> lock(engine->generationMutex);jlong values[10]={0};for(int i=0;i<5;i++)values[i]=engine->stats[i];if(engine->model){values[5]=llama_model_n_ctx_train(engine->model);values[6]=llama_vocab_n_tokens(llama_model_get_vocab(engine->model));values[7]=llama_model_n_params(engine->model);values[8]=llama_model_size(engine->model);values[9]=llama_vocab_type(llama_model_get_vocab(engine->model));}auto result=env->NewLongArray(10);if(result)env->SetLongArrayRegion(result,0,10,values);return result;}
    catch(const std::exception &e){throwJava(env,"java/lang/IllegalStateException",e.what());return nullptr;}
}
