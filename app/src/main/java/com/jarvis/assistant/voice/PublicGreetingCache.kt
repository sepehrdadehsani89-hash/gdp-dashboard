package com.jarvis.assistant.voice

import java.io.File
import java.security.MessageDigest

/** Only fixed public courtesy clips. No conversation, user names or private results are cached. */
class PublicGreetingCache(private val directory:File,private val now:()->Long=System::currentTimeMillis){
    companion object {
        fun eligible(text:String)=com.jarvis.assistant.core.AppLanguage.supported.any{language->(0..5).any{index->text==com.jarvis.assistant.pickup.PickupGreetings.text(index,language)}} || text in setOf("Hello sir, how can I help you?","Hola, señor. ¿Cómo puedo ayudarte?",com.jarvis.assistant.call.AiCallPolicy.greeting(false),com.jarvis.assistant.call.AiCallPolicy.greeting(true)) || listOf("0","1","2").any{itId->text==com.jarvis.assistant.checkin.CallMePolicy.invitation(itId,false) || text==com.jarvis.assistant.checkin.CallMePolicy.invitation(itId,true)}
    }
    private fun file(c:CustomVoiceConfig,text:String):File {
        val key=listOf(c.id,c.model,c.key,text).joinToString("\u0000")
        val digest=MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString(""){"%02x".format(it)}
        return File(directory,"$digest.mp3")
    }
    @Synchronized fun read(c:CustomVoiceConfig,text:String):ByteArray? {
        if(!eligible(text) || !c.enabled)return null
        val f=file(c,text)
        return runCatching{if(f.length() in 1..4_000_000 && now()-f.lastModified() in 0..604_800_000L)f.readBytes()else null}.getOrNull()
    }
    @Synchronized fun write(c:CustomVoiceConfig,text:String,audio:ByteArray){
        if(!eligible(text) || !c.enabled || audio.isEmpty() || audio.size>4_000_000)return
        runCatching{
            check(directory.isDirectory || directory.mkdirs())
            val target=file(c,text);val temporary=File.createTempFile("greeting-",".part",directory)
            try{temporary.writeBytes(audio);check(temporary.renameTo(target))}finally{temporary.delete()}
            directory.listFiles()?.filter{it.extension=="mp3"}?.sortedByDescending{it.lastModified()}?.drop(4)?.forEach{it.delete()}
        }
    }
    @Synchronized fun clear(){directory.listFiles()?.forEach{it.delete()}}
}
