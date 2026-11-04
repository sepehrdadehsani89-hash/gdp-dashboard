package com.jarvis.assistant.rebirth

import org.junit.Assert.*
import org.junit.Test

class RemovedMailIntegrationTest {
    @Test fun removedMailboxRequestsCannotBePassedToAiAsIfItHadAccess(){
        for(text in listOf("Read my latest email","Summarize my latest email","Read emails from Teacher","Open account center","Revisa mi correo"))assertTrue(text,RemovedMailIntegration.matches(text))
    }
    @Test fun openingAnInstalledMailAppAndDraftingTextRemainOrdinaryRequests(){
        for(text in listOf("Open Gmail","Open Outlook","Write a thank you email","Explain how email works","Read it"))assertFalse(text,RemovedMailIntegration.matches(text))
    }
}
