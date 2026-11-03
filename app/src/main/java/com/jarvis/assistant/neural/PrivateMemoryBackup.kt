package com.jarvis.assistant.neural

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.security.MessageDigest

data class RestoredDocument(val id:String,val title:String,val text:String,val source:String,val version:String,val publication:String,val license:String)
data class RestoredMemory(val text:String,val createdAt:Long)
data class PrivateMemoryBackup(val documents:List<RestoredDocument>,val tasks:List<String>,val memories:List<RestoredMemory>,val aliases:Map<String,String>) {
    companion object {
        const val MAX_BYTES=32*1024*1024
        fun read(input:InputStream):PrivateMemoryBackup {
            val output=java.io.ByteArrayOutputStream()
            val buffer=ByteArray(8192)
            while(true){
                val count=input.read(buffer)
                if(count<0)break
                require(output.size().toLong()+count<=MAX_BYTES){"Backup exceeds the 32 MiB limit."}
                output.write(buffer,0,count)
            }
            val bytes=output.toByteArray()
            return parse(bytes.toString(Charsets.UTF_8))
        }
        fun parse(text:String):PrivateMemoryBackup {
            require(text.toByteArray(Charsets.UTF_8).size<=MAX_BYTES){"Backup exceeds the 32 MiB limit."}
            val root=JSONObject(text)
            require(root.opt("format")=="jarvis-neural-export-v1"){"Unsupported backup format."}
            fun array(key:String,max:Int):JSONArray {
                val value=root.get(key)
                require(value is JSONArray && value.length()<=max){"Invalid backup collection."}
                return value
            }
            fun field(obj:JSONObject,key:String,max:Int,blank:Boolean=false):String {
                val value=obj.get(key)
                require(value is String && value.length<=max && (blank || value.isNotBlank()) && '\u0000' !in value){"Invalid backup field."}
                return value
            }
            val ids=mutableSetOf<String>()
            val docs=array("documents",200).let{a->(0 until a.length()).map{i->
                val d=a.getJSONObject(i)
                val originalId=field(d,"id",200)
                require(ids.add(originalId)){"Duplicate document identifier."}
                require(field(d,"kind",40) in setOf("USER_DOCUMENT","USER_NOTE","OFFLINE_KNOWLEDGE","CONVERSATION_SUMMARY")){"Invalid document kind."}
                val title=field(d,"title",160)
                // Old exports append a newline after each 700-character passage.
                val body=field(d,"text",150_220)
                val source=field(d,"source",500,true)
                val version=field(d,"version",40,true)
                val publication=field(d,"publication",80,true)
                val license=field(d,"license",100,true)
                val identity=JSONArray(listOf(originalId,title,body,source,version,publication,license)).toString()
                val digest=MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString(""){"%02x".format(it)}
                RestoredDocument("import:$digest",title,body,source,version,publication,license)
            }}
            val tasks=array("tasks",30).let{a->(0 until a.length()).map{i->
                val value=a.get(i);require(value is String && value.isNotBlank() && value.length<=300 && '\u0000' !in value){"Invalid task."};value.trim()
            }.distinct()}
            val memories=array("approved_memories",1000).let{a->(0 until a.length()).map{i->
                val m=a.getJSONObject(i);val value=field(m,"text",4000).trim();val time=m.get("created_at")
                require((time is Long || time is Int) && (time as Number).toLong()>=0){"Invalid memory timestamp."}
                RestoredMemory(value,(time as Number).toLong())
            }.distinctBy{it.text}}
            val aliases=root.getJSONObject("app_aliases")
            require(aliases.length()<=100){"Too many app aliases."}
            val names=mutableSetOf<String>()
            val mapped=aliases.keys().asSequence().associate{name->
                val normalized=AppMatching.normalize(name)
                require(normalized.length in 2..80 && names.add(normalized)){"Invalid or duplicate app alias."}
                val pkg=aliases.get(name)
                require(pkg is String && pkg.length<=255 && pkg.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))){"Invalid app package."}
                normalized to pkg
            }
            return PrivateMemoryBackup(docs,tasks,memories,mapped)
        }
    }
}
