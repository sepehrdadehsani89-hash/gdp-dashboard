package com.jarvis.assistant.neural

import android.content.Context
import android.content.Intent
import com.jarvis.assistant.tools.ToolPolicy
import org.json.JSONObject

/** Android's visible launchable apps only. No QUERY_ALL_PACKAGES or private databases. */
class AppRegistry(private val context:Context){
    private val prefs=context.getSharedPreferences("jarvis_app_aliases_v4",Context.MODE_PRIVATE)
    private var cached:List<DiscoveredApp> = emptyList();private var cachedAt=0L
    @Synchronized fun invalidate(){cachedAt=0}
    @Synchronized fun apps():List<DiscoveredApp>{
        val now=android.os.SystemClock.elapsedRealtime()
        if(cachedAt>0 && now-cachedAt<60_000)return cached
        val alias=aliases()
        cached=context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).distinctBy{it.activityInfo.packageName}.take(1000).map{info->
            val pkg=info.activityInfo.packageName
            // Spelling variants never create packages; aliases apply only to discovered apps.
            val known=when(pkg){"com.whatsapp"->listOf("واتساپ","واتس اپ");"com.google.android.youtube"->listOf("یوتیوب");"org.telegram.messenger"->listOf("تلگرام");"com.instagram.android"->listOf("اینستاگرام");"com.spotify.music"->listOf("اسپاتیفای");else->emptyList()}
            DiscoveredApp(pkg,info.loadLabel(context.packageManager).toString(),known+alias.filterValues{it==pkg}.keys)
        }.filterNot{ToolPolicy.protectedPackage(it.packageId)}
        cachedAt=now;return cached
    }
    fun aliases():Map<String,String> = prefs.all.mapNotNull{(k,v)->(v as? String)?.let{k to it}}.toMap()
    @Synchronized fun saveAlias(alias:String,pkg:String){val name=AppMatching.normalize(alias);require(name.length in 2..80);require(apps().any{it.packageId==pkg}){"Choose a currently available app"};require(aliases().size<100 || name in aliases());prefs.edit().putString(name,pkg).apply();invalidate()}
    @Synchronized fun removeAlias(alias:String){prefs.edit().remove(alias).apply();invalidate()}
    @Synchronized fun restoreAliases(items:Map<String,String>):Pair<Int,Int>{
        val existing=aliases()
        val available=apps().map{it.packageId}.toSet()
        val eligible=items.filter{(name,pkg)->name !in existing && pkg in available}
        val selected=eligible.entries.take((100-existing.size).coerceAtLeast(0))
        if(selected.isNotEmpty()){
            val edit=prefs.edit();selected.forEach{edit.putString(it.key,it.value)}
            check(edit.commit()){"Alias storage unavailable. Retry the import."};invalidate()
        }
        return selected.size to (items.size-selected.size)
    }
    @Synchronized fun clear(){prefs.edit().clear().apply();invalidate()}
}
