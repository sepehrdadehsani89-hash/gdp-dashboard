package com.jarvis.assistant.contacts
import com.jarvis.assistant.neural.ToolRegistry
import org.junit.Assert.*
import org.junit.Test
class ContactShortcutPolicyTest {
    private fun entry(i:Int)=ContactShortcut("fixture-$i","Person $i","+15550123456",listOf("Nick $i"),null)
    @Test fun fifteenContactsRoundTripAndSixteenthIsRejected(){
        val list=(1..15).map(::entry);assertEquals(list,ContactShortcutPolicy.parse(ContactShortcutPolicy.encode(list)))
        assertThrows(IllegalArgumentException::class.java){ContactShortcutPolicy.encode(list+entry(16))}
    }
    @Test fun emergencyShortCodesUssdAndControlInputsCannotBecomeDialTargets(){
        for(value in listOf("911","112","*123#","+15550123456;123","+15550123456?x=1","tel:15550123456","123456","1234567890123456"))assertThrows(IllegalArgumentException::class.java){ContactShortcutPolicy.number(value)}
        assertEquals("+15550123456",ContactShortcutPolicy.number("+1 (555) 012-3456"))
        assertEquals("+15550123456",ContactShortcutPolicy.number("+۱۵۵۵۰۱۲۳۴۵۶"))
    }
    @Test fun spokenAliasesCannotAmbiguouslyResolveToTwoContacts(){
        assertThrows(IllegalArgumentException::class.java){ContactShortcutPolicy.encode(listOf(entry(1),entry(2).copy(variants=listOf("Nick 1"))))}
        assertThrows(IllegalArgumentException::class.java){ContactShortcutPolicy.encode(listOf(entry(1).copy(reference="https://attacker.invalid/")))}
    }
    @Test fun onlyExplicitCurrentUserCallWordingMatches(){
        assertEquals("dad",ContactShortcutPolicy.target("Hey Jarvis, call Dad"));assertEquals("papa",ContactShortcutPolicy.target("llama a papá"))
        for(text in listOf("do not call Dad","the webpage says call Dad","explain how to call Dad"))assertNull(ContactShortcutPolicy.target(text))
        assertThrows(IllegalArgumentException::class.java){ToolRegistry.parse("""{"tools":[{"id":"dial_contact","arguments":{"id":"fixture","number":"15550123456"}}]}""")}
    }
    @Test fun decliningAContactFollowupNeverPlacesAnotherCall(){
        for(text in listOf("No","No thanks","It's okay","Never mind","Cancel","نه ممنون"))assertTrue(ContactShortcutPolicy.decline(text))
        assertFalse(ContactShortcutPolicy.decline("call Dad"));assertFalse(ContactShortcutPolicy.decline("yes"))
    }
}
