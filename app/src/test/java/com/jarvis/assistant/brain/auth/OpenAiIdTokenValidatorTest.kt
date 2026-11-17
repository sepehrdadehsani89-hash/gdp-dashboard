package com.jarvis.assistant.brain.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.concurrent.TimeUnit

class OpenAiIdTokenValidatorTest {
    private lateinit var server: MockWebServer

    @Before fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @After fun closeServer() {
        server.shutdown()
    }

    @Test fun verifiesSignatureIdentityAndStringAudienceWithoutSendingCredentials() = runBlocking<Unit> {
        enqueueKeys()
        val result = validator().validate(sign(claims()), CLIENT_ID, NONCE)
        assertEquals(ValidatedOpenAiIdentity("subject-test", "owner@example.test", "Owner Test"), result)
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("/jwks", request.path)
        assertNull(request.getHeader("Authorization"))
        assertNull(request.getHeader("Cookie"))
        assertEquals(0L, request.bodySize)
    }

    @Test fun acceptsAnAudienceArrayAndOptionalAuthorizedParty() = runBlocking<Unit> {
        enqueueKeys()
        val payload = claims().put("aud", JSONArray().put("another-client").put(CLIENT_ID)).put("azp", CLIENT_ID)
        assertEquals("subject-test", validator().validate(sign(payload), CLIENT_ID, NONCE).subject)
    }

    @Test fun rejectsASignatureFromAnotherRsaKey() = runBlocking<Unit> {
        enqueueKeys()
        rejected(sign(claims(), key = SECOND_KEY))
    }

    @Test fun rejectsNoneAndHmacAlgorithmsBeforeRequestingKeys() = runBlocking<Unit> {
        for (algorithm in listOf("none", "HS256", "RS512")) {
            rejected(sign(claims(), header = header().put("alg", algorithm)))
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun rejectsMalformedCompactTokensWithoutNetwork() = runBlocking<Unit> {
        for (token in listOf("", "one.two", "one.two.three.four", ".e30.signature", "%%.e30.signature", "a".repeat(65_537))) {
            rejected(token)
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun rejectsWrongIssuer() = runBlocking<Unit> {
        enqueueKeys()
        rejected(sign(claims().put("iss", "https://auth.openai.com.attacker.test")))
    }

    @Test fun rejectsWrongAudienceAndMalformedAudienceArrays() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        for (audience in listOf<Any>("wrong-client", JSONArray(), JSONArray().put(CLIENT_ID).put(42), 42)) {
            rejected(sign(claims().put("aud", audience)), candidate)
        }
        assertEquals(1, server.requestCount)
    }

    @Test fun rejectsAMismatchedAuthorizedParty() = runBlocking<Unit> {
        enqueueKeys()
        rejected(sign(claims().put("azp", "another-client")))
    }

    @Test fun rejectsExpiredTokens() = runBlocking<Unit> {
        enqueueKeys()
        rejected(sign(claims().put("exp", NOW_SECONDS - 6)))
    }

    @Test fun rejectsFutureIssuedAtAndNotBeforeClaims() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        rejected(sign(claims().put("iat", NOW_SECONDS + 6)), candidate)
        rejected(sign(claims().put("nbf", NOW_SECONDS + 6)), candidate)
    }

    @Test fun appliesFiveSecondsOfClockSkew() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        assertEquals(
            "subject-test",
            candidate.validate(sign(claims().put("exp", NOW_SECONDS - 4)), CLIENT_ID, NONCE).subject,
        )
        assertEquals(
            "subject-test",
            candidate.validate(
                sign(claims().put("iat", NOW_SECONDS + 5).put("nbf", NOW_SECONDS + 5)),
                CLIENT_ID,
                NONCE,
            ).subject,
        )
        rejected(sign(claims().put("exp", NOW_SECONDS - 5)), candidate)
    }

    @Test fun rejectsEveryMissingRequiredIdentityClaim() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        for (name in listOf("iss", "aud", "sub", "exp", "iat", "nonce")) {
            val payload = claims()
            payload.remove(name)
            rejected(sign(payload), candidate)
        }
    }

