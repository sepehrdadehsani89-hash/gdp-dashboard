package com.jarvis.assistant.google

import com.jarvis.assistant.omnisystem.FeatureVault
import org.json.JSONObject

data class GoogleCloudConfiguration(val projectId:String="",val androidClientId:String="",val apiKey:String=""){
    override fun toString()="GoogleCloudConfiguration(credentials=redacted)"
    fun validated():GoogleCloudConfiguration {
        require(projectId.isEmpty() || projectId.matches(Regex("[a-z][a-z0-9-]{4,28}[a-z0-9]"))){"Use the actual Google project ID (6–30 lowercase letters/digits/hyphens)."}
        require(androidClientId.isEmpty() || androidClientId.matches(Regex("[0-9]{1,30}-[A-Za-z0-9_-]{5,160}\\.apps\\.googleusercontent\\.com"))){"Use an Android OAuth client ID; never enter a client secret."}
        require(apiKey.isEmpty() || apiKey.matches(Regex("AIza[A-Za-z0-9_-]{35}"))){"Use a Google API key only; OAuth tokens and passwords are not API keys."}
        return this
    }
}
class GoogleCloudConfigurationStore internal constructor(private val read:()->String?,private val exists:()->Boolean,private val write:(String)->Unit,private val remove:()->Unit){
    constructor(vault:FeatureVault):this({vault.read("google_cloud_configuration")},{vault.contains("google_cloud_configuration")},{vault.write("google_cloud_configuration",it)},{vault.remove("google_cloud_configuration")})
    fun load():GoogleCloudConfiguration {
        val raw=read();if(raw==null){check(!exists()){"Private Google configuration is unavailable. Existing data was preserved."};return GoogleCloudConfiguration()}
        val j=JSONObject(raw);require(j.getInt("version")==1)
        return GoogleCloudConfiguration(j.getString("project"),j.getString("client"),j.getString("api_key")).validated()
    }
    fun save(value:GoogleCloudConfiguration){val v=value.validated();load();write(JSONObject().put("version",1).put("project",v.projectId).put("client",v.androidClientId).put("api_key",v.apiKey).toString())}
    fun clear(){remove()}
}
