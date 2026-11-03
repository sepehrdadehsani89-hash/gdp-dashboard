package com.jarvis.assistant.omnisystem

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class MusicSecurityPolicyTest {
    @Test fun noMatchIsNotFabricated(){assertNull(MusicResultPolicy.parse(JSONObject("""{"status":"success","result":null}""")))}
    @Test fun invalidProviderStatusDoesNotBecomeASong(){assertThrows(IllegalArgumentException::class.java){MusicResultPolicy.parse(JSONObject("""{"status":"error","result":{"title":"not a match","artist":"fixture"}}"""))}}
    @Test fun genuineFieldsAreBoundedAndArtworkIsRestricted(){
        val source=JSONObject("""{"status":"success","result":{"title":"Fixture song","artist":"Fixture artist","album":"Fixture album","spotify":{"id":"0123456789ABCDEFGHIJKL","album":{"images":[{"url":"https://i.scdn.co/image/fixture"}]}}}}""")
        val value=MusicResultPolicy.parse(source)!!
        assertEquals("Fixture song",value.title);assertEquals("0123456789ABCDEFGHIJKL",value.spotifyId)
        assertEquals("https://i.scdn.co/image/fixture",value.artwork)
    }
    @Test fun rejectsUnsafeImageOriginsAndInvalidTrackIds(){
        listOf("http://i.scdn.co/image/x","https://i.scdn.co.attacker.example/x","https://i.scdn.co@attacker.example/x","file:///private/x","https://i.scdn.co:8443/x").forEach{assertNull(MusicResultPolicy.artwork(it))}
        listOf("../wrong","spotify:track:anything","0123").forEach{assertNull(MusicResultPolicy.trackId(it))}
    }
    @Test fun pkceAuthorizationUsesExactRedirectAndFreshIndependentSecrets(){
        val id="a".repeat(32);val a=SpotifyPkce.create(id,1000);val b=SpotifyPkce.create(id,1000)
        assertNotEquals(a.state,b.state);assertNotEquals(a.state,a.verifier)
        val fields=URI(a.url).rawQuery.split('&').associate{val pair=it.split('=',limit=2);URLDecoder.decode(pair[0],"UTF-8") to URLDecoder.decode(pair[1],"UTF-8")}
        assertEquals("accounts.spotify.com",URI(a.url).host);assertEquals("S256",fields["code_challenge_method"])
        assertEquals(SpotifyPkce.REDIRECT,fields["redirect_uri"]);assertFalse(fields.containsKey("client_secret"))
        assertTrue(a.verifier.length in 43..128);assertEquals(a.state,fields["state"])
    }
    @Test fun callbackStateExpiresAndRejectsClockRollbackOrWrongNonce(){
        val state="x".repeat(64)
        assertTrue(SpotifyPkce.validState(state,state,1000,2000))
        assertFalse(SpotifyPkce.validState(state,"y".repeat(64),1000,2000))
        assertFalse(SpotifyPkce.validState(state,state,1000,301001))
        assertFalse(SpotifyPkce.validState(state,state,1000,999))
    }
    @Test fun callbackOriginCannotBeConfusedWithAnotherAppOrEndpoint(){
        assertTrue(SpotifyPkce.validRedirect(URI(SpotifyPkce.REDIRECT+"?code=fixture")))
        listOf("https://spotify-callback","com.jarvis.next://spotify-callback.attacker.example","com.jarvis.next://spotify-callback/path","com.jarvis.next://spotify-callback#fragment","com.jarvis.next://user@spotify-callback").forEach{assertFalse(SpotifyPkce.validRedirect(URI(it)))}
    }
    @Test fun wavHasActualPcmDurationFormatAndPayload(){
        val pcm=ByteArray(16000*2*10){(it%91).toByte()};val wav=WavePcm.encode(pcm)
        val h=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF",wav.copyOfRange(0,4).toString(Charsets.US_ASCII));assertEquals(pcm.size+36,h.getInt(4))
        assertEquals(16000,h.getInt(24));assertEquals(32000,h.getInt(28));assertEquals(1,h.getShort(22).toInt())
        assertEquals(pcm.size,h.getInt(40));assertArrayEquals(pcm,wav.copyOfRange(44,wav.size))
    }
    @Test fun spectrumUsesActualAudioRatherThanDecorativeValues(){
        assertTrue(AudioSpectrum.bands(ByteArray(3200),3200).all{it==0f})
        val pcm=ByteArray(3200)
        for(i in 0 until 1600){val v=(sin(2*PI*1000*i/16000)*15000).toInt();pcm[2*i]=v.toByte();pcm[2*i+1]=(v shr 8).toByte()}
        val values=AudioSpectrum.bands(pcm,pcm.size)
        assertEquals(AudioSpectrum.frequencies.indexOf(1000),values.indices.maxBy{values[it]})
        assertTrue(values[4]>.4f)
    }
}
