package com.jarvis.assistant.neural

/** Names augment only packages Android actually exposes. Never invent an installed application. */
object AppDiscoveryPolicy {
    private val common=mapOf(
        "com.whatsapp" to listOf("WhatsApp","واتساپ","واتس اپ"),
        "com.whatsapp.w4b" to listOf("WhatsApp Business"),
        "com.google.android.gm" to listOf("Gmail"),
        "com.instagram.android" to listOf("Instagram","اینستاگرام"),
        "com.android.chrome" to listOf("Chrome","Google Chrome"),
        "com.google.android.youtube" to listOf("YouTube","You Tube","یوتیوب"),
        "com.google.android.apps.maps" to listOf("Maps","Google Maps"),
        "com.spotify.music" to listOf("Spotify","اسپاتیفای"),
        "com.android.camera" to listOf("Camera","Cámara","دوربین"),
        "com.google.android.GoogleCamera" to listOf("Camera","Google Camera"),
        "com.miui.calculator" to listOf("Calculator","Calculadora","ماشین حساب"),
        "com.google.android.calculator" to listOf("Calculator","Calculadora","ماشین حساب"),
        "com.android.calculator2" to listOf("Calculator"),
        "org.telegram.messenger" to listOf("Telegram","تلگرام")
    )
    fun names(packageId:String)=common[packageId].orEmpty()
    fun eligible(applicationEnabled:Boolean,activityEnabled:Boolean,exported:Boolean)=applicationEnabled && activityEnabled && exported
}
