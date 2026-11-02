package com.jarvis.assistant.neural

import java.text.Normalizer
import java.util.Locale

data class DiscoveredApp(val packageId:String,val label:String,val aliases:List<String> = emptyList())
data class AppMatch(val app:DiscoveredApp,val score:Double,val exact:Boolean)
object AppMatching {
    fun normalize(s:String)=Normalizer.normalize(s,Normalizer.Form.NFKD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
    private fun compact(s:String)=normalize(s).replace(" ","").take(120)
    private fun phonetic(s:String)=compact(s).replace("ph","f").replace("ck","k").replace("c","k").replace("z","s").replace(Regex("[aeiou]"),"").replace(Regex("(.)\\1+"),"$1")
    private fun distance(a:String,b:String):Int {
        var previous=IntArray(b.length+1){it}
        for(i in a.indices){val next=IntArray(b.length+1);next[0]=i+1;for(j in b.indices)next[j+1]=minOf(next[j]+1,previous[j+1]+1,previous[j]+if(a[i]==b[j])0 else 1);previous=next}
        return previous[b.length]
    }
    fun match(query:String,apps:List<DiscoveredApp>):List<AppMatch>{
        val q=compact(query);if(q.length<2)return emptyList()
        return apps.take(1000).map { app->
            val names=(listOf(app.label)+app.aliases).map(::compact).filter{it.length>=2}
            val exact=q in names
            val score=if(exact)1.0 else names.maxOfOrNull { n->
                val edit=1-distance(q,n).toDouble()/maxOf(q.length,n.length)
                maxOf(edit,if(q.length>=5 && phonetic(q)==phonetic(n)) .83 else 0.0)
            } ?: 0.0
            AppMatch(app,score,exact)
        }.filter{it.score>=.62}.sortedWith(compareByDescending<AppMatch>{it.score}.thenBy{it.app.packageId}).take(5)
    }
    fun commandTarget(input:String):String? {
        val text=normalize(input).removePrefix("jarvis ").removePrefix("جارویس ")
        val en=Regex("^(?:(?:can|could|would) you |please )?(?:please )?(?:open|launch|start) (.+?)(?: for me| please)?$").matchEntire(text)?.groupValues?.get(1)
        val es=Regex("^(?:puedes |por favor )?(?:abrir|abre|inicia|abrirme) (.+?)(?: por favor)?$").matchEntire(text)?.groupValues?.get(1)
        val fa=Regex("^(?:لطفا )?(.+?)(?: را)? باز کن$").matchEntire(text)?.groupValues?.get(1)
        return (en ?: es ?: fa)?.trim()?.takeIf{it.length in 2..100 && !it.contains(" and ") && !it.contains(" then ")}
    }
}
