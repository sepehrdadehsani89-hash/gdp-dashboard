package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.jarvis.assistant.companion.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Actual bundled ML Kit bitmap decoding, NOT a test of a physical camera or Windows pairing. */
@RunWith(Parameterized::class)
class BundledQrDecoderInstrumentedTest(private val scenario:String){
    @Test fun actualLocalDecoderAndInvitationBoundary(){
        val now=System.currentTimeMillis()
        val j=JSONObject().put("schema_version",1).put("url","https://192.168.1.8:8765").put("certificate_sha256","a".repeat(64)).put("device_id",UUID.randomUUID().toString()).put("pairing_id",UUID.randomUUID().toString()).put("code","C".repeat(43)).put("expires_at",now+120000).put("device_name","Fixture computer")
        var error:String?=null;var raw:String?=null;var rotation=0;var size=600;var noQr=false;var format=BarcodeFormat.QR_CODE
        when(scenario){
            "expired"->{j.put("expires_at",now-1);error="INVITATION_EXPIRED"}
            "unsupported"->{j.put("schema_version",2);error="UNSUPPORTED_PROTOCOL"}
            "missing-secret"->{j.remove("code");error="INVALID_INVITATION"}
            "bad-pin"->{j.put("certificate_sha256","wrong");error="INVALID_INVITATION"}
            "public-host"->{j.put("url","https://8.8.8.8:8765");error="INVALID_INVITATION"}
            "full-name"->j.put("device_name","رایانه personal computer señor")
            "rotate90"->rotation=90;"rotate180"->rotation=180;"rotate270"->rotation=270
            "small"->size=400;"large"->size=1000
            "blank-white","blank-black"->noQr=true
            "code128"->{raw="1234567890";format=BarcodeFormat.CODE_128;noQr=true}
            "malformed"->{raw="JARVIS-PAIR-1:"+"+".repeat(100);error="INVALID_INVITATION"}
            "ordinary-url"->{raw="https://192.168.1.8:8765";error="INVALID_INVITATION"}
            "filename"->{raw="pairing-qr.svg";error="INVALID_INVITATION"}
        }
        var expected=raw ?: PairingInvitation.PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(j.toString().toByteArray())
        if(scenario=="wrapped")expected=PairingInvitation.PREFIX+expected.removePrefix(PairingInvitation.PREFIX).chunked(60).joinToString("\n")
        if(scenario=="outer-whitespace")expected=" \n$expected\r\n "
        val b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
        if(scenario.startsWith("blank-"))b.eraseColor(if(scenario=="blank-white")Color.WHITE else Color.BLACK)
        else{val m=MultiFormatWriter().encode(expected,format,size,size,mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"));val pixels=IntArray(size*size){if(m[it%size,it/size])Color.BLACK else Color.WHITE};b.setPixels(pixels,0,size,0,0,size,size)}
        val scanner=BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        try{
            val result=Tasks.await(scanner.process(InputImage.fromBitmap(b,rotation)),20,TimeUnit.SECONDS)
            if(noQr){assertTrue(result.isEmpty());return}
            val decoded=result.firstOrNull{it.format==Barcode.FORMAT_QR_CODE}?.rawValue
            assertEquals(expected,decoded)
            if(error!=null){val e=assertThrows(InvitationFailure::class.java){PairingInvitation.parse(requireNotNull(decoded),now)};assertEquals(error,e.code)}
            else{val parsed=PairingInvitation.parse(requireNotNull(decoded),now);assertEquals(j.getString("device_id"),parsed.deviceId);assertEquals(j.getString("pairing_id"),parsed.pairingId);assertEquals(j.getString("code"),parsed.code)}
        }finally{scanner.close();b.recycle()}
    }
    companion object{@JvmStatic @Parameterized.Parameters(name="{0}")fun cases()=listOf("valid","wrapped","outer-whitespace","expired","unsupported","missing-secret","bad-pin","public-host","full-name","rotate90","rotate180","rotate270","small","large","blank-white","blank-black","code128","malformed","ordinary-url","filename")}
}
