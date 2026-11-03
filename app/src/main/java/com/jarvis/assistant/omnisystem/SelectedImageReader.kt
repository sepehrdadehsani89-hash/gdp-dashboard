package com.jarvis.assistant.omnisystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.CancellationSignal
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

data class SelectedVisionImage(val uri:Uri,val preview:Bitmap,val jpeg:ByteArray)
object SelectedImageReader {
    suspend fun read(context:Context,uri:Uri):SelectedVisionImage=withTimeout(20_000){withContext(Dispatchers.IO){
        require(uri.scheme=="content"){"Select an image through Android's picker."}
        val signal=CancellationSignal();val stream=AtomicReference<InputStream?>()
        val watcher=CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default,start=CoroutineStart.UNDISPATCHED){
            try{awaitCancellation()}finally{signal.cancel();runCatching{stream.get()?.close()}}
        }
        var raw:ByteArray?=null
        try {
            context.contentResolver.openAssetFileDescriptor(uri,"r",signal).use{descriptor->
                check(descriptor!=null){"Image unavailable."}
                descriptor.createInputStream().use{input->
                    stream.set(input);val buffer=ByteArray(8192);val output=ByteArrayOutputStream()
                    while(true){ensureActive();val n=input.read(buffer);if(n<0)break;require(output.size().toLong()+n<=12_000_000){"Image file exceeds 12 MB."};output.write(buffer,0,n)}
                    raw=output.toByteArray()
                }
            }
            val bytes=raw ?: error("Image unavailable.")
            val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            require(options.outWidth>0 && options.outHeight>0 && options.outWidth.toLong()*options.outHeight<=40_000_000){"Unsupported image dimensions."}
            var sample=1;while(maxOf(options.outWidth,options.outHeight)/sample>1024)sample*=2
            options.inJustDecodeBounds=false;options.inSampleSize=sample
            val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Unsupported image.")
            val transform=runCatching{ExifInterface(ByteArrayInputStream(bytes)).let{exif->Matrix().apply{if(exif.isFlipped)postScale(-1f,1f);postRotate(exif.rotationDegrees.toFloat())}}}.getOrElse{Matrix()}
            val preview=if(transform.isIdentity)decoded else Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,transform,true).also{if(it!==decoded)decoded.recycle()}
            val output=ByteArrayOutputStream();check(preview.compress(Bitmap.CompressFormat.JPEG,85,output))
            val jpeg=output.toByteArray();require(jpeg.isNotEmpty() && jpeg.size<=1_500_000)
            ensureActive();SelectedVisionImage(uri,preview,jpeg)
        } finally {raw?.fill(0);watcher.cancel();runCatching{stream.getAndSet(null)?.close()}}
    }}
}
