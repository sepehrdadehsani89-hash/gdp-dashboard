package com.jarvis.assistant.rebirth

object NativeNavigation {
    enum class Destination { CONTENT, PHOTOS, ALIASES, MEMORY, CLOCK, BLUETOOTH, PHANTOM, SETTINGS }
    fun destination(text:String):Destination? {
        val t=com.jarvis.assistant.neural.AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return when(t){
            "open settings","open android settings","abre ajustes","abre configuracion"->Destination.SETTINGS
            "open my files","open my documents","open personal content"->Destination.CONTENT
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