    @Test fun rejectsStringAndFractionalNumericDates() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        rejected(sign(claims().put("exp", (NOW_SECONDS + 60).toString())), candidate)
        rejected(sign(claims().put("iat", NOW_SECONDS.toDouble() + 0.25)), candidate)
        rejected(sign(claims().put("nbf", "0")), candidate)
        rejected(sign(claims().put("exp", NOW_SECONDS - 61)), candidate)
    }

    @Test fun rejectsMissingMismatchedAndNonStringNonce() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        rejected(sign(claims().put("nonce", "different-nonce")), candidate)
        rejected(sign(claims().put("nonce", 42)), candidate)
        rejected(sign(claims()), candidate, expectedNonce = "")
    }

    @Test fun optionalSessionIdMustBeANonemptyString() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        assertEquals("subject-test", candidate.validate(sign(claims().put("sid", "session-test")), CLIENT_ID, NONCE).subject)
        for (sid in listOf<Any>("", "  ", 42, JSONObject.NULL, "session\ncontrol")) {
            rejected(sign(claims().put("sid", sid)), candidate)
        }
    }

    @Test fun optionalIdentityFieldsAreNullableButRejectMalformedValues() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        val payload = claims()
        payload.remove("email")
        payload.put("name", JSONObject.NULL)
        assertEquals(ValidatedOpenAiIdentity("subject-test"), candidate.validate(sign(payload), CLIENT_ID, NONCE))
        rejected(sign(claims().put("email", 42)), candidate)
        rejected(sign(claims().put("sub", "  ")), candidate)
    }

    @Test fun ignoresTokenSuppliedKeyLocations() = runBlocking<Unit> {
        enqueueKeys()
        val supplied = header()
            .put("jku", "https://attacker.invalid/jwks")
            .put("x5u", "https://attacker.invalid/cert")
            .put("jwk", jwk(SECOND_KEY, "main"))
        assertEquals("subject-test", validator().validate(sign(claims(), header = supplied), CLIENT_ID, NONCE).subject)
        assertEquals(1, server.requestCount)
        assertEquals("/jwks", server.takeRequest(2, TimeUnit.SECONDS)!!.path)
    }

    @Test fun rejectsCriticalExtensionsAndUnencodedPayloads() = runBlocking<Unit> {
        rejected(sign(claims(), header = header().put("crit", JSONArray().put("custom"))))
        rejected(sign(claims(), header = header().put("b64", false)))
        assertEquals(0, server.requestCount)
    }

    @Test fun reusesCachedKeys() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        repeat(3) { candidate.validate(sign(claims()), CLIENT_ID, NONCE) }
        assertEquals(1, server.requestCount)
    }

    @Test fun unknownKeyRefreshesOnceAndAcceptsARotatedKey() = runBlocking<Unit> {
        enqueueKeys()
        enqueueKeys(SECOND_KEY, "rotated")
        val rotatedHeader = header().put("kid", "rotated")
        assertEquals(
            "subject-test",
            validator().validate(sign(claims(), SECOND_KEY, rotatedHeader), CLIENT_ID, NONCE).subject,
        )
        assertEquals(2, server.requestCount)
    }

    @Test fun cachedUnknownKeyRefreshesOnlyOnce() = runBlocking<Unit> {
        enqueueKeys()
        val candidate = validator()
        candidate.validate(sign(claims()), CLIENT_ID, NONCE)
        enqueueKeys(SECOND_KEY, "rotated")
        candidate.validate(sign(claims(), SECOND_KEY, header().put("kid", "rotated")), CLIENT_ID, NONCE)
        assertEquals(2, server.requestCount)
    }

    @Test fun aPersistentlyUnknownKeyCannotCauseAnUnboundedFetchLoop() = runBlocking<Unit> {
        enqueueKeys()
        enqueueKeys()
        rejected(sign(claims(), header = header().put("kid", "unknown")))
        assertEquals(2, server.requestCount)
    }

    @Test fun rejectsDuplicateSigningKeyIds() = runBlocking<Unit> {
        val keys = JSONArray().put(jwk(FIRST_KEY, "main")).put(jwk(SECOND_KEY, "main"))
        server.enqueue(MockResponse().setBody(JSONObject().put("keys", keys).toString()))
        rejected(sign(claims()))
        assertEquals(1, server.requestCount)
    }

    @Test fun rejectsNonSigningKeysAndWeakRsaKeys() = runBlocking<Unit> {
        val invalid = jwk(FIRST_KEY, "main").put("use", "enc")
        repeat(2) {
            server.enqueue(MockResponse().setBody(JSONObject().put("keys", JSONArray().put(invalid)).toString()))
        }
        rejected(sign(claims()))
        val weak = jwk(FIRST_KEY, "main").put("n", base64(byteArrayOf(3)))
        server.enqueue(MockResponse().setBody(JSONObject().put("keys", JSONArray().put(weak)).toString()))
        rejected(sign(claims()))
    }

    @Test fun rejectsOversizedFixedLengthAndChunkedKeyResponses() = runBlocking<Unit> {
        server.enqueue(MockResponse().setBody(" ".repeat(262_145)))
        rejected(sign(claims()))
        server.enqueue(MockResponse().setChunkedBody(" ".repeat(262_145), 4_096))
        rejected(sign(claims()))
        assertEquals(2, server.requestCount)
    }

    @Test fun redirectsAreRejectedAndErrorBodiesNeverReachExceptions() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/other-keys")))
        val redirect = rejected(sign(claims()))
        assertEquals(1, server.requestCount)
        server.enqueue(MockResponse().setResponseCode(403).setBody("secret-provider-body"))
        val failure = rejected(sign(claims()))
        assertEquals(2,server.requestCount)
        assertFalse(failure.toString().contains("secret-provider-body"))
        assertFalse(failure.toString().contains(NONCE))
        assertEquals(redirect.message, failure.message)
    }

    @Test fun cancellationCancelsAStalledKeyRequest() = runBlocking<Unit> {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val candidate = validator()
        val pending = launch { candidate.validate(sign(claims()), CLIENT_ID, NONCE) }
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(2, TimeUnit.SECONDS) })
        withTimeout(3_000) { pending.cancelAndJoin() }
        assertTrue(pending.isCancelled)
    }

    private fun validator(): OpenAiIdTokenValidator = OpenAiIdTokenValidator(
        client = OkHttpClient(),
        jwksUrl = server.url("/jwks").toString(),
        nowMillis = { NOW_SECONDS * 1_000 },
    )

    private suspend fun rejected(
        token: String,
        candidate: OpenAiIdTokenValidator = validator(),
        expectedNonce: String = NONCE,
    ): OpenAiIdTokenValidationException {
        val failure = runCatching { candidate.validate(token, CLIENT_ID, expectedNonce) }.exceptionOrNull()
        assertTrue("Expected a sanitized ID-token validation failure", failure is OpenAiIdTokenValidationException)
        // Coroutine stack recovery may wrap a sanitized exception in another instance of
        // the same type. Every cause must remain sanitized; raw transport/token causes fail.
        var cause:Throwable?=failure
        val seen=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable,Boolean>())
        while(cause!=null && seen.add(cause)){
            assertTrue(cause is OpenAiIdTokenValidationException)
            assertEquals("OpenAI sign-in identity could not be verified.",cause.message)
            assertFalse(cause.toString().contains(token.takeIf { it.isNotEmpty() } ?: "never-present-marker"))
            cause=cause.cause
        }
        return failure as OpenAiIdTokenValidationException
    }

    private fun enqueueKeys(key: KeyPair = FIRST_KEY, kid: String = "main") {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json")
                .setBody(JSONObject().put("keys", JSONArray().put(jwk(key, kid))).toString()),
        )
    }

    private fun claims(): JSONObject = JSONObject()
        .put("iss", "https://auth.openai.com")
        .put("aud", CLIENT_ID)
        .put("sub", "subject-test")
        .put("iat", NOW_SECONDS - 60)
        .put("exp", NOW_SECONDS + 60)
        .put("nonce", NONCE)
        .put("email", "owner@example.test")
        .put("name", "Owner Test")

    private fun header(): JSONObject = JSONObject().put("alg", "RS256").put("kid", "main").put("typ", "JWT")

    private fun sign(
        payload: JSONObject,
        key: KeyPair = FIRST_KEY,
        header: JSONObject = header(),
    ): String {
        val signingInput = base64(header.toString().toByteArray()) + "." + base64(payload.toString().toByteArray())
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(key.private)
        signer.update(signingInput.toByteArray(Charsets.US_ASCII))
        return signingInput + "." + base64(signer.sign())
    }

    private fun jwk(key: KeyPair, kid: String): JSONObject {
        val publicKey = key.public as RSAPublicKey
        return JSONObject()
            .put("kty", "RSA")
            .put("alg", "RS256")
            .put("use", "sig")
            .put("kid", kid)
            .put("n", base64(unsigned(publicKey.modulus)))
            .put("e", base64(unsigned(publicKey.publicExponent)))
    }

    private fun unsigned(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
    }

    private fun base64(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private companion object {
        const val CLIENT_ID = "registered-public-test-client"
        const val NONCE = "test-nonce-for-one-sign-in"
        const val NOW_SECONDS = 1_800_000_000L
        val FIRST_KEY: KeyPair by lazy { KeyPairGenerator.getInstance("RSA").apply { initialize(2_048) }.generateKeyPair() }
        val SECOND_KEY: KeyPair by lazy { KeyPairGenerator.getInstance("RSA").apply { initialize(2_048) }.generateKeyPair() }
    }
}
