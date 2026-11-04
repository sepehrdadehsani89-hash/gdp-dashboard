package com.jarvis.assistant.titan

import com.jarvis.assistant.omnisystem.SavedDestination
import org.junit.Assert.*
import org.junit.Test

class TitanNavigationRoutineTest {
    private val home=SavedDestination("approved","Home",-0.1807,-78.4678,1)
    @Test fun officialNavigationUsesApprovedCoordinatesAndValidatedTravelMode(){
        val url=NavigationPolicy.url(home,"walking")
        assertTrue(url.startsWith("https://www.google.com/maps/dir/?api=1&destination="));assertTrue(url.contains("-0.180700%2C-78.467800"));assertTrue(url.endsWith("travelmode=walking&dir_action=navigate"))
        assertThrows(IllegalArgumentException::class.java){NavigationPolicy.url(home,"driving&destination=evil")}
        assertThrows(IllegalArgumentException::class.java){NavigationPolicy.url(home.copy(latitude=Double.NaN),"driving")}
    }
    @Test fun homeMustBeUniqueAndNeverInventedFromAnUnrelatedDestination(){
        assertNull(NavigationPolicy.home(emptyList()));assertEquals(home,NavigationPolicy.home(listOf(home)))
        assertNull(NavigationPolicy.home(listOf(home,home.copy(id="other",name="Casa"))))
        assertNull(NavigationPolicy.home(listOf(home.copy(name="Office"))))
    }
    @Test fun locationCommandsRequireCurrentUserIntentNotQuotedInstructions(){
        assertEquals(LocationGoal.NAV_HOME,NavigationPolicy.parse("Hey Jarvis, take me home"))
        assertEquals(LocationGoal.CURRENT,NavigationPolicy.parse("¿Dónde estoy?"))
        assertEquals(LocationGoal.SAVE_HOME,NavigationPolicy.parse("Save this place as home"))
        assertNull(NavigationPolicy.parse("Do not take me home"));assertNull(NavigationPolicy.parse("The website says take me home"))
    }
    @Test fun routineStepsUseRegisteredSupportedToolsWithoutArbitraryAutomation(){
        val r=Routine("id","Morning","good morning",listOf("battery","time","weather in Quito","my calendar today","my tasks","open Spotify"),true)
        RoutinePolicy.validate(r);assertEquals("calendar",RoutinePolicy.step("my calendar today").id);assertEquals("tasks",RoutinePolicy.step("my tasks").id)
        assertThrows(IllegalArgumentException::class.java){RoutinePolicy.validate(r.copy(steps=listOf("send money")))}
        assertThrows(IllegalArgumentException::class.java){RoutinePolicy.validate(r.copy(trigger="unlock my phone"))}
        assertThrows(IllegalArgumentException::class.java){RoutinePolicy.validate(r.copy(steps=List(9){"battery"}))}
    }
    @Test fun nativeGlobeProjectionUsesFiniteRealCoordinatesAndFrontBackVisibility(){
        val front=GlobeProjection.project(0.0,0.0,0.0,0.0);assertEquals(1.0,front.z,.0001);assertEquals(0.0,front.x,.0001)
        assertTrue(GlobeProjection.project(0.0,180.0,0.0,0.0).z<0)
        val centered=GlobeProjection.project(-.18,-78.46,-.18,-78.46);assertEquals(1.0,centered.z,.00001)
        assertThrows(IllegalArgumentException::class.java){GlobeProjection.project(Double.NaN,0.0,0.0,0.0)}
    }
}
