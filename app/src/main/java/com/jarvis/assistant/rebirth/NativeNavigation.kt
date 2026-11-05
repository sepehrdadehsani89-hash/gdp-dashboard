package com.jarvis.assistant.rebirth

object NativeNavigation {
    enum class Destination { CONTENT, PHOTOS, ALIASES, MEMORY, CLOCK, BLUETOOTH, PHANTOM, SETTINGS, VOICE, WIDGETS }
    fun destination(text:String):Destination? {
        val t=com.jarvis.assistant.neural.AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return when(t){
            "open my widgets","open widgets","show my widgets","show all widgets","open widget center","access all my widgets","abre mis widgets","muestra mis widgets"->Destination.WIDGETS
            "open settings","open android settings","abre ajustes","abre configuracion"->Destination.SETTINGS
            "can you recognize these voices","can you recognize my voice","recognize my voice","learn my voice","remember my voice","enroll my voice","hold this as your one and only owner","configura mi voz","reconoce mi voz"->Destination.VOICE
            "open my files","open my documents","open personal content","connect my files","connect all my files","connect to all my files"->Destination.CONTENT
            "open my photos","show my photos"->Destination.PHOTOS
            "show available apps","show my app aliases"->Destination.ALIASES
            "take me to my memory","open my memory","show my memory cards"->Destination.MEMORY
            "open the world clock","open world clock","show world clocks"->Destination.CLOCK
            "open the bluetooth radar","open bluetooth radar"->Destination.BLUETOOTH
            "open phantom settings"->Destination.PHANTOM
            else->null
        }
    }
}
