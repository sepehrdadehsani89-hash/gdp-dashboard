package com.jarvis.assistant.google

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GoogleCommandParserTest(private val command:String,private val product:GoogleProduct?,private val kind:GoogleQueryKind?){
    @Test fun actualIntentAndServiceAreClassified(){val q=GoogleCommandParser.parse(command);assertEquals(product,q?.product);assertEquals(kind,q?.kind)}
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}") fun cases()=listOf(
            arrayOf("Catch me up on my emails.",GoogleProduct.GMAIL,GoogleQueryKind.RECENT),
            arrayOf("Hey Jarvis, summarize my Gmail",GoogleProduct.GMAIL,GoogleQueryKind.RECENT),
            arrayOf("How many unread emails do I have?",GoogleProduct.GMAIL,GoogleQueryKind.UNREAD),
            arrayOf("What's my most important email?",GoogleProduct.GMAIL,GoogleQueryKind.IMPORTANT),
            arrayOf("Did my teacher email me?",GoogleProduct.GMAIL,GoogleQueryKind.TEACHER),
            arrayOf("Summarize today's messages.",GoogleProduct.GMAIL,GoogleQueryKind.TODAY),
            arrayOf("Show unread emails",GoogleProduct.GMAIL,GoogleQueryKind.UNREAD_MESSAGES),
            arrayOf("What projects am I working on?",GoogleProduct.DRIVE,GoogleQueryKind.PROJECTS),
            arrayOf("Find my latest project",GoogleProduct.DRIVE,GoogleQueryKind.PROJECTS),
            arrayOf("Which files did I recently edit?",GoogleProduct.DRIVE,GoogleQueryKind.RECENT),
            arrayOf("my Google calendar today",GoogleProduct.CALENDAR,GoogleQueryKind.TODAY),
            arrayOf("show my Google tasks",GoogleProduct.TASKS,GoogleQueryKind.RECENT),
            arrayOf("lee mi último correo",GoogleProduct.GMAIL,GoogleQueryKind.RECENT),
            arrayOf("muestra correos sin leer",GoogleProduct.GMAIL,GoogleQueryKind.UNREAD_MESSAGES),
            arrayOf("Open Gmail",null,null),arrayOf("Open Chrome",null,null),
            arrayOf("How do I check my email?",null,null),arrayOf("Do not check my email",null,null),
            arrayOf("Send my teacher an email",null,null),arrayOf("Delete all my mail",null,null),
            arrayOf("What is Google Drive?",null,null),arrayOf("What does unread mean?",null,null),
            arrayOf("Open my Google password manager",null,null),arrayOf("Search YouTube for music",null,null)
        )
    }
}
