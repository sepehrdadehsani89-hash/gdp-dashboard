package com.jarvis.assistant.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneRequestParserTest {
    @Test fun explicitContactListInThreeLanguages() {
        assertEquals(PhoneRequest.Contacts(), PhoneRequestParser.parse("Show my contacts"))
        assertEquals(PhoneRequest.Contacts(), PhoneRequestParser.parse("Muéstrame mis contactos"))
        assertEquals(PhoneRequest.Contacts(), PhoneRequestParser.parse("مخاطبین من را نشان بده"))
    }

    @Test fun contactNamesRetainAccentsAndPersianCharacters() {
        assertEquals(PhoneRequest.Contacts("Jane Smith"), PhoneRequestParser.parse("Find contact Jane Smith"))
        assertEquals(PhoneRequest.Contacts("María"), PhoneRequestParser.parse("Número de teléfono de María"))
        assertEquals(PhoneRequest.Contacts("علی"), PhoneRequestParser.parse("شماره تلفن علی چیه؟"))
    }

    @Test fun lookupCannotBecomeWildcardOrSqlSyntax() {
        assertNull(PhoneRequestParser.parse("contact Jane; delete contacts"))
        assertNull(PhoneRequestParser.parse("contact " + "x".repeat(81)))
    }

    @Test fun sharingAndMutationRequestsDoNotReadPrivateData() {
        listOf("show my contacts and send them", "delete my calendar", "envía mi ubicación", "مخاطبین من را ارسال کن").forEach { assertNull(it, PhoneRequestParser.parse(it)) }
    }

    @Test fun calendarUsesTodayOrSevenDayWindow() {
        assertEquals(PhoneRequest.Calendar(1), PhoneRequestParser.parse("show my calendar today"))
        assertEquals(PhoneRequest.Calendar(7), PhoneRequestParser.parse("my calendar"))
        assertEquals(PhoneRequest.Calendar(7), PhoneRequestParser.parse("muestra mi calendario"))
        assertEquals(PhoneRequest.Calendar(1), PhoneRequestParser.parse("تقویم امروز را نشان بده"))
    }

    @Test fun locationRequiresAnExplicitSelfLocationRequest() {
        assertEquals(PhoneRequest.Location, PhoneRequestParser.parse("Where am I?"))
        assertEquals(PhoneRequest.Location, PhoneRequestParser.parse("¿Dónde estoy?"))
        assertEquals(PhoneRequest.Location, PhoneRequestParser.parse("من کجا هستم؟"))
        assertNull(PhoneRequestParser.parse("where is the museum"))
    }

    @Test fun ordinaryConversationAndNewsDoNotReadPhoneContext() {
        listOf("find news about Ecuador", "tell me a story", "add an event tomorrow", "what is a calendar", "location tracking", "what is the number of planets", "número de planetas").forEach { assertNull(it, PhoneRequestParser.parse(it)) }
    }

    @Test fun stripsWakePrefixButDoesNotRequireIt() {
        assertEquals(PhoneRequest.Location, PhoneRequestParser.parse("Hey Jarvis, where am I?"))
        assertEquals(PhoneRequest.Location, PhoneRequestParser.parse("سلام جارویس، من کجا هستم؟"))
    }
}
