package com.jarvis.assistant.companion

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.*
import org.junit.Test

class QrImageDecoderTest {
    private val content="JARVIS-PAIR-1:"+"actual-local-fixture-".repeat(15)
    private fun pixels(text:String=content,size:Int=400):IntArray{val m=MultiFormatWriter().encode(text,BarcodeFormat.QR_CODE,size,size,mapOf(com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"));return IntArray(size*size){i->if(m[i%size,i/size])0xff000000.toInt()else 0xffffffff.toInt()}}
    private fun rotate(p:IntArray,size:Int)=IntArray(p.size){i->p[(size-1-i%size)*size+i/size]}
    @Test fun decodesFullPayload(){assertEquals(content,QrImageDecoder.decode(400,400,pixels()))}
    @Test fun payloadIsNotTruncated(){val text="private-".repeat(120);assertEquals(text,QrImageDecoder.decode(600,600,pixels(text,600)))}
    @Test fun decodesQuarterTurn(){assertEquals(content,QrImageDecoder.decode(400,400,rotate(pixels(),400)))}
    @Test fun decodesHalfTurn(){assertEquals(content,QrImageDecoder.decode(400,400,rotate(rotate(pixels(),400),400)))}
    @Test fun decodesThreeQuarterTurn(){assertEquals(content,QrImageDecoder.decode(400,400,rotate(rotate(rotate(pixels(),400),400),400)))}
    @Test fun decodesInvertedCode(){val p=pixels().map{it xor 0x00ffffff}.toIntArray();assertEquals(content,QrImageDecoder.decode(400,400,p))}
    @Test fun transparentBackgroundIsCompositedWhite(){val p=pixels().map{if(it==-1)0 else it}.toIntArray();assertEquals(content,QrImageDecoder.decode(400,400,p))}
    @Test fun actualSmallCodeCanBeRead(){assertEquals("short",QrImageDecoder.decode(120,120,pixels("short",120)))}
    @Test fun largeCodeWithinBudgetCanBeRead(){assertEquals(content,QrImageDecoder.decode(1024,1024,pixels(content,1024)))}
    @Test fun unicodeDataSurvivesLocalDecode(){val text="Jarvis señor رایانه";assertEquals(text,QrImageDecoder.decode(400,400,pixels(text)))}
    @Test fun nonInvitationQrIsDecodedThenRejectedByParser(){val raw=QrImageDecoder.decode(400,400,pixels("https://example.com"));assertThrows(InvitationFailure::class.java){PairingInvitation.parse(raw,1700000000000)}}
    @Test fun whiteImageIsNotAConnection(){assertThrows(CompanionFailure::class.java){QrImageDecoder.decode(100,100,IntArray(10000){-1})}}
    @Test fun blackImageIsNotAConnection(){assertThrows(CompanionFailure::class.java){QrImageDecoder.decode(100,100,IntArray(10000){0xff000000.toInt()})}}
    @Test fun transparentImageIsNotAConnection(){assertThrows(CompanionFailure::class.java){QrImageDecoder.decode(100,100,IntArray(10000))}}
    @Test fun zeroWidthRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(0,100,IntArray(0))}}
    @Test fun zeroHeightRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(100,0,IntArray(0))}}
    @Test fun negativeDimensionRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(-1,100,IntArray(0))}}
    @Test fun tooWideRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(2049,1,IntArray(2049))}}
    @Test fun tooTallRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(1,2049,IntArray(2049))}}
    @Test fun incompletePixelBufferRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(100,100,IntArray(9999))}}
    @Test fun extraPixelsRejected(){assertThrows(IllegalArgumentException::class.java){QrImageDecoder.decode(100,100,IntArray(10001))}}
    @Test fun callerPixelsRemainUnchanged(){val p=pixels();val before=p.clone();QrImageDecoder.decode(400,400,p);assertArrayEquals(before,p)}
}
