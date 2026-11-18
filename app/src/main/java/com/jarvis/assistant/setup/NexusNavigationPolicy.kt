package com.jarvis.assistant.setup

import com.jarvis.assistant.neural.AppMatching

enum class NexusDestination { GOOGLE_CLOUD,GOOGLE_SERVICES,WALLPAPER_STUDIO,COMPUTERS,SETUP,SYSTEM_MODES,COMMAND_LAB }
/** Opening a reviewed screen is USER_ACTION_REQUIRED, never a completed setting or remote action. */
object NexusNavigationPolicy {
    fun parse(raw:String):NexusDestination?=when(AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")){
        "open google cloud configuration","open cloud configuration","configure google cloud"->NexusDestination.GOOGLE_CLOUD
        "open google connected services","manage google permissions","manage google accounts"->NexusDestination.GOOGLE_SERVICES
        "open wallpaper studio","open color studio","open widget library","edit my wallpaper","abre el estudio de fondos","استودیوی تصویر زمینه را باز کن"->NexusDestination.WALLPAPER_STUDIO
        "open paired computers","scan computer qr","open pairing diagnostics","pair my computer"->NexusDestination.COMPUTERS
        "open complete setup wizard","reconfigure jarvis","open nexus setup"->NexusDestination.SETUP
        "open system modes","open silent mode","open deep shutdown"->NexusDestination.SYSTEM_MODES
        "open command test lab","test voice commands"->NexusDestination.COMMAND_LAB
        else->null
    }
}
