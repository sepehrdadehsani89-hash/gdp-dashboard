package com.jarvis.assistant.companion

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DesktopUnsupportedActionsTest(private val command:String,private val unsupported:Boolean){
    @Test fun computerMutationAndPrivateTelemetryNeverBecomeFictionalAiResults(){assertEquals(unsupported,DesktopUnsupportedActions.matches(command))}
    companion object{@JvmStatic @Parameterized.Parameters(name="{0}")fun cases()=listOf(
        arrayOf("Shut down my computer",true),arrayOf("Restart my PC",true),arrayOf("Sleep my desktop",true),arrayOf("Lock my Windows computer",true),
        arrayOf("Open Photoshop on my computer",true),arrayOf("Mute my computer",true),arrayOf("Increase computer volume",true),arrayOf("Play music on my computer",true),
        arrayOf("Read my computer status",true),arrayOf("Show my desktop telemetry",true),arrayOf("What is my computer RAM",true),arrayOf("How much RAM does my computer have",true),
        arrayOf("Open my computer files",true),arrayOf("Copy files on my PC",true),arrayOf("Delete my computer files",true),arrayOf("Access my desktop workspace",true),
        arrayOf("Open Chrome on my computer",false),arrayOf("Open Chrome on my computer and search for Jarvis",false),arrayOf("Open Gmail",false),arrayOf("Lock my phone",false),
        arrayOf("How do I restart my computer",false),arrayOf("Translate restart my computer into Spanish",false),arrayOf("Do not shut down my computer",false),arrayOf("Remind me to restart my computer",false)
    )}
}
