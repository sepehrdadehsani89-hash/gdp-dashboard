package com.jarvis.assistant.brain.auth

import com.jarvis.assistant.omnisystem.FeatureVault
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

const val CHATGPT_PLAN_SCOPE = "chatgpt.tokens.use.direct"
private const val RESOURCE = "https://api.openai.com/v1"
private const val CALLBACK_PATH = "/auth/callback"
private const val ACCOUNT_ENTRY = "brain_openai_accounts"
private const val HOST_ENTRY = "brain_openai_host"
private const val REGISTRATION_ENTRY = "brain_openai_pending_registration"
private val REQUESTED_SCOPES = listOf("openid", "profile", "email", "offline_access", "resource.invoke", CHATGPT_PLAN_SCOPE)

/** Public state contains identity and permission information, never OAuth secrets. */
data class OpenAiAccountView(
    val accountId: String,
    val label: String,
    val email: String?,
    val displayName: String?,
    val signedIn: Boolean,
    val planAuthorized: Boolean,
    val expiresAtEpochMillis: Long,
    val revision: Long,
)

data class OpenAiAuthState(
    val accounts: List<OpenAiAccountView> = emptyList(),
    val selectedAccountId: String? = null,
    val signingIn: Boolean = false,
    val message: String? = null,
)

/** Safe proof binding. This is deliberately separate from the bearer credential. */
data class OpenAiCredentialDescriptor(
    val accountId: String,
    val clientId: String,
    val subject: String,
    val scopes: Set<String>,
    val revision: Long,
)

data class OpenAiDisconnectResult(val remoteRevocationConfirmed: Boolean, val message: String)

class OpenAiAuthException(message: String,val kind:com.jarvis.assistant.brain.ProviderFailureKind=com.jarvis.assistant.brain.ProviderFailureKind.AUTHENTICATION) : IllegalStateException(message)

internal interface OpenAiAuthStorage {
    fun read(name: String): String?
    fun write(name: String, value: String)
    fun remove(name: String)
}

private class VaultAuthStorage(private val vault: FeatureVault) : OpenAiAuthStorage {
    override fun read(name: String) = vault.read(name)
    override fun write(name: String, value: String) = vault.write(name, value)
    override fun remove(name: String) = vault.remove(name)
}

internal data class OpenAiOAuthEndpoints(
    val authorize: String = "https://auth.openai.com/api/accounts/authorize",
    val token: String = "https://auth.openai.com/api/accounts/oauth/token",
    val revoke: String = "https://auth.openai.com/api/accounts/oauth/revoke",
    val jwks: String = "https://auth.openai.com/.well-known/jwks.json",
)

/**
 * Application-owned, five-minute SIWC public-client flow. Activity pause/disposal does not cancel it.
 * Only callback-issued registrations are exchanged; no secret or first-party client ID is used.
 */
