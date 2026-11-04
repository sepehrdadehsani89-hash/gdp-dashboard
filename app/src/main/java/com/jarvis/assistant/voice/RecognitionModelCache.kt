package com.jarvis.assistant.voice

import org.vosk.Model
import java.io.File

/** One shared immutable Vosk model; recognizers own independent bounded sessions. */
object RecognitionModelCache {
    private var path:String?=null
    private var model:Model?=null
    private var users=0
    class Reference internal constructor(val model:Model):AutoCloseable {
        private var closed=false
        @Synchronized override fun close(){if(!closed){closed=true;releaseReference()}}
    }
    @Synchronized fun acquire(directory:File):Reference {
        val wanted=directory.canonicalPath
        if(path!=wanted){check(users==0){"RECOGNIZER_BUSY: another recognition model is releasing."};model?.close();model=null;path=null}
        if(model==null){model=Model(wanted);path=wanted}
        users++;return Reference(checkNotNull(model))
    }
    @Synchronized private fun releaseReference(){users=(users-1).coerceAtLeast(0)}
    @Synchronized fun releaseIdle(){if(users==0){model?.close();model=null;path=null}}
}
