package com.jarvis.assistant.brain

import com.jarvis.assistant.data.JarvisStore
import org.json.JSONObject

/** Stores model/verification metadata, never a key, bearer, email, response sample or user prompt. */
class ProviderConnectionStore(private val store:JarvisStore):ProviderConnectionRepository {
    private fun root()=runCatching{JSONObject(store.readProviderMetadata())}.getOrElse{JSONObject()}
    @Synchronized override fun proof(id:String):ProviderVerificationProof?=runCatching {
        val p=root().optJSONObject("proofs")?.optJSONObject(id) ?: return null
        val config=ProviderConfig(p.getString("provider"),p.getString("model"),ProviderCredentialMode.valueOf(p.getString("mode"))).validated()
        require(config.id==id)
        ProviderVerificationProof(config,p.getString("binding"),p.getLong("verifiedAt"),p.getString("nonce")).also{require(it.nonce.length>=24)}
    }.getOrNull()
    @Synchronized override fun save(proof:ProviderVerificationProof){
        val root=root();val proofs=root.optJSONObject("proofs") ?: JSONObject()
        proofs.put(proof.config.id,JSONObject().put("provider",proof.config.provider).put("model",proof.config.model).put("mode",proof.config.mode.name)
            .put("binding",proof.credentialBinding).put("verifiedAt",proof.verifiedAt).put("nonce",proof.nonce))
        root.put("proofs",proofs);store.writeProviderMetadata(root.toString())
    }
    @Synchronized override fun remove(id:String){val root=root();root.optJSONObject("proofs")?.remove(id);store.writeProviderMetadata(root.toString())}
    @Synchronized override fun mode(provider:String):ProviderCredentialMode=runCatching{ProviderCredentialMode.valueOf(root().optJSONObject("modes")?.getString(provider) ?: "")}.getOrElse{if(provider=="local")ProviderCredentialMode.LOCAL else ProviderCredentialMode.API_KEY}
    @Synchronized override fun setMode(provider:String,mode:ProviderCredentialMode){val root=root();val modes=root.optJSONObject("modes") ?: JSONObject();modes.put(provider,mode.name);root.put("modes",modes);store.writeProviderMetadata(root.toString())}
    @Synchronized override fun configuredModel(id:String)=root().optJSONObject("models")?.optString(id)?.takeIf{it.isNotBlank()}
    @Synchronized override fun saveConfigured(config:ProviderConfig){val root=root();val models=root.optJSONObject("models") ?: JSONObject();models.put(config.id,config.model);root.put("models",models);store.writeProviderMetadata(root.toString())}
}
