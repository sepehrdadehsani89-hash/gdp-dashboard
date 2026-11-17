package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage

/** Authored summaries for native proposals. No screen text, payload, name or URL is accepted. */
object NativeSpeechSummary {
    fun confirmation(kind:String,language:AppLanguage):String? {
        val pair=when(kind){
            "dial_contact"->"I found your requested contact. Confirm opening Android's dialer, or cancel. You still press Call there." to "Encontré el contacto solicitado. Confirma abrir el marcador Android o cancela. Debes pulsar Llamar allí."
            "open_app"->"I resolved the requested app. Confirm opening it, or cancel." to "Encontré la aplicación solicitada. Confirma abrirla o cancela."
            "app_alias"->"Confirm saving the requested app nickname locally, or cancel." to "Confirma guardar el alias de aplicación localmente o cancela."
            "open_url"->"Confirm opening the requested link with Android, or cancel. Any message draft still requires manual review and sending." to "Confirma abrir el enlace con Android o cancela. Debes revisar y enviar cualquier borrador manualmente."
            "live_control"->"Confirm activating the selected screen control, or cancel. It has not been activated yet." to "Confirma activar el control seleccionado o cancela. Aún no se activó."
            "system_panel"->"Confirm opening the requested Android settings panel, or cancel. No setting has changed." to "Confirma abrir el panel Android o cancela. No se cambió ningún ajuste."
            "infinity_controls"->"Confirm opening device controls, or cancel. Optional tests and permissions require your action there." to "Confirma abrir los controles o cancela. Las pruebas y permisos requieren tu acción allí."
            "media_control"->"Confirm sending your requested media control to Android, or cancel." to "Confirma enviar el control multimedia solicitado a Android o cancela."
            "nav_home"->"Confirm returning to the Android home screen, or cancel." to "Confirma volver al inicio de Android o cancela."
            "nav_back"->"Confirm asking Android to go back, or cancel." to "Confirma solicitar Atrás a Android o cancela."
            "set_timer"->"Confirm opening the requested timer in Android's Clock app, or cancel. Review and save it there." to "Confirma abrir el temporizador solicitado en Reloj o cancela. Revísalo y guárdalo allí."
            "set_alarm"->"Confirm opening the requested alarm in Android's Clock app, or cancel. Review the time and save it there." to "Confirma abrir la alarma solicitada en Reloj o cancela. Revisa la hora y guárdala allí."
            "reminder_draft"->"Confirm opening your calendar draft, or cancel. Review and save it there; no reminder has been saved." to "Confirma abrir el borrador de calendario o cancela. Debes revisarlo y guardarlo; no se guardó un recordatorio."
            else->return null
        }
        return if(language==AppLanguage.ES)pair.second else pair.first
    }
    // Only these legacy executor branches return authored status without private payload values.
    fun publicResult(kind:String)=kind in setOf("dial_contact","app_alias","open_url","live_control","system_panel","infinity_controls","media_control","nav_home","nav_back","set_timer","set_alarm","reminder_draft")
}
