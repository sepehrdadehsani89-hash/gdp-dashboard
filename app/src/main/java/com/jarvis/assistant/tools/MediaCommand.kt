package com.jarvis.assistant.tools

import android.view.KeyEvent

/** Only explicit user commands; no model-provided phone instructions. */
object MediaCommand {
    fun parse(text:String):String? = when(ToolPolicy.normalize(text)){
        "play music","resume music","reproduce música","reproduce musica","continuar música","موسیقی را پخش کن","ادامه موسیقی"->"play"
        "pause music","pause playback","pausa la música","pausa la musica","موسیقی را متوقف کن","مکث موسیقی"->"pause"
        "next track","next song","siguiente pista","siguiente canción","آهنگ بعدی"->"next"
        "previous track","previous song","pista anterior","canción anterior","آهنگ قبلی"->"previous"
        else->null
    }
    fun key(action:String):Int=when(action){"play"->KeyEvent.KEYCODE_MEDIA_PLAY;"pause"->KeyEvent.KEYCODE_MEDIA_PAUSE;"next"->KeyEvent.KEYCODE_MEDIA_NEXT;"previous"->KeyEvent.KEYCODE_MEDIA_PREVIOUS;else->error("Unsupported media request")}
}
