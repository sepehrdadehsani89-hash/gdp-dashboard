package com.jarvis.assistant.local

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/** Shared by the phone downloader and filesystem tests. A partial never counts as installed. */
object ModelFileTransfer {
    suspend fun copyVerified(input:InputStream,temporary:File,descriptor:ModelDownloadPolicy.Descriptor,initial:Long,onProgress:(Float)->Unit,cancelled:()->Boolean = {false}):String {
        require(initial in 0..descriptor.size && (initial==0L || temporary.length()==initial))
        val digest=MessageDigest.getInstance("SHA-256")
        fun checkCancelled(){if(cancelled())throw kotlinx.coroutines.CancellationException("Model download cancelled")}
        if(initial>0)temporary.inputStream().use{old->val bytes=ByteArray(131072);while(true){currentCoroutineContext().ensureActive();checkCancelled();val n=old.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
        var total=initial;var percent=-1
        FileOutputStream(temporary,initial>0).use{output->
            val buffer=ByteArray(131072)
            while(true){
                currentCoroutineContext().ensureActive();checkCancelled()
                val count=input.read(buffer);if(count<0)break
                total+=count;require(total<=descriptor.size){"Model response exceeded verified size."}
                output.write(buffer,0,count);digest.update(buffer,0,count)
                val next=(total*100/descriptor.size).toInt()
                if(next!=percent){percent=next;onProgress((total.toFloat()/descriptor.size).coerceIn(0f,.99f))}
            }
            output.flush();output.fd.sync()
        }
        val hash=digest.digest().joinToString(""){"%02x".format(it)}
        ModelDownloadPolicy.verifyDownloaded(descriptor,total,hash)
        currentCoroutineContext().ensureActive();checkCancelled()
        return hash
    }
}
