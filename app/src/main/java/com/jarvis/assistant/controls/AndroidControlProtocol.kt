package com.jarvis.assistant.controls

import com.jarvis.assistant.neural.AppMatching

enum class ControlResultStatus { SUCCESS, USER_ACTION_REQUIRED, PERMISSION_REQUIRED, UNSUPPORTED, FAILED, TIMEOUT }
enum class ControlSupport { FULLY_SUPPORTED, REQUIRES_USER_CONFIRMATION, SETTINGS_SHORTCUT_ONLY, UNSUPPORTED_OR_DEVICE_DEPENDENT }
enum class AndroidControlId {
    TORCH_ON,TORCH_OFF,WIFI_ON,WIFI_OFF,WIFI_SETTINGS,WIFI_STATUS,BLUETOOTH_ON,BLUETOOTH_OFF,BLUETOOTH_SETTINGS,BLUETOOTH_STATUS,HOTSPOT,AIRPLANE,
    VOLUME_UP,VOLUME_DOWN,VOLUME_SET,VOLUME_STATUS,MEDIA_MUTE,MEDIA_UNMUTE,RINGER_SILENT,RINGER_NORMAL,VIBRATE,DND_ON,DND_OFF,DND_STATUS,
    LOCK,UNLOCK,BATTERY,POWER_SAVER,DISPLAY,SOUND,ROTATION,PORTRAIT,LANDSCAPE,DARK_MODE,READING_MODE,CAST,SHARE,QUICK_SHARE,MI_SHARE,TV_REMOTE,FOCUS,BEDTIME,WALLET,RECORD_START,RECORD_STOP
}
data class AndroidControlCommand(val id:AndroidControlId,val percent:Int?=null){
    fun validated():AndroidControlCommand {require(if(id==AndroidControlId.VOLUME_SET)percent!=null && percent in 0..100 else percent==null){"Volume must be 0–100 percent; this command accepts no other parameter."};return this}
}
data class ControlOutcome(val command:AndroidControlCommand,val status:ControlResultStatus,val detail:String,val observed:String?=null,val at:Long=System.currentTimeMillis())
data class ControlCapability(val id:AndroidControlId,val name:String,val support:ControlSupport,val setup:String,val permission:String="None beyond normal application access")
data class ControlTrace(val input:String,val origin:String,val command:AndroidControlCommand,val permission:String,val actualOperation:String,val outcome:ControlOutcome)

