package com.jarvis.assistant.security
import com.jarvis.assistant.tools.UtilityIntentParser
import org.junit.Assert.*
import org.junit.Test
class UnlockPresentationPolicyTest {
    @Test fun setupDoesNotGrantAnAuthenticationBypass(){assertEquals(UnlockPresentation.DISABLED,UnlockPresentationPolicy.choose(false,true,true,true,true))}
    @Test fun alreadyUnlockedNeedsNoExtraChallenge(){assertEquals(UnlockPresentation.ALREADY_UNLOCKED,UnlockPresentationPolicy.choose(true,false,false,true,true))}
    @Test fun lockedBackgroundNeedsUserNotificationTapOrManualOpen(){
        assertEquals(UnlockPresentation.NOTIFICATION,UnlockPresentationPolicy.choose(true,true,true,false,true))
        assertEquals(UnlockPresentation.OPEN_MANUALLY,UnlockPresentationPolicy.choose(true,true,true,false,false))
        assertEquals(UnlockPresentation.FOREGROUND_REQUEST,UnlockPresentationPolicy.choose(true,true,true,true,false))
    }
    @Test fun twentyMinuteTimersDoNotBecomeFiveOrTwentyFiveMinutes(){
        assertEquals(1200,UtilityIntentParser.timerSeconds("start a twenty-minute timer"))
        assertEquals(1200,UtilityIntentParser.timerSeconds("temporizador de veinte minutos"))
        assertEquals(1200,UtilityIntentParser.timerSeconds("تایمر بیست دقیقه"))
        assertNull(UtilityIntentParser.timerSeconds("timer twenty five minutes"))
        assertNull(UtilityIntentParser.timerSeconds("timer twenty-one minutes"))
    }
}
