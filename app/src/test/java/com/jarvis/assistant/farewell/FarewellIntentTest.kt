package com.jarvis.assistant.farewell

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FarewellIntentTest(private val text:String,private val ends:Boolean){
    @Test fun genuineCompletionIsSeparatedFromQuestionsAndNegations(){assertEquals(ends,FarewellIntent.matches(text))}
    companion object {@JvmStatic @Parameterized.Parameters(name="{0}") fun cases()=listOf(
        arrayOf("Goodbye, Jarvis.",true),arrayOf("Thank you, Jarvis. I have to go.",true),arrayOf("Thank you, Jarvis. What does goodbye mean?",false),arrayOf("Goodbye.",true),arrayOf("Bye!",true),arrayOf("See you later.",true),arrayOf("I have to go.",true),
        arrayOf("Thank you, I'm done.",true),arrayOf("That's all.",true),arrayOf("You can rest.",true),arrayOf("Stand down.",true),
        arrayOf("Good night.",true),arrayOf("Talk tomorrow.",true),arrayOf("Stop listening.",true),arrayOf("End conversation.",true),
        arrayOf("Hey Jarvis, I'm done",true),arrayOf("I'm done studying",true),arrayOf("I am done working",true),
        arrayOf("Hasta luego",true),arrayOf("Adiós",true),arrayOf("Buenas noches",true),arrayOf("Ya terminé",true),
        arrayOf("خداحافظ",true),arrayOf("شب بخیر",true),arrayOf("دیگه کافیه",true),
        arrayOf("What does goodbye mean?",false),arrayOf("Translate goodbye into Spanish",false),arrayOf("How do I say bye in Persian?",false),
        arrayOf("Do not stop listening",false),arrayOf("Don't end this conversation",false),arrayOf("I'm not done",false),
        arrayOf("If I say goodbye, what happens?",false),arrayOf("Remind me to say goodbye tomorrow",false),arrayOf("Call Mom and say goodbye",false),
        arrayOf("What's the song Goodbye?",false),arrayOf("Good night mode settings",false),arrayOf("See you later is a greeting?",false),
        arrayOf("Thank you, what's my battery?",false),arrayOf("I'm done with dinner, what's the weather?",false),arrayOf("You can rest after you answer this",false),
        arrayOf("What is standby?",false),arrayOf("How do I end conversation?",false),arrayOf("Stop listening to music",false),
        arrayOf("No te despidas",false),arrayOf("¿Qué significa adiós?",false),arrayOf("خداحافظ یعنی چه",false)
    )}
}
