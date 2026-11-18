package com.jarvis.assistant.google

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GoogleNativeReadBoundaryTest(private val text:String,private val native:Boolean){
    @Test fun privateMailReadsDoNotFallThroughToGeneralAI(){assertEquals(native,GoogleCommandParser.requiresNativeRead(text))}
    companion object{@JvmStatic @Parameterized.Parameters(name="{0}")fun cases()=listOf(
        arrayOf("Read my latest email",true),arrayOf("Show me the emails from Mom",true),arrayOf("Read emails from my teacher",true),
        arrayOf("Show email from Alice",true),arrayOf("Summarize my Gmail",true),arrayOf("Show my email",true),
        arrayOf("Who emailed me?",true),arrayOf("Read my last email",true),arrayOf("How many unread emails do I have?",true),
        arrayOf("Catch me up on my emails",true),arrayOf("Did my teacher email me?",true),arrayOf("Summarize today's messages",true),
        arrayOf("Open Gmail",false),arrayOf("Open Chrome",false),arrayOf("Read my WhatsApp notifications",false),
        arrayOf("Catch me up on my messages",false),arrayOf("How do email servers work?",false),arrayOf("What is Gmail?",false),
        arrayOf("Do not check my email",false),arrayOf("What does unread mean?",false),arrayOf("Translate the word email",false),
        arrayOf("Read a book about email protocols",false)
    )}
}
