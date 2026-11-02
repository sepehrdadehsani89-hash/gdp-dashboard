package com.jarvis.assistant.infinity

import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.neural.DiscoveredApp

data class Accessory(val address:String,val name:String,val type:String,val paired:Boolean,val connected:Boolean=false,val rssi:Int?=null)
object DevicePolicy {
    fun informationalRequest(input:String):Boolean {
        val n=AppMatching.normalize(input)
        return Regex("^(?:what is|whats|what s|define|describe|explain|que es|explica) (?:bluetooth|wi fi|wifi|battery health|la salud de la bateria)$").matches(n) ||
            Regex("^(?:how to |how do i |how can i |how does |como |چگونه |چطور )").containsMatchIn(n) ||
            Regex("(?:چیست|یعنی چه)$").containsMatchIn(n)
    }
    fun normalizedName(text:String):String {
        val numbers=mapOf("zero" to "0","one" to "1","two" to "2","three" to "3","four" to "4","five" to "5","six" to "6","seven" to "7","eight" to "8","nine" to "9","cero" to "0","uno" to "1","dos" to "2","tres" to "3","cuatro" to "4","cinco" to "5","seis" to "6","siete" to "7","ocho" to "8","nueve" to "9","یک" to "1","دو" to "2","سه" to "3","چهار" to "4","پنج" to "5","شش" to "6","هفت" to "7","هشت" to "8","نه" to "9")
        return AppMatching.normalize(text).split(' ').joinToString(" "){word->numbers[word] ?: word.map{c->Character.digit(c,10).takeIf{it>=0}?.let{('0'.code+it).toChar()} ?: c}.joinToString("")}
    }
    fun matches(query:String,devices:List<Accessory>,aliases:Map<String,String> = emptyMap()):List<Accessory> {
        val apps=devices.map{d->DiscoveredApp(d.address,normalizedName(d.name),aliases.filterValues{it==d.address}.keys.map(::normalizedName))}
        return AppMatching.match(normalizedName(query),apps).mapNotNull{match->devices.firstOrNull{it.address==match.app.packageId}}
    }
    fun reference(query:String,devices:List<Accessory>):Accessory? {
        val n=AppMatching.normalize(query)
        val index=when(n){"first","the first one","primero","el primero","اولی"->0;"second","the second one","segundo","el segundo","دومی"->1;"third","the third one","tercero","سومی"->2;else->return null}
        return devices.getOrNull(index)
    }
    fun usableSsid(raw:String?):String?=raw?.trim()?.removeSurrounding("\"")?.takeIf{it.isNotBlank() && !it.equals("<unknown ssid>",true) && it!="0x"}
    fun wifiRssi(raw:Int?):Int?=raw?.takeIf{it in -126..-1} // Android's -127 sentinel is not a measurement.
    fun throughputMbps(bytes:Long,elapsedNanos:Long):Double {require(bytes>0 && elapsedNanos>0);return bytes*8_000.0/elapsedNanos}
    fun knownProperty(value:Long):Long?=value.takeUnless{it==Long.MIN_VALUE || it==Int.MIN_VALUE.toLong()}
}
