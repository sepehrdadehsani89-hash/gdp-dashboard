package com.jarvis.assistant.omnisystem

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Independent additive store. Tokens/templates never enter backups, logs or LLM context. */
class FeatureVault internal constructor(context:Context,private val alias:String="jarvis_omnisystem_private_v1") {
    private val prefs=context.applicationContext.getSharedPreferences(alias,Context.MODE_PRIVATE)
    @Synchronized private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun read(name:String):String? {
        val raw=prefs.getString(name,null) ?: return null
        return try {
            val bytes=Base64.decode(raw,Base64.NO_WRAP)
            require(bytes.size>=29)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
            cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
            cipher.doFinal(bytes.copyOfRange(12,bytes.size)).toString(Charsets.UTF_8)
        } catch(_:Exception){null} // Invalidated keys are unavailable, never treated as plaintext.
    }
    @Synchronized fun write(name:String,value:String){
        require(name.matches(Regex("[a-z_]{1,60}")) && value.toByteArray().size<=1_000_000)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key());cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val encoded=Base64.encodeToString(cipher.iv+cipher.doFinal(value.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
        check(prefs.edit().putString(name,encoded).commit()){"Private storage is unavailable."}
    }
    @Synchronized fun remove(name:String){check(prefs.edit().remove(name).commit()){"Private storage is unavailable."}}
    fun contains(name:String)=prefs.contains(name)
    @Synchronized fun clear(){
        check(prefs.edit().clear().commit()){"Private storage is unavailable."}
        KeyStore.getInstance("AndroidKeyStore").apply{load(null)}.deleteEntry(alias)
    }
}
