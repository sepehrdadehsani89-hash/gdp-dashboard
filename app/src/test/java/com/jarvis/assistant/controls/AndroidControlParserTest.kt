package com.jarvis.assistant.controls

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Typed routing fixtures, not Android hardware execution. */
@RunWith(Parameterized::class)
class AndroidControlParserTest(private val input:String,private val expected:AndroidControlId?){
    @Test fun classifiesOnlyAnExplicitSupportedNativeCommand(){assertEquals(expected,AndroidControlParser.parse(input)?.id)}
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}") fun cases():Collection<Array<Any?>> = listOf(
            arrayOf("Hey Jarvis, turn on flashlight",AndroidControlId.TORCH_ON),arrayOf("enable the torch",AndroidControlId.TORCH_ON),arrayOf("disable torch",AndroidControlId.TORCH_OFF),
            arrayOf("turn on Wi-Fi",AndroidControlId.WIFI_ON),arrayOf("turn off wifi",AndroidControlId.WIFI_OFF),arrayOf("show Wi-Fi settings",AndroidControlId.WIFI_SETTINGS),
            arrayOf("is wifi on",AndroidControlId.WIFI_STATUS),arrayOf("turn on Bluetooth",AndroidControlId.BLUETOOTH_ON),arrayOf("turn off Bluetooth",AndroidControlId.BLUETOOTH_OFF),arrayOf("open Bluetooth settings",AndroidControlId.BLUETOOTH_SETTINGS),
            arrayOf("open hotspot settings",AndroidControlId.HOTSPOT),arrayOf("help me turn on Airplane Mode",AndroidControlId.AIRPLANE),arrayOf("lower the volume",AndroidControlId.VOLUME_DOWN),
            arrayOf("set media volume to 40 percent",AndroidControlId.VOLUME_SET),arrayOf("mute my phone",AndroidControlId.RINGER_SILENT),arrayOf("mute media",AndroidControlId.MEDIA_MUTE),arrayOf("unmute media",AndroidControlId.MEDIA_UNMUTE),
            arrayOf("switch to vibrate",AndroidControlId.VIBRATE),arrayOf("enable DND",AndroidControlId.DND_ON),arrayOf("turn off do not disturb",AndroidControlId.DND_OFF),arrayOf("lock my phone",AndroidControlId.LOCK),
            arrayOf("unlock my phone",AndroidControlId.UNLOCK),arrayOf("check battery",AndroidControlId.BATTERY),arrayOf("open battery saver settings",AndroidControlId.POWER_SAVER),arrayOf("set Jarvis to portrait",AndroidControlId.PORTRAIT),
            arrayOf("open dark mode settings",AndroidControlId.DARK_MODE),arrayOf("open reading mode",AndroidControlId.READING_MODE),arrayOf("open Cast",AndroidControlId.CAST),arrayOf("open Quick Share",AndroidControlId.QUICK_SHARE),
            arrayOf("open focus mode",AndroidControlId.FOCUS),arrayOf("open bedtime mode",AndroidControlId.BEDTIME),arrayOf("open wallet",AndroidControlId.WALLET),arrayOf("start screen recording",AndroidControlId.RECORD_START),arrayOf("stop screen recording",AndroidControlId.RECORD_STOP),
            arrayOf("open sound settings",AndroidControlId.SOUND),arrayOf("enciende la linterna",AndroidControlId.TORCH_ON),arrayOf("چراغ قوه را خاموش کن",AndroidControlId.TORCH_OFF),
            arrayOf("how do I turn on flashlight",null),arrayOf("turn on flashlight and open Gmail",null),arrayOf("set media volume to 101 percent",null),arrayOf("set volume to -5",null),arrayOf("run rm -rf /",null),arrayOf("what does mute my phone mean",null),arrayOf("open WhatsApp",null)
        )
    }
}
