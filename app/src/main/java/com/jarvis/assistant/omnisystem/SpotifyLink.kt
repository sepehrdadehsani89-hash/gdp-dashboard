package com.jarvis.assistant.omnisystem

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class SpotifyAuthorization(val url:String,val state:String,val verifier:String,val createdAt:Long)
object SpotifyPkce {
    const val REDIRECT="com.jarvis.next://spotify-callback"
    private fun nonce()=Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(48).also{SecureRandom().nextBytes(it)})
    fun clientId(value:String)=value.matches(Regex("[a-fA-F0-9]{32}"))
    fun create(id:String,now:Long=System.currentTimeMillis()):SpotifyAuthorization {
        require(clientId(id));val state=nonce();val verifier=nonce()
        val challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val params=linkedMapOf("client_id" to id,"response_type" to "code","redirect_uri" to REDIRECT,"scope" to "user-library-read user-library-modify","state" to state,"code_challenge_method" to "S256","code_challenge" to challenge)
        val query=params.entries.joinToString("&"){java.net.URLEncoder.encode(it.key,"UTF-8")+"="+java.net.URLEncoder.encode(it.value,"UTF-8")}
        return SpotifyAuthorization("https://accounts.spotify.com/authorize?$query",state,verifier,now)
    }
    fun validRedirect(uri:URI)=uri.scheme=="com.jarvis.next" && uri.host=="spotify-callback" && uri.userInfo==null && uri.port==-1 && uri.path.orEmpty().isEmpty() && uri.fragment==null
    fun validState(expected:String,actual:String?,createdAt:Long,now:Long)=actual!=null && expected.length>=32 && now-createdAt in 0..300_000 && MessageDigest.isEqual(expected.toByteArray(),actual.toByteArray())
}
/** Native public-client OAuth: PKCE only, no embedded client secret, exact callback and replay guard. */
class SpotifyLink(private val vault:FeatureVault,private val http:ModuleHttp=ModuleHttp()) {
    fun configured()=SpotifyPkce.clientId(vault.read("spotify_client_id").orEmpty())
    fun linked()=vault.read("spotify_tokens")!=null
    fun start():String {
        val id=vault.read("spotify_client_id").orEmpty()
        val request=SpotifyPkce.create(id)
        vault.write("spotify_pending",JSONObject().put("state",request.state).put("verifier",request.verifier).put("created",request.createdAt).put("client",id).toString())
        return request.url
    }
    suspend fun callback(uri:android.net.Uri){
        require(uri.toString().length<=10_000 && uri.getQueryParameters("state").size==1 && uri.getQueryParameters("code").size<=1 && uri.getQueryParameters("error").size<=1){"Ambiguous Spotify callback."}
        require(SpotifyPkce.validRedirect(URI(uri.toString()))){"Invalid callback."}
        val raw=vault.read("spotify_pending") ?: error("No pending Spotify authorization.")
        val pending=JSONObject(raw)
        require(SpotifyPkce.validState(pending.getString("state"),uri.getQueryParameter("state"),pending.getLong("created"),System.currentTimeMillis())){"Spotify state is invalid or expired."}
        vault.remove("spotify_pending")
        require(uri.getQueryParameter("error")==null){"Spotify authorization was declined."}
        val code=uri.getQueryParameter("code")?.takeIf{it.length in 1..2048 && it.none(Char::isISOControl)} ?: error("Missing Spotify code.")
        val body=FormBody.Builder().add("grant_type","authorization_code").add("code",code).add("redirect_uri",SpotifyPkce.REDIRECT)
            .add("client_id",pending.getString("client")).add("code_verifier",pending.getString("verifier")).build()
        val response=http.json(Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build())
        currentCoroutineContext().ensureActive()
        check(vault.read("spotify_client_id")==pending.getString("client")){"Spotify configuration changed."}
        vault.write("spotify_tokens",tokenBundle(response,null).toString())
    }
    private fun tokenBundle(response:JSONObject,refresh:String?):JSONObject {
        val access=response.getString("access_token");val next=response.optString("refresh_token").takeIf{it.isNotBlank()} ?: refresh
        val seconds=response.getLong("expires_in")
        require(response.optString("token_type").equals("Bearer",true) && access.length in 1..8192 && access.none{it.isWhitespace() || it.isISOControl()} && seconds in 1..86400)
        require(next!=null && next.length in 1..8192 && next.none{it.isWhitespace() || it.isISOControl()})
        return JSONObject().put("access",access).put("refresh",next).put("expires",System.currentTimeMillis()+seconds*1000)
    }
    private suspend fun accessToken():String {
        val raw=vault.read("spotify_tokens") ?: error("Authorize Spotify first.")
        val token=JSONObject(raw)
        if(token.getLong("expires")>System.currentTimeMillis()+60_000)return token.getString("access")
        val body=FormBody.Builder().add("grant_type","refresh_token").add("refresh_token",token.getString("refresh"))
            .add("client_id",vault.read("spotify_client_id") ?: error("Configure Spotify first.")).build()
        val response=http.json(Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build())
        currentCoroutineContext().ensureActive()
        check(vault.read("spotify_tokens")==raw){"Spotify authorization changed."}
        val next=tokenBundle(response,token.getString("refresh"));vault.write("spotify_tokens",next.toString())
        return next.getString("access")
    }
    suspend fun save(track:String):Boolean {
        require(MusicResultPolicy.trackId(track)!=null)
        val token=accessToken()
        http.bytes(Request.Builder().url("https://api.spotify.com/v1/me/tracks?ids=$track").header("Authorization","Bearer $token").put(ByteArray(0).toRequestBody()).build(),100_000)
        val bytes=http.bytes(Request.Builder().url("https://api.spotify.com/v1/me/tracks/contains?ids=$track").header("Authorization","Bearer $token").get().build(),100_000)
        val result=org.json.JSONArray(bytes.toString(Charsets.UTF_8))
        return result.length()==1 && result.get(0)==true
    }
    fun disconnect(){vault.remove("spotify_tokens");vault.remove("spotify_pending")}
}

class SpotifyCallbackActivity:PrivateModuleActivity(){
    override fun onCreate(savedInstanceState:android.os.Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        screen("Spotify"){androidx.compose.material3.Text(words("Only the authorization you started in Jarvis can be accepted. No client secret is embedded.","Solo se acepta la autorización iniciada en Jarvis. No se incluye un secreto de cliente.","فقط مجوزی که در جارویس آغاز کرده‌اید پذیرفته می‌شود. رمز برنامه جاسازی نشده است."))}
        task {
            SpotifyLink(vault).callback(intent.data ?: error("Missing callback."));finish()
        }
    }
}
