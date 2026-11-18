package com.jarvis.assistant.companion

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

/** Local bounded pixels only; used by gallery import, with no network or external service. */
object QrImageDecoder {
    fun decode(width:Int,height:Int,pixels:IntArray):String {
        require(width in 1..2048 && height in 1..2048 && width.toLong()*height==pixels.size.toLong())
        val opaque=IntArray(pixels.size){i->val p=pixels[i];val a=p ushr 24
            fun blend(v:Int)=(v*a+255*(255-a))/255
            (0xff shl 24) or (blend(p ushr 16 and 255) shl 16) or (blend(p ushr 8 and 255) shl 8) or blend(p and 255)}
        val source=RGBLuminanceSource(width,height,opaque)
        val reader=MultiFormatReader();val hints=mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER to true)
        for(candidate in listOf(source,source.invert()))try{return reader.decode(BinaryBitmap(HybridBinarizer(candidate)),hints).text} catch(_:NotFoundException){} catch(_:ChecksumException){} catch(_:FormatException){} finally{reader.reset()}
        throw CompanionFailure("NO_QR_IN_IMAGE")
    }
}
