package com.jarvis.assistant.power

enum class PowerCommand { STATUS, TEMPERATURE, OPEN, DISMISS, SNOOZE }
object PowerCommands {
    fun parse(input:String,batteryContext:Boolean=false):PowerCommand? {
        val n=com.jarvis.assistant.neural.AppMatching.normalize(input).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ").removePrefix("please ")
        return when(n){
            "battery","battery level","what is my battery","what is my battery level","what s my battery","what s up with my battery","whats up with my battery",
            "is my phone charging","what s my charging percentage","what is my charging percentage","how long until full","how long until my phone is fully charged",
            "how much longer until my phone is charged","how much longer until my phone is fully charged","how much longer until it s full","when will my phone be charged","when will my phone be fully charged","when will my phone reach one hundred percent",
            "what time will charging finish","is my phone fully charged","bateria","nivel de bateria","esta cargando mi telefono","cuanto falta para cargar","cuando termina la carga"->PowerCommand.STATUS
            "how much time is left","when will it be full"->if(batteryContext)PowerCommand.STATUS else null
            "what s my battery temperature","what is my battery temperature","battery temperature","temperatura de la bateria"->PowerCommand.TEMPERATURE
            "show charging details","take me to the power core","open power core","power core","open the full battery interface","take me to the charging screen","show battery settings","abre el nucleo de energia","muestra detalles de carga"->PowerCommand.OPEN
            "stop charging reminders","stop full charge reminders","stop the full battery alert","dismiss charging reminders","stop full battery alerts","deja de recordar la carga","deten los avisos de carga"->PowerCommand.DISMISS
            "remind me later","remind me again later","snooze charging reminders","recuerdame mas tarde"->if(batteryContext || n.contains("charging") || n.contains("carga"))PowerCommand.SNOOZE else null
            else->null
        }
    }
}
