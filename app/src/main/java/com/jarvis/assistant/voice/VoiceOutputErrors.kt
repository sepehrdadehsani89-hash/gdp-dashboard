package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.omnisystem.ModuleServiceFailure

/** Safe, actual output categories; never expose provider bodies, tokens, URLs or private text. */
object VoiceOutputErrors {
    fun message(error:Exception,language:AppLanguage):String {
        val es=language==AppLanguage.ES
        if(error is ModuleServiceFailure)return if(es)"Fish Audio rechazó la solicitud (HTTP ${error.code}). Revisa acceso a la voz, modelo, clave y cuota. El texto sigue disponible." else "Fish Audio rejected the voice request (HTTP ${error.code}). Check voice access, model, key and quota. Text remains available."
        return when(error.message){
            "Voice reply paused by Silent or Do Not Disturb mode."->if(es)"La voz está pausada por Silencio o No molestar. El texto sigue disponible." else "Voice reply paused by Silent or Do Not Disturb mode. Text remains available."
            "Audio is in use by another app.","Voice playback was interrupted by another audio session."->if(es)"Otra sesión de audio interrumpió la voz. El texto sigue disponible." else "Another audio session interrupted voice playback. Text remains available."
            "Voice playback timed out. Listening can resume; no substitute voice was used.","Voice audio could not start. Listening can resume; check Output voice and retry."->if(es)"El audio de voz agotó el tiempo de espera. Revisa salida de voz y audio. El texto sigue disponible." else "Voice playback timed out. Check Output voice and the audio route. Text remains available."
            else->if(es)"No se pudo reproducir la voz configurada. Revisa salida de voz, conexión, acceso y cuota. El texto sigue disponible; no se sustituyó otra voz." else "The configured voice could not play. Check Output voice, connection, access and quota. Text remains available; no substitute voice was used."
        }
    }
}
