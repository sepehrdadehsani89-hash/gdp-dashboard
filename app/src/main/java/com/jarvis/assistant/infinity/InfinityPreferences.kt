package com.jarvis.assistant.infinity

import android.content.Context
import com.jarvis.assistant.tools.ToolPolicy
import org.json.JSONArray
import org.json.JSONObject

data class SavedWebsite(val name:String,val url:String,val description:String)
class InfinityPreferences(context:Context){
    private val prefs=context.getSharedPreferences("jarvis_infinity_v7",Context.MODE_PRIVATE)
    fun websites():List<SavedWebsite> = runCatching{val a=JSONArray(prefs.getString("sites","[]"));(0 until a.length()).map{i->val s=a.getJSONObject(i);SavedWebsite(s.getString("name"),s.getString("url"),s.optString("description"))}}.getOrDefault(emptyList())
    fun saveWebsite(site:SavedWebsite){
        val next=site.copy(name=site.name.trim().take(60),url=site.url.trim(),description=site.description.trim().take(160))
        require(next.name.isNotBlank() && next.url.startsWith("https://") && ToolPolicy.allowedWebUrl(next.url) && android.net.Uri.parse(next.url).userInfo==null){"Use a named HTTPS website without embedded credentials."}
        val all=websites().filterNot{it.name.equals(next.name,true)}+next;require(all.size<=10){"Ten websites maximum. Delete one before adding another."}
        prefs.edit().putString("sites",JSONArray().apply{all.forEach{put(JSONObject().put("name",it.name).put("url",it.url).put("description",it.description))}}.toString()).apply()
    }
    fun removeWebsite(name:String){val all=websites().filterNot{it.name==name};prefs.edit().putString("sites",JSONArray().apply{all.forEach{put(JSONObject().put("name",it.name).put("url",it.url).put("description",it.description))}}.toString()).apply()}
    fun aliases():Map<String,String> = runCatching{val j=JSONObject(prefs.getString("aliases","{}") ?: "{}");j.keys().asSequence().filterNotNull().associateWith{j.getString(it)}}.getOrDefault(emptyMap())
    fun alias(name:String,address:String,available:List<Accessory>){val n=DevicePolicy.normalizedName(name).take(60);require(n.length>=2 && available.any{it.address==address});val all=aliases()+ (n to address);require(all.size<=40);prefs.edit().putString("aliases",JSONObject(all).toString()).apply()}
    fun removeAlias(name:String){prefs.edit().putString("aliases",JSONObject(aliases()-name).toString()).apply()}
}