/** Deterministic commands only. A model response is never parsed or authorized as a device action. */
object AndroidControlParser {
    private val aliases=mapOf(
        AndroidControlId.TORCH_ON to listOf("turn on flashlight","turn on my flashlight","turn on the flashlight","enable the torch","enable torch","flashlight on","turn the flashlight on","enciende la linterna","چراغ قوه را روشن کن"),
        AndroidControlId.TORCH_OFF to listOf("turn off flashlight","turn off my flashlight","turn off the flashlight","disable the torch","disable torch","flashlight off","turn the flashlight off","apaga la linterna","چراغ قوه را خاموش کن"),
        AndroidControlId.WIFI_ON to listOf("turn on wi fi","turn on wifi","enable wifi","help me turn on wi fi"),
        AndroidControlId.WIFI_SETTINGS to listOf("show wi fi settings","show wifi settings","open wifi settings","open wi fi settings"),
        AndroidControlId.WIFI_OFF to listOf("turn off wi fi","turn off wifi","disable wifi","help me turn off wi fi"),
        AndroidControlId.WIFI_STATUS to listOf("is wifi on","is wi fi on","check wifi","check wi fi","wifi status"),
        AndroidControlId.BLUETOOTH_ON to listOf("turn on bluetooth","enable bluetooth","activa bluetooth"),
        AndroidControlId.BLUETOOTH_OFF to listOf("turn off bluetooth","disable bluetooth"),
        AndroidControlId.BLUETOOTH_SETTINGS to listOf("open bluetooth settings","show bluetooth settings"),
        AndroidControlId.BLUETOOTH_STATUS to listOf("is bluetooth on","check bluetooth","bluetooth status"),
        AndroidControlId.HOTSPOT to listOf("open hotspot settings","turn on hotspot","turn off hotspot","enable hotspot","disable hotspot"),
        AndroidControlId.AIRPLANE to listOf("open airplane mode settings","turn on airplane mode","turn off airplane mode","help me turn on airplane mode","help me turn off airplane mode"),
        AndroidControlId.VOLUME_UP to listOf("increase volume","increase the volume","turn up the volume","raise the volume","louder"),
        AndroidControlId.VOLUME_DOWN to listOf("lower the volume","decrease volume","decrease the volume","turn down the volume","quieter"),
        AndroidControlId.VOLUME_STATUS to listOf("check volume","what is the volume","media volume","volume level"),
        AndroidControlId.MEDIA_MUTE to listOf("mute media","mute the media","mute media volume","mute volume","mute the volume"),
        AndroidControlId.MEDIA_UNMUTE to listOf("unmute media","unmute the media"),
        AndroidControlId.RINGER_SILENT to listOf("mute my phone","silence my phone","silent mode","set silent mode"),
        AndroidControlId.RINGER_NORMAL to listOf("unmute my phone","turn the ringer on","normal ringer mode"),
        AndroidControlId.VIBRATE to listOf("switch to vibrate","vibrate mode","set vibrate mode"),
        AndroidControlId.DND_ON to listOf("enable do not disturb","turn on do not disturb","enable dnd","turn on dnd"),
        AndroidControlId.DND_OFF to listOf("disable do not disturb","turn off do not disturb","disable dnd","turn off dnd"),
        AndroidControlId.DND_STATUS to listOf("check do not disturb","is do not disturb on","dnd status"),
        AndroidControlId.LOCK to listOf("lock my phone","lock the screen","secure my phone","turn off and lock my screen"),
        AndroidControlId.UNLOCK to listOf("unlock my phone","unlock the screen"),
        AndroidControlId.BATTERY to listOf("check battery","battery status","battery percentage","tell me the battery","what is my battery","charging status"),
        AndroidControlId.POWER_SAVER to listOf("open battery saver settings","turn on battery saver","ultra battery saver","open battery usage"),
        AndroidControlId.DISPLAY to listOf("open display settings","change brightness"),
        AndroidControlId.SOUND to listOf("open sound settings"),
        AndroidControlId.ROTATION to listOf("open rotation settings","enable auto rotate","disable auto rotate","change screen rotation"),
        AndroidControlId.PORTRAIT to listOf("set jarvis to portrait","jarvis portrait mode"),
        AndroidControlId.LANDSCAPE to listOf("set jarvis to landscape","jarvis landscape mode"),
        AndroidControlId.DARK_MODE to listOf("open dark mode settings","enable dark mode","disable dark mode","system dark mode"),
        AndroidControlId.READING_MODE to listOf("open reading mode","turn on reading mode","eye comfort settings"),
        AndroidControlId.CAST to listOf("open cast","open cast settings","cast my screen"),
        AndroidControlId.SHARE to listOf("share a file","open share","share my file"),
        AndroidControlId.QUICK_SHARE to listOf("open quick share","quick share"),
        AndroidControlId.MI_SHARE to listOf("open mi share","mi share"),
        AndroidControlId.TV_REMOTE to listOf("open tv remote","control my television"),
        AndroidControlId.FOCUS to listOf("open focus mode","enable focus mode","disable focus mode"),
        AndroidControlId.BEDTIME to listOf("open bedtime mode","enable bedtime mode","disable bedtime mode"),
        AndroidControlId.WALLET to listOf("open wallet","open payment settings"),
        AndroidControlId.RECORD_START to listOf("start screen recording","record my screen","start recording my screen"),
        AndroidControlId.RECORD_STOP to listOf("stop screen recording","stop recording my screen")
    ).flatMap{(id,names)->names.map{it to id}}.toMap()
    fun parse(raw:String):AndroidControlCommand? {
        if(Regex("[-−]\\s*\\p{Nd}").containsMatchIn(raw))return null
        val input=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ").removePrefix("جارویس ").removePrefix("please ").removeSuffix(" please")
        aliases[input]?.let{return AndroidControlCommand(it)}
        val volume=Regex("^(?:set )?(?:media )?volume(?: to)? ([0-9]{1,3})(?: percent)?$").matchEntire(input)
        return volume?.groupValues?.get(1)?.toIntOrNull()?.takeIf{it in 0..100}?.let{AndroidControlCommand(AndroidControlId.VOLUME_SET,it)}
    }
}

