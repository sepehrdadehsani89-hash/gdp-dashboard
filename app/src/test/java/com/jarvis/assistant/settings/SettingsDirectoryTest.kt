package com.jarvis.assistant.settings
import org.junit.Assert.*
import org.junit.Test
class SettingsDirectoryTest {
    private fun ids(q:String)=SettingsDirectory.search(q).map{it.id}
    @Test fun emptySearchShowsElevenSections(){assertEquals(11,ids("").size)}
    @Test fun whitespaceSearchShowsSections(){assertEquals(11,ids("   ").size)}
    @Test fun microphoneIsDiscoverable(){assertTrue("voice" in ids("microphone"))}
    @Test fun motionIsDiscoverable(){assertTrue("appearance" in ids("reduced motion"))}
    @Test fun captionsIsDiscoverable(){assertTrue("voice" in ids("captions"))}
    @Test fun chatgptIsDiscoverable(){assertEquals(listOf("ai"),ids("ChatGPT"))}
    @Test fun claudeIsDiscoverable(){assertEquals(listOf("ai"),ids("Claude"))}
    @Test fun localIsDiscoverable(){assertEquals(listOf("ai"),ids("local"))}
    @Test fun contactsIsDiscoverable(){assertEquals(listOf("device"),ids("contacts"))}
    @Test fun maAliasIsDiscoverable(){assertTrue("device" in ids("mom"))}
    @Test fun wifiIsDiscoverable(){assertTrue("device" in ids("wifi"))}
    @Test fun lockIsDiscoverable(){assertTrue("security" in ids("lock"))}
    @Test fun permissionIsDiscoverable(){assertTrue("security" in ids("permission"))}
    @Test fun setupIsDiscoverable(){assertEquals(listOf("about"),ids("onboarding"))}
    @Test fun spanishIsDiscoverable(){assertTrue("language" in ids("Spanish"))}
    @Test fun multiwordSearchRequiresEveryTerm(){assertEquals(listOf("appearance"),ids("charging particles"))}
    @Test fun unmatchedSearchIsHonest(){assertTrue(ids("teleport the phone").isEmpty())}
    @Test fun diagnosticSearchDoesNotInferSuccess(){assertTrue("diagnostics" in ids("retry"))}
    @Test fun punctuationDoesNotBreakSearch(){assertEquals(ids("AI Brain"),ids("AI—Brain"))}
    @Test fun enormousSearchIsBounded(){assertTrue(ids("x".repeat(100000)).isEmpty())}
}
