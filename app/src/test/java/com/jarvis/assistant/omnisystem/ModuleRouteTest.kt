package com.jarvis.assistant.omnisystem
import org.junit.Assert.*
import org.junit.Test
class ModuleRouteTest {
    @Test fun routesActualQueriesWithoutLaunchingTools() {
        assertEquals(HudModule.WEATHER,ModuleRoute.fromUserInput("tell me the weather tomorrow"))
        assertEquals(HudModule.BATTERY,ModuleRoute.fromUserInput("show my battery health"))
        assertEquals(HudModule.BLUETOOTH,ModuleRoute.fromUserInput("show my Bluetooth devices"))
        assertEquals(HudModule.NETWORK,ModuleRoute.fromUserInput("what is my internet speed"))
        assertEquals(HudModule.WIFI,ModuleRoute.fromUserInput("what is my Wi-Fi"))
    }
    @Test fun preservesDirectAppLaunchesAndNegativeRequests() {
        listOf("open Spotify","open YouTube","open Camera","do not show my battery","never scan bluetooth","remember my battery preference").forEach{assertNull(it,ModuleRoute.fromUserInput(it))}
    }
    @Test fun accessoryFollowUpRetainsTheSelectedModule() {
        assertNull(ModuleRoute.fromUserInput("what is its battery"))
        assertNull(ModuleRoute.fromUserInput("باتری آن چقدر است"))
        assertEquals(HudModule.CAMERA,ModuleRoute.fromUserInput("دوربین جلو را باز کن"))
    }
    @Test fun supportsSpanishAndPersianQueries() {
        assertEquals(HudModule.WEATHER,ModuleRoute.fromUserInput("clima de Quito"))
        assertEquals(HudModule.WEATHER,ModuleRoute.fromUserInput("آب و هوا"))
        assertEquals(HudModule.MUSIC,ModuleRoute.fromUserInput("این چه آهنگی است"))
        assertEquals(HudModule.BLUETOOTH,ModuleRoute.fromUserInput("بلوتوث"))
        assertEquals(HudModule.BATTERY,ModuleRoute.fromUserInput("سلامت باتری"))
    }
}
