package com.jarvis.assistant.brain.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ValidatedOpenAiIdentity(
    val subject: String,
    val email: String? = null,
    val name: String? = null,
)

/** Contains only a fixed explanation: provider responses and credentials never become causes. */
class OpenAiIdTokenValidationException :
    Exception("OpenAI sign-in identity could not be verified.")

/**
 * Verifies SIWC identity tokens against the configured issuer's keys.
 * An ID token proves identity; it does not authorize inference or establish plan eligibility.
 */
class OpenAiIdTokenValidator(
    client: OkHttpClient = OkHttpClient(),
    private val jwksUrl: String = "https://auth.openai.com/.well-known/jwks.json",
    private val expectedIssuer: String = "https://auth.openai.com",
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val http = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
    private val keysMutex = Mutex()
    private var cachedKeys: Map<String, PublicKey>? = null
    private var keysFetchedAt = 0L

    suspend fun validate(
        token: String,
        clientId: String,
        nonce: String,
    ): ValidatedOpenAiIdentity = withContext(Dispatchers.IO) {
        try {
            requireValid(token.length in 1..MAX_TOKEN_CHARS)
            requireValid(validText(clientId, MAX_CLAIM_CHARS) && validText(nonce, MAX_CLAIM_CHARS))
            val parts = token.split('.')
            requireValid(parts.size == 3 && parts.all { it.isNotEmpty() })
            val header = json(decode(parts[0], MAX_HEADER_BYTES))
            requireValid(header.opt("alg") == "RS256")
            // No critical extension or detached/unencoded-payload interpretation is supported.
            requireValid(!header.has("crit") && (!header.has("b64") || header.opt("b64") == true))
            val kid = requiredText(header, "kid", MAX_KID_CHARS)
            val claims = json(decode(parts[1], MAX_CLAIMS_BYTES))
            val signatureBytes = decode(parts[2], MAX_SIGNATURE_BYTES)
            // jku, x5u, and embedded jwk are intentionally never used for key discovery.
            val key = keyFor(kid)
            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(key)
            verifier.update((parts[0] + "." + parts[1]).toByteArray(Charsets.US_ASCII))
            requireValid(verifier.verify(signatureBytes))
            validateClaims(claims, clientId, nonce)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: OpenAiIdTokenValidationException) {
            throw failure
        } catch (_: Exception) {
            throw OpenAiIdTokenValidationException()
        }
    }

    private fun validateClaims(
        claims: JSONObject,
        clientId: String,
        expectedNonce: String,
    ): ValidatedOpenAiIdentity {
        requireValid(claims.opt("iss") == expectedIssuer)
        val audience = claims.opt("aud")
        requireValid(
            when (audience) {
                is String -> audience == clientId
                is JSONArray -> audience.length() in 1..MAX_AUDIENCES &&
                    (0 until audience.length()).all { validText(audience.opt(it), MAX_CLAIM_CHARS) } &&
                    (0 until audience.length()).any { audience.opt(it) == clientId }
                else -> false
            },
        )
        if (claims.has("azp")) requireValid(claims.opt("azp") == clientId)
        val subject = requiredText(claims, "sub", MAX_CLAIM_CHARS)
        val expiresAt = numericDate(claims, "exp")
        val issuedAt = numericDate(claims, "iat")
        val now = Math.floorDiv(nowMillis(), 1_000L)
        requireValid(now >= 0L && expiresAt > 0L && issuedAt >= 0L && expiresAt >= issuedAt)
        requireValid(expiresAt > now - CLOCK_SKEW_SECONDS)
        requireValid(issuedAt <= now + CLOCK_SKEW_SECONDS)
        if (claims.has("nbf")) {
            requireValid(numericDate(claims, "nbf") <= now + CLOCK_SKEW_SECONDS)
        }
        val actualNonce = requiredText(claims, "nonce", MAX_CLAIM_CHARS)
        requireValid(
            MessageDigest.isEqual(
                expectedNonce.toByteArray(Charsets.UTF_8),
                actualNonce.toByteArray(Charsets.UTF_8),
            ),
        )
        if (claims.has("sid")) requiredText(claims, "sid", MAX_CLAIM_CHARS)
        return ValidatedOpenAiIdentity(
            subject = subject,
            email = optionalText(claims, "email"),
            name = optionalText(claims, "name"),
        )
    }

    private suspend fun keyFor(kid: String): PublicKey = keysMutex.withLock {
        val now = nowMillis()
        var keys = cachedKeys
        if (keys == null || now < keysFetchedAt || now - keysFetchedAt >= KEY_CACHE_MILLIS) {
            keys = fetchKeys()
            cachedKeys = keys
            keysFetchedAt = now
        }
        keys[kid]?.let { return@withLock it }
        // A new signing key may have appeared since caching. One refresh, never a retry loop.
        keys = fetchKeys()
        cachedKeys = keys
        keysFetchedAt = nowMillis()
        keys[kid] ?: throw OpenAiIdTokenValidationException()
    }

    private suspend fun fetchKeys(): Map<String, PublicKey> {
        val body = fetchJwks()
        val keys = json(body).optJSONArray("keys") ?: throw OpenAiIdTokenValidationException()
        requireValid(keys.length() <= MAX_JWKS_KEYS)
        val result = LinkedHashMap<String, PublicKey>()
        for (index in 0 until keys.length()) {
            val jwk = keys.optJSONObject(index) ?: throw OpenAiIdTokenValidationException()
            if (jwk.opt("kty") != "RSA") continue
            if (jwk.has("use") && jwk.opt("use") != "sig") continue
            if (jwk.has("alg") && jwk.opt("alg") != "RS256") continue
            if (jwk.has("key_ops")) {
                val operations = jwk.optJSONArray("key_ops") ?: continue
                if (operations.length() !in 1..8 ||
                    (0 until operations.length()).any { operations.opt(it) !is String } ||
                    (0 until operations.length()).none { operations.opt(it) == "verify" }
                ) continue
            }
            val kid = requiredText(jwk, "kid", MAX_KID_CHARS)
            requireValid(!result.containsKey(kid))
            val modulus = BigInteger(1, decode(requiredText(jwk, "n", MAX_CLAIM_CHARS), 1_024))
            val exponent = BigInteger(1, decode(requiredText(jwk, "e", MAX_KID_CHARS), 4))
            requireValid(modulus.bitLength() in 2_048..8_192 && modulus.testBit(0))
            requireValid(exponent > BigInteger.ONE && exponent.bitLength() <= 32 && exponent.testBit(0))
            result[kid] = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))
        }
        return result
    }

    private suspend fun fetchJwks(): ByteArray = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(jwksUrl).header("Accept", "application/json").get().build()
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(OpenAiIdTokenValidationException())
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes = response.use {
                        requireValid(response.isSuccessful)
                        val body = response.body ?: throw OpenAiIdTokenValidationException()
                        requireValid(body.contentLength() <= MAX_JWKS_BYTES)
                        val output = ByteArrayOutputStream()
                        body.byteStream().use { input ->
                            val buffer = ByteArray(4_096)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                requireValid(read <= MAX_JWKS_BYTES - output.size())
                                output.write(buffer, 0, read)
                            }
                        }
                        output.toByteArray()
                    }
                    if (continuation.isActive) continuation.resume(bytes)
                } catch (_: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(OpenAiIdTokenValidationException())
                    }
                }
            }
        })
    }

    private fun numericDate(json: JSONObject, name: String): Long = when (val value = json.opt(name)) {
        is Int -> value.toLong()
        is Long -> value
        else -> throw OpenAiIdTokenValidationException()
    }

    private fun optionalText(json: JSONObject, name: String): String? {
        if (!json.has(name) || json.isNull(name)) return null
        return requiredText(json, name, MAX_CLAIM_CHARS)
    }

    private fun requiredText(json: JSONObject, name: String, maximum: Int): String {
        val value = json.opt(name)
        requireValid(validText(value, maximum))
        return value as String
    }

    private fun validText(value: Any?, maximum: Int): Boolean =
        value is String && value.isNotBlank() && value.length <= maximum &&
            value.none { Character.isISOControl(it) }

    private fun decode(segment: String, maximumBytes: Int): ByteArray {
        requireValid(segment.isNotEmpty() && segment.length <= (maximumBytes * 4 / 3) + 4)
        requireValid(segment.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' })
        val decoded = Base64.getUrlDecoder().decode(segment)
        requireValid(decoded.size <= maximumBytes)
        return decoded
    }

    private fun json(bytes: ByteArray): JSONObject {
        val utf8 = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
        return JSONObject(utf8.toString())
    }

    private fun requireValid(condition: Boolean) {
        if (!condition) throw OpenAiIdTokenValidationException()
    }

    private companion object {
        const val CLOCK_SKEW_SECONDS = 5L
        const val KEY_CACHE_MILLIS = 10L * 60 * 1_000
        const val MAX_TOKEN_CHARS = 65_536
        const val MAX_HEADER_BYTES = 4_096
        const val MAX_CLAIMS_BYTES = 32_768
        const val MAX_SIGNATURE_BYTES = 1_024
        const val MAX_CLAIM_CHARS = 4_096
        const val MAX_KID_CHARS = 256
        const val MAX_AUDIENCES = 16
        const val MAX_JWKS_BYTES = 262_144
        const val MAX_JWKS_KEYS = 64
    }
}