class OpenAiAuthManager internal constructor(
    private val storage: OpenAiAuthStorage,
    client: OkHttpClient = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build(),
    private val endpoints: OpenAiOAuthEndpoints = OpenAiOAuthEndpoints(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val timeoutMillis: Long = 5 * 60_000L,
    private val validateIdentity: suspend (String, String, String) -> ValidatedOpenAiIdentity =
        OpenAiIdTokenValidator(client, endpoints.jwks, nowMillis = nowMillis)::validate,
) {
    constructor(vault: FeatureVault) : this(VaultAuthStorage(vault))

    private val http = client.newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    private val lock = Any()
    private val refreshLock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var generation = 0L
    private var pending: PendingSignIn? = null
    private val mutableState = MutableStateFlow(OpenAiAuthState())
    val state: StateFlow<OpenAiAuthState> = mutableState.asStateFlow()

    init { synchronized(lock) { publishLocked() } }

    /** Starts the loopback listener before returning the official authorization URL. */
    fun beginSignIn(accountId: String? = null): String = synchronized(lock) {
        cancelPendingLocked()
        val bundle = readBundle()
        val existing = accountId?.let { id -> bundle.accounts.firstOrNull { it.id == id }
            ?: throw OpenAiAuthException("Choose a saved ChatGPT account or add a new one.") }
        val hostId = storage.read(HOST_ENTRY)?.takeIf { it.startsWith("urn:uuid:") }
            ?: "urn:uuid:${UUID.randomUUID()}".also { storage.write(HOST_ENTRY, it) }
        val retryClient = if (accountId == null) pendingRegistration(hostId)?.takeUnless { clientId ->
            bundle.accounts.any { it.clientId == clientId }
        } else null
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 500 }
        val attempt = PendingSignIn(
            server, hostId, existing?.id, existing?.clientId ?: retryClient, existing?.subject,
            existing?.idToken?.takeIf { it.isNotBlank() }, randomValue(), randomValue(), randomValue(64),
            generation, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis),
        )
        pending = attempt
        val authorization = endpoints.authorize.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", attempt.existingClientId ?: "dynamic_agent_client")
            .addQueryParameter("ext_agent_host_id", hostId)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", attempt.redirectUri)
            .addQueryParameter("scope", REQUESTED_SCOPES.joinToString(" "))
            .addQueryParameter("resource", RESOURCE)
            .addQueryParameter("state", attempt.state)
            .addQueryParameter("nonce", attempt.nonce)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("code_challenge", base64Url(MessageDigest.getInstance("SHA-256").digest(attempt.verifier.toByteArray(Charsets.US_ASCII))))
            .apply {
                if (attempt.existingClientId == null) addQueryParameter("agent_name_hint", "J.A.R.V.I.S.")
                attempt.idTokenHint?.let { addQueryParameter("id_token_hint", it) }
                existing?.email?.let { addQueryParameter("login_hint", it) }
            }.build().toString()
        publishLocked(signingIn = true)
        scope.launch { awaitCallback(attempt) }
        authorization
    }

    fun cancelSignIn() = synchronized(lock) {
        cancelPendingLocked()
        publishLocked(message = "ChatGPT sign-in was canceled.")
    }

    fun selectAccount(accountId: String) = synchronized(lock) {
        val bundle = readBundle()
        if (bundle.accounts.none { it.id == accountId }) throw OpenAiAuthException("The ChatGPT account is unavailable.")
        cancelPendingLocked()
        writeBundle(Bundle(accountId, bundle.accounts))
        publishLocked()
    }

    /** Refresh UI after an application-wide vault reset. Inference always rechecks the vault. */
    fun refreshState() = synchronized(lock) { publishLocked() }

    fun credentialDescriptor(): OpenAiCredentialDescriptor? = synchronized(lock) {
        val bundle = readBundle()
        bundle.accounts.firstOrNull { it.id == bundle.selected }?.takeIf { it.planAuthorized }
            ?.let { OpenAiCredentialDescriptor(it.id, it.clientId, it.subject, it.scopes.toSet(), it.revision) }
    }

    /** Selected-account plan token only. No API-key fallback exists in this component. */
    suspend fun bearerToken(): String = refreshLock.withLock {
        currentCoroutineContext().ensureActive()
        // A rotating grant cannot be rolled back remotely when an Activity pauses. Finish and
        // persist this bounded transaction, while account/generation checks still defeat revoke.
        withContext(Dispatchers.IO + NonCancellable) {
            val snapshot = synchronized(lock) {
                val bundle = readBundle()
                val account = bundle.accounts.firstOrNull { it.id == bundle.selected }
                    ?: throw OpenAiAuthException("Continue with ChatGPT to authorize AI access.")
                if (!account.planAuthorized) throw OpenAiAuthException("ChatGPT is signed in, but AI access has not been authorized.")
                val host = storage.read(HOST_ENTRY) ?: throw OpenAiAuthException("The ChatGPT installation identity was removed. Sign in again.")
                Triple(account, generation, host)
            }
            val account = snapshot.first
            val now = nowMillis()
            if (account.expiresAt > now + 60_000L) {
                return@withContext synchronized(lock) {
                    requireCurrentLocked(account, snapshot.second, snapshot.third)
                    account.accessToken
                }
            }
            if (account.refreshToken.isBlank()) {
                throw OpenAiAuthException("Your ChatGPT session expired. Continue with ChatGPT again.")
            }
            val body = FormBody.Builder().add("grant_type", "refresh_token")
                .add("client_id", account.clientId).add("refresh_token", account.refreshToken)
                .add("resource", RESOURCE).build()
            val response = executeTokenRequest(body)
            currentCoroutineContext().ensureActive()
            if (response.error != null) {
                if (response.error in TERMINAL_REFRESH_ERRORS) synchronized(lock) {
                    requireCurrentLocked(account, snapshot.second, snapshot.third)
                    replaceAccountLocked(account.withoutCredentials(nextRevision(readBundle())), "Your ChatGPT session expired. Continue with ChatGPT again.")
                }
                throw OpenAiAuthException(if (response.error in TERMINAL_REFRESH_ERRORS)
                    "Your ChatGPT session expired. Continue with ChatGPT again." else "ChatGPT token renewal failed. Saved credentials were retained; retry reconnect.",if(response.error in TERMINAL_REFRESH_ERRORS)com.jarvis.assistant.brain.ProviderFailureKind.AUTHENTICATION else if(response.status==429)com.jarvis.assistant.brain.ProviderFailureKind.RATE_LIMIT else if(response.status>=500)com.jarvis.assistant.brain.ProviderFailureKind.USAGE_UNAVAILABLE else com.jarvis.assistant.brain.ProviderFailureKind.AUTHORIZATION)
            }
            val tokens = parseTokens(response.json, account.idToken, requireIdToken = false,
                retainedScopes = account.scopes, retainedRefresh = account.refreshToken)
            if (response.json.optString("id_token").isNotBlank()) {
                if (account.nonce.isBlank()) throw OpenAiAuthException("Continue with ChatGPT again to validate the renewed identity.")
                val identity = validateIdentity(tokens.idToken, account.clientId, account.nonce)
                if (identity.subject != account.subject) throw OpenAiAuthException("The renewed ChatGPT identity does not match this account.")
            }
            val renewed = account.withRefreshed(tokens)
            synchronized(lock) {
                requireCurrentLocked(account, snapshot.second, snapshot.third)
                val updated = renewed.withRevision(if (renewed.scopes == account.scopes) account.revision else nextRevision(readBundle()))
                replaceAccountLocked(updated)
                if (!updated.planAuthorized) throw OpenAiAuthException("ChatGPT AI access is no longer authorized.")
                updated.accessToken
            }
        }
    }

    /** Local disconnect wins immediately, including against any stale code exchange or refresh. */
    suspend fun disconnect(accountId: String? = null): OpenAiDisconnectResult = withContext(Dispatchers.IO) {
        val removed = synchronized(lock) {
            cancelPendingLocked()
            val bundle = readBundle()
            val id = accountId ?: bundle.selected
            val account = bundle.accounts.firstOrNull { it.id == id }
            if (account != null) {
                replaceAccountLocked(account.withoutCredentials(nextRevision(bundle)), "ChatGPT disconnected.")
            } else publishLocked(message = "ChatGPT disconnected.")
            account
        }
        if (removed == null || removed.refreshToken.isBlank()) {
            val confirmed = removed == null || removed.accessToken.isBlank()
            val message = if (confirmed) "ChatGPT disconnected." else "Disconnected on this device. No refresh token was available to confirm remote revocation; review ChatGPT settings."
            synchronized(lock) { publishLocked(message = message) }
            return@withContext OpenAiDisconnectResult(confirmed, message)
        }
        val confirmed = try {
            http.newCall(Request.Builder().url(endpoints.revoke).post(FormBody.Builder()
                .add("token", removed.refreshToken).add("token_type_hint", "refresh_token")
                .add("client_id", removed.clientId).build()).build()).execute().use { it.code == 200 }
        } catch (_: Exception) { false }
        val result = OpenAiDisconnectResult(confirmed, if (confirmed) "ChatGPT disconnected."
            else "Disconnected on this device. Remote revocation was not confirmed; disconnect J.A.R.V.I.S. in ChatGPT settings.")
        synchronized(lock) {
            // Do not replace a newer sign-in's status with this older revocation result.
            val current = readBundle().accounts.firstOrNull { it.id == removed.id }
            if (current != null && current.accessToken.isBlank() && current.refreshToken.isBlank()) publishLocked(message = result.message)
        }
        result
    }

    /** Removes only this app's saved connection. It never deletes a provider account/workspace. */
    suspend fun removeSavedConnection(accountId:String):OpenAiDisconnectResult=withContext(Dispatchers.IO){
        val removed=synchronized(lock){
            cancelPendingLocked()
            val bundle=readBundle();val account=bundle.accounts.firstOrNull{it.id==accountId} ?: return@synchronized null
            val retained=bundle.accounts.filterNot{it.id==accountId}
            writeBundle(Bundle(if(bundle.selected==accountId)retained.firstOrNull()?.id else bundle.selected,retained))
            publishLocked(message="Saved ChatGPT connection removed from this device.");account
        }
        val revoked=if(removed==null || removed.refreshToken.isBlank())removed==null || removed.accessToken.isBlank() else try{
            http.newCall(Request.Builder().url(endpoints.revoke).post(FormBody.Builder().add("token",removed.refreshToken).add("token_type_hint","refresh_token").add("client_id",removed.clientId).build()).build()).execute().use{it.code==200}
        }catch(_:Exception){false}
        OpenAiDisconnectResult(revoked,if(revoked)"Local connection removed and its refresh authorization revoked. No ChatGPT account or workspace was deleted." else "Local connection removed. Remote revocation was not confirmed; review connected apps in ChatGPT. No provider account or workspace was deleted.")
    }

    private suspend fun awaitCallback(attempt: PendingSignIn) {
        try {
            while (isPending(attempt) && System.nanoTime() < attempt.deadlineNanos) {
                val socket = try { attempt.server.accept() } catch (_: SocketTimeoutException) { continue }
                attempt.socket = socket
                socket.use {
                    it.soTimeout = 2_000
                    val callback = try { readCallback(it, attempt.server.localPort) } catch (_: Exception) {
                        respond(it, false); null
                    } ?: return@use
                    if (!constantTimeEquals(callback.state, attempt.state)) { respond(it, false); return@use }
                    if (callback.error != null) {
                        respond(it, true)
                        finishFailure(attempt, if (callback.error == "access_denied") "ChatGPT sign-in was canceled." else "ChatGPT authorization was denied. Please try again.")
                        return
                    }
                    val issuedClientId = callback.clientId ?: attempt.existingClientId
                    if (callback.code.isNullOrBlank() || !validIssuedClientId(issuedClientId)
                        || (attempt.existingClientId != null && issuedClientId != attempt.existingClientId)) {
                        respond(it, false)
                        finishFailure(attempt, "The ChatGPT authorization callback could not be verified.")
                        return
                    }
                    synchronized(lock) {
                        if (!isPendingLocked(attempt)) return
                        if (attempt.accountId == null) storage.write(REGISTRATION_ENTRY,
                            JSONObject().put("host_id", attempt.hostId).put("client_id", issuedClientId).toString())
                    }
                    respond(it, true)
                    attempt.server.close()
                    exchangeCode(attempt, callback.code, issuedClientId!!)
                    return
                }
                attempt.socket = null
            }
            if (isPending(attempt)) finishFailure(attempt, "ChatGPT sign-in timed out. Please try again.")
        } catch (_: Exception) {
            if (isPending(attempt)) finishFailure(attempt, "ChatGPT sign-in failed. Please try again.")
        } finally {
            runCatching { attempt.server.close() }
            runCatching { attempt.socket?.close() }
        }
    }

    private suspend fun exchangeCode(attempt: PendingSignIn, code: String, issuedClientId: String) {
        val form = FormBody.Builder().add("grant_type", "authorization_code").add("client_id", issuedClientId)
            .add("code", code).add("code_verifier", attempt.verifier).add("redirect_uri", attempt.redirectUri)
            .add("resource", RESOURCE).build()
        val response = executeTokenRequest(form, attempt)
        if (response.error != null) {
            finishFailure(attempt, if (response.error == "invalid_grant") "ChatGPT authorization expired. Start sign-in again."
                else "ChatGPT could not complete authorization. Please try again.")
            return
        }
        val tokens = parseTokens(response.json, "", requireIdToken = true)
        val identity = validateIdentity(tokens.idToken, issuedClientId, attempt.nonce)
        synchronized(lock) {
            if (!isPendingLocked(attempt)) return
            if (attempt.expectedSubject != null && identity.subject != attempt.expectedSubject) {
                finishFailure(attempt, "The returned ChatGPT identity does not match the selected account.")
                return
            }
            val bundle = readBundle()
            val existing = bundle.accounts.firstOrNull { it.clientId == issuedClientId && it.subject == identity.subject }
            val account = SavedAccount(existing?.id ?: UUID.randomUUID().toString(), issuedClientId, identity.subject,
                identity.email, identity.name, tokens.idToken, tokens.accessToken, tokens.refreshToken, tokens.scopes,
                tokens.expiresAt, nextRevision(bundle), attempt.nonce)
            val updated = bundle.accounts.filterNot { it.id == account.id } + account
            writeBundle(Bundle(account.id, updated))
            if (pendingRegistration(attempt.hostId) == issuedClientId) storage.remove(REGISTRATION_ENTRY)
            pending = null
            generation++
            publishLocked(message = if (account.planAuthorized) "ChatGPT authorized. Test Connection to verify AI access."
                else "ChatGPT signed in. AI access has not been authorized.")
        }
    }

    private fun executeTokenRequest(form: FormBody, attempt: PendingSignIn? = null): TokenResponse {
        val call = http.newCall(Request.Builder().url(endpoints.token).post(form).header("Accept", "application/json").build())
        if (attempt != null) synchronized(lock) {
            if (!isPendingLocked(attempt)) throw OpenAiAuthException("ChatGPT sign-in was canceled.")
            attempt.call = call
        }
        try {
            return call.execute().use { response ->
                val body = response.body ?: throw OpenAiAuthException("ChatGPT returned an empty authorization response.")
                val source = body.source()
                if (source.request(512_001) && source.buffer.size > 512_000) throw OpenAiAuthException("ChatGPT returned an invalid authorization response.")
                val json = try { JSONObject(source.readUtf8()) } catch (_: Exception) {
                    throw OpenAiAuthException("ChatGPT returned an invalid authorization response.")
                }
                if (!response.isSuccessful) TokenResponse(json, json.optString("error").takeIf { it.isNotBlank() } ?: "request_failed",response.code)
                else TokenResponse(json, null)
            }
        } catch (error: OpenAiAuthException) { throw error }
        catch (error:java.io.IOException) { throw OpenAiAuthException("ChatGPT authorization could not reach OpenAI. Saved access was retained; retry reconnect.",if(error is java.net.SocketTimeoutException)com.jarvis.assistant.brain.ProviderFailureKind.TIMEOUT else com.jarvis.assistant.brain.ProviderFailureKind.NETWORK) }
        catch (_: Exception) { throw OpenAiAuthException("ChatGPT authorization response could not be validated.",com.jarvis.assistant.brain.ProviderFailureKind.INCOMPLETE) }
    }

    private fun parseTokens(json: JSONObject, retainedId: String, requireIdToken: Boolean, retainedScopes: Set<String> = emptySet(), retainedRefresh: String = ""): Tokens {
        val id = json.optString("id_token").takeIf { it.isNotBlank() } ?: retainedId
        if (requireIdToken && id.isBlank()) throw OpenAiAuthException("ChatGPT did not return a verifiable identity.")
        val access = json.optString("access_token")
        if (access.isNotBlank() && !json.optString("token_type").equals("Bearer", true)) throw OpenAiAuthException("ChatGPT returned an unsupported token type.")
        val expires = (json.opt("expires_in") as? Number)?.let { value ->
            value.toLong().takeIf { it.toDouble() == value.toDouble() }
        } ?: 0L
        if (access.isNotBlank() && expires !in 1..86_400) throw OpenAiAuthException("ChatGPT returned an invalid session lifetime.")
        val scopes = if (!requireIdToken && !json.has("scope")) retainedScopes else json.optString("scope").split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        if (scopes.size > 64 || scopes.any { it.length > 128 }) throw OpenAiAuthException("ChatGPT returned invalid authorization scopes.")
        // earliest_refresh_at has no documented type/semantics in the current reference.
        // Ignore it; only expires_in determines local expiry, and the server decides refresh eligibility.
        return Tokens(id, access, json.optString("refresh_token").ifBlank { retainedRefresh }, scopes, nowMillis() + expires * 1_000L)
    }

    private fun requireCurrentLocked(account: SavedAccount, capturedGeneration: Long, capturedHost: String) {
        val bundle = readBundle()
        val saved = bundle.accounts.firstOrNull { it.id == account.id }
        if (storage.read(HOST_ENTRY) != capturedHost || generation != capturedGeneration || bundle.selected != account.id || saved == null || saved.revision != account.revision
            || saved.accessToken != account.accessToken) throw OpenAiAuthException("The selected ChatGPT connection changed. Please try again.")
    }

    private fun replaceAccountLocked(account: SavedAccount, message: String? = null) {
        val bundle = readBundle()
        writeBundle(Bundle(bundle.selected, bundle.accounts.map { if (it.id == account.id) account else it }))
        publishLocked(message = message)
    }

    private fun nextRevision(bundle: Bundle) = (bundle.accounts.maxOfOrNull { it.revision } ?: 0L) + 1L

    /** Registration survives failure without retaining any code, nonce or verifier. */
    private fun pendingRegistration(hostId: String): String? = runCatching {
        val json = JSONObject(storage.read(REGISTRATION_ENTRY) ?: return null)
        json.optString("client_id").takeIf { json.optString("host_id") == hostId && validIssuedClientId(it) }
    }.getOrNull()

    private fun finishFailure(attempt: PendingSignIn, message: String) = synchronized(lock) {
        if (isPendingLocked(attempt)) {
            pending = null
            generation++
            runCatching { attempt.server.close() }
            publishLocked(message = message)
        }
    }

    private fun isPending(attempt: PendingSignIn) = synchronized(lock) { isPendingLocked(attempt) }
    private fun isPendingLocked(attempt: PendingSignIn) = pending === attempt && generation == attempt.generation
        && storage.read(HOST_ENTRY) == attempt.hostId

    private fun cancelPendingLocked() {
        generation++
        pending?.let { attempt ->
            attempt.call?.cancel()
            runCatching { attempt.server.close() }
            runCatching { attempt.socket?.close() }
        }
        pending = null
    }

    private fun publishLocked(signingIn: Boolean = pending != null, message: String? = null) {
        val bundle = readBundle()
        mutableState.value = OpenAiAuthState(bundle.accounts.map { account ->
            OpenAiAccountView(account.id, "${account.name ?: account.email ?: "ChatGPT account"} · ${account.clientId.takeLast(6)}",
                account.email, account.name, account.idToken.isNotBlank(), account.planAuthorized, account.expiresAt, account.revision)
        }, bundle.selected, signingIn, message)
    }

    private fun readBundle(): Bundle {
        val raw = storage.read(ACCOUNT_ENTRY) ?: return Bundle(null, emptyList())
        return try {
            val json = JSONObject(raw)
            val accounts = json.optJSONArray("accounts") ?: JSONArray()
            val records = (0 until minOf(accounts.length(), 32)).mapNotNull { index ->
                val item = accounts.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                val clientId = item.optString("client_id")
                val subject = item.optString("subject")
                if (id.isBlank() || !validIssuedClientId(clientId) || subject.isBlank()) return@mapNotNull null
                val scopes = item.optJSONArray("scopes") ?: JSONArray()
                SavedAccount(id, clientId, subject, item.optString("email").takeIf { it.isNotBlank() },
                    item.optString("name").takeIf { it.isNotBlank() }, item.optString("id_token"), item.optString("access_token"),
                    item.optString("refresh_token"), (0 until scopes.length()).map { scopes.optString(it) }.toSet(),
                    item.optLong("expires_at"), item.optLong("revision", 1), item.optString("nonce"))
            }
            Bundle(json.optString("selected").takeIf { id -> records.any { it.id == id } }, records)
        } catch (_: Exception) { Bundle(null, emptyList()) }
    }

    private fun writeBundle(bundle: Bundle) {
        val list = JSONArray()
        bundle.accounts.forEach { account -> list.put(JSONObject().put("id", account.id).put("client_id", account.clientId)
            .put("subject", account.subject).put("email", account.email ?: "").put("name", account.name ?: "")
            .put("id_token", account.idToken).put("access_token", account.accessToken).put("refresh_token", account.refreshToken)
            .put("scopes", JSONArray(account.scopes.toList())).put("expires_at", account.expiresAt)
            .put("revision", account.revision).put("nonce", account.nonce)) }
        storage.write(ACCOUNT_ENTRY, JSONObject().put("version", 1).put("selected", bundle.selected ?: "").put("accounts", list).toString())
    }

    private class PendingSignIn(
        val server: ServerSocket, val hostId: String, val accountId: String?, val existingClientId: String?,
        val expectedSubject: String?, val idTokenHint: String?, val state: String, val nonce: String, val verifier: String,
        val generation: Long, val deadlineNanos: Long,
    ) {
        val redirectUri = "http://127.0.0.1:${server.localPort}$CALLBACK_PATH"
        @Volatile var socket: Socket? = null
        @Volatile var call: Call? = null
        override fun toString() = "PendingSignIn(redacted)"
    }

    private class SavedAccount(
        val id: String, val clientId: String, val subject: String, val email: String?, val name: String?,
        val idToken: String, val accessToken: String, val refreshToken: String, val scopes: Set<String>,
        val expiresAt: Long, val revision: Long, val nonce: String,
    ) {
        val planAuthorized get() = idToken.isNotBlank() && accessToken.isNotBlank() && scopes.containsAll(listOf(CHATGPT_PLAN_SCOPE, "resource.invoke"))
        fun withoutCredentials(revision: Long) = SavedAccount(id, clientId, subject, email, name, "", "", "", emptySet(), 0L, revision, nonce)
        fun withRevision(revision: Long) = SavedAccount(id, clientId, subject, email, name, idToken, accessToken, refreshToken, scopes, expiresAt, revision, nonce)
        fun withRefreshed(tokens: Tokens) = SavedAccount(id, clientId, subject, email, name, tokens.idToken, tokens.accessToken,
            tokens.refreshToken, tokens.scopes, tokens.expiresAt, revision, nonce)
        override fun toString() = "SavedChatGptAccount(redacted)"
    }
    private class Bundle(val selected: String?, val accounts: List<SavedAccount>)
    private class TokenResponse(val json: JSONObject, val error: String?,val status:Int=200)
    private class Tokens(val idToken: String, val accessToken: String, val refreshToken: String, val scopes: Set<String>, val expiresAt: Long)

    companion object {
        private val TERMINAL_REFRESH_ERRORS = setOf("invalid_grant", "invalid_refresh_token", "token_expired", "refresh_token_expired", "refresh_token_invalidated", "refresh_token_reused")
        private fun randomValue(bytes: Int = 32) = base64Url(ByteArray(bytes).also { SecureRandom().nextBytes(it) })
        private fun base64Url(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        private fun validIssuedClientId(value: String?) = value != null && value.length in 8..256 && value.matches(Regex("oaiapp_[A-Za-z0-9_-]+"))
        private fun constantTimeEquals(a: String?, b: String) = a != null && MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

        private class Callback(val state: String?, val code: String?, val clientId: String?, val error: String?)

        private fun readCallback(socket: Socket, port: Int): Callback {
            val input = socket.getInputStream()
            var total = 0
            fun line(): String {
                val bytes = ArrayList<Byte>()
                while (true) {
                    val value = input.read()
                    if (value < 0 || ++total > 16_384 || bytes.size > 8_192) throw IllegalArgumentException()
                    if (value == 10) break
                    if (value != 13) bytes.add(value.toByte())
                }
                return bytes.toByteArray().toString(Charsets.US_ASCII)
            }
            val request = line().split(' ')
            if (request.size != 3 || request[0] != "GET" || request[2] !in setOf("HTTP/1.1", "HTTP/1.0")) throw IllegalArgumentException()
            var host: String? = null
            repeat(65) {
                val header = line()
                if (header.isEmpty()) {
                    if (host != "127.0.0.1:$port") throw IllegalArgumentException()
                    val target = request[1]
                    if (!target.startsWith("$CALLBACK_PATH?") || '#' in target) throw IllegalArgumentException()
                    val uri = URI(target)
                    if (uri.rawPath != CALLBACK_PATH || uri.isAbsolute) throw IllegalArgumentException()
                    val values = LinkedHashMap<String, String>()
                    uri.rawQuery.orEmpty().split('&').forEach { pair ->
                        val parts = pair.split('=', limit = 2)
                        val key = URLDecoder.decode(parts[0], "UTF-8")
                        val value = URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
                        if (values.put(key, value) != null) throw IllegalArgumentException()
                    }
                    return Callback(values["state"], values["code"], values["client_id"], values["error"])
                }
                val parts = header.split(':', limit = 2)
                if (parts.size != 2) throw IllegalArgumentException()
                if (parts[0].equals("Host", true)) {
                    if (host != null) throw IllegalArgumentException()
                    host = parts[1].trim()
                }
            }
            throw IllegalArgumentException()
        }

        private fun respond(socket: Socket, accepted: Boolean) {
            val text = if (accepted) "Authorization received. Return to J.A.R.V.I.S. to check your connection." else "This authorization callback could not be verified. Return to J.A.R.V.I.S."
            val body = "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>J.A.R.V.I.S.</title></head><body><p>$text</p></body></html>".toByteArray(Charsets.UTF_8)
            val headers = "HTTP/1.1 ${if (accepted) "200 OK" else "400 Bad Request"}\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'none'\r\nConnection: close\r\n\r\n"
            runCatching { socket.getOutputStream().apply { write(headers.toByteArray(Charsets.US_ASCII)); write(body); flush() } }
        }
    }
}