object AndroidControlCatalog {
    val entries=AndroidControlId.entries.map{id->
        val support=when(id){
            AndroidControlId.TORCH_ON,AndroidControlId.TORCH_OFF,AndroidControlId.VOLUME_UP,AndroidControlId.VOLUME_DOWN,AndroidControlId.VOLUME_SET,AndroidControlId.VOLUME_STATUS,AndroidControlId.MEDIA_MUTE,AndroidControlId.MEDIA_UNMUTE,AndroidControlId.BATTERY,AndroidControlId.DND_STATUS,AndroidControlId.WIFI_STATUS,AndroidControlId.BLUETOOTH_STATUS->ControlSupport.FULLY_SUPPORTED
            AndroidControlId.LOCK,AndroidControlId.UNLOCK,AndroidControlId.DND_ON,AndroidControlId.DND_OFF,AndroidControlId.RINGER_SILENT,AndroidControlId.RINGER_NORMAL,AndroidControlId.VIBRATE,AndroidControlId.BLUETOOTH_ON,AndroidControlId.RECORD_START,AndroidControlId.RECORD_STOP,AndroidControlId.PORTRAIT,AndroidControlId.LANDSCAPE->ControlSupport.REQUIRES_USER_CONFIRMATION
            AndroidControlId.TV_REMOTE,AndroidControlId.MI_SHARE,AndroidControlId.WALLET->ControlSupport.UNSUPPORTED_OR_DEVICE_DEPENDENT
            else->ControlSupport.SETTINGS_SHORTCUT_ONLY
        }
        val permission=when(id){AndroidControlId.TORCH_ON,AndroidControlId.TORCH_OFF->"Camera permission and an available torch";AndroidControlId.BLUETOOTH_ON,AndroidControlId.BLUETOOTH_OFF,AndroidControlId.BLUETOOTH_STATUS->"Nearby devices on Android12+";AndroidControlId.LOCK->"Explicit lock-only Android device-administrator grant";AndroidControlId.UNLOCK->"Android PIN, password, pattern or biometrics when required";AndroidControlId.DND_ON,AndroidControlId.DND_OFF,AndroidControlId.RINGER_SILENT->"Notification policy access when Android requires it";AndroidControlId.RECORD_START->"Fresh Android screen-capture consent and visible recording service";else->"Normal Android access; system interaction where indicated"}
        val setup=when(id){
            AndroidControlId.PORTRAIT,AndroidControlId.LANDSCAPE->"Changes the Jarvis control activity only, not system-wide rotation."
            AndroidControlId.HOTSPOT->"Open Android network settings. Internet tethering is restricted; local-only hotspot is not an internet tethering replacement."
            AndroidControlId.DARK_MODE,AndroidControlId.READING_MODE->"Open Android display settings. Xiaomi/global theme changes require your action; no global change is inferred."
            AndroidControlId.FOCUS,AndroidControlId.BEDTIME->"Open Android Settings and choose Digital Wellbeing. No public API is assumed to toggle its private modes."
            AndroidControlId.SHARE,AndroidControlId.QUICK_SHARE,AndroidControlId.MI_SHARE->"Select an authorized file in Personal Content, then use its Android Sharesheet. Quick Share/Mi Share availability depends on installed handlers."
            AndroidControlId.TV_REMOTE->"Pair a supported TV through its authorized remote app. No universal television protocol is assumed."
            else->permission
        }
        ControlCapability(id,id.name.lowercase().replace('_',' '),support,setup,permission)
    }
    fun capability(id:AndroidControlId)=entries.first{it.id==id}
}
