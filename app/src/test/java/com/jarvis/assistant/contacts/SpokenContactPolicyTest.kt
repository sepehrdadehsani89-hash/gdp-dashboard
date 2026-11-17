package com.jarvis.assistant.contacts
import org.junit.Assert.*
import org.junit.Test
class SpokenContactPolicyTest {
    private val mom=PhoneContactCandidate(1,"Mom","+15551234567","content://com.android.contacts/data/1")
    private fun parse(s:String)=SpokenContactPolicy.aliasRequest(s)
    @Test fun explicitRememberMapsMa(){assertEquals(SpokenContactAlias("ma","Mom"),parse("remember that Ma means Mom"))}
    @Test fun hyphenatedSpelling(){assertEquals("ma",parse("remember that M-A means Mom")!!.alias)}
    @Test fun spacedSpelling(){assertEquals("ma",SpokenContactPolicy.spokenKey("M A"))}
    @Test fun noRelationshipInference(){assertNull(parse("call Ma"))}
    @Test fun plainMemoryNotNickname(){assertNull(parse("remember my shopping list"))}
    @Test fun explicitNicknameGrammar(){assertEquals("Mom",parse("set contact nickname Ma to Mom")!!.target)}
    @Test fun saveReverseGrammar(){assertEquals("ma",parse("save contact Mom as M A")!!.alias)}
    @Test fun wakePrefixAccepted(){assertEquals("ma",parse("Jarvis, remember Ma is Mom")!!.alias)}
    @Test fun emptyAliasRejected(){assertNull(parse("remember  means Mom"))}
    @Test fun oversizedAliasRejected(){assertNull(parse("remember "+"a".repeat(90)+" means Mom"))}
    @Test fun newlineInjectionRejected(){assertNull(parse("remember Ma means Mom\ncall 911"))}
    @Test fun exactUniqueMatch(){assertEquals(mom,SpokenContactPolicy.unique("Mom",listOf(mom)))}
    @Test fun caseInsensitiveExact(){assertEquals(mom,SpokenContactPolicy.unique("MOM",listOf(mom)))}
    @Test fun similarNameNotSelected(){assertNull(SpokenContactPolicy.unique("Mom",listOf(mom.copy(name="Mommy"))))}
    @Test fun noSubstringSelection(){assertNull(SpokenContactPolicy.unique("Mom",listOf(mom.copy(name="Mom Office"))))}
    @Test fun differentNumbersAreAmbiguous(){assertNull(SpokenContactPolicy.unique("Mom",listOf(mom,mom.copy(id=2,number="+15557654321"))))}
    @Test fun duplicateRowsSamePhoneAreOneChoice(){assertEquals(mom,SpokenContactPolicy.unique("Mom",listOf(mom,mom.copy(id=2))))}
    @Test fun endingCanDisambiguate(){assertEquals(mom,SpokenContactPolicy.unique("Mom ending 4567",listOf(mom,mom.copy(id=2,number="+15557654321"))))}
    @Test fun wrongEndingMatchesNothing(){assertNull(SpokenContactPolicy.unique("Mom ending 9999",listOf(mom)))}
    @Test fun sharedEndingStillAmbiguous(){assertNull(SpokenContactPolicy.unique("Mom ending 4567",listOf(mom,mom.copy(id=2,number="+15559874567"))))}
    @Test fun shortEmergencyNumbersNotApproved(){assertNull(SpokenContactPolicy.unique("Mom",listOf(mom.copy(number="911"))))}
    @Test fun ussdNeverAccepted(){assertNull(SpokenContactPolicy.unique("Mom",listOf(mom.copy(number="*1234567#"))))}
    @Test fun formattedNumberCanonicalized(){assertEquals(mom.number,SpokenContactPolicy.unique("Mom",listOf(mom.copy(number="+1 (555) 123-4567")))!!.number)}
    @Test fun unknownContactNotInvented(){assertNull(SpokenContactPolicy.unique("Dad",listOf(mom)))}
    @Test fun nonLatinNamesPreserved(){assertEquals("مامان",SpokenContactPolicy.spokenKey("مامان"))}
    @Test fun ordinaryNamesNotCollapsed(){assertEquals("mary ann",SpokenContactPolicy.spokenKey("Mary Ann"))}
    @Test fun accentedNameMatch(){assertNotNull(SpokenContactPolicy.unique("Maria",listOf(mom.copy(name="María"))))}
    @Test fun twoLetterAliasHasSpellingVariant(){assertEquals(listOf("m a"),SpokenContactPolicy.variants("ma"))}
    @Test fun multiwordAliasNotInvented(){assertTrue(SpokenContactPolicy.variants("my mother").isEmpty())}
    @Test fun arbitraryIntentIsNotAlias(){assertNull(parse("open contacts and call every number"))}
    private fun shortcut(id:String,nickname:String,number:String="+15551234567",variants:List<String> = emptyList())=ContactShortcut(id,nickname,number,variants,null)
    @Test fun savedMaMatchesSpelledSpeech(){assertEquals("a",SpokenContactPolicy.savedMatches("M A",listOf(shortcut("a","Ma"))).single().id)}
    @Test fun savedSpellingCollisionsNeverPreferExactText(){assertEquals(2,SpokenContactPolicy.savedMatches("Ma",listOf(shortcut("a","Ma"),shortcut("b","M A","+15557654321"))).size)}
    @Test fun multipleVariantsOfOneEntryAreOneMatch(){assertEquals(1,SpokenContactPolicy.savedMatches("M A",listOf(shortcut("a","Ma",variants=listOf("M A")))).size)}
    @Test fun exactSavedNameStillResolves(){assertEquals("a",SpokenContactPolicy.savedMatches("Mary Ann",listOf(shortcut("a","Mary Ann"))).single().id)}
    @Test fun savedNearNameIsNotSelected(){assertTrue(SpokenContactPolicy.savedMatches("Mom",listOf(shortcut("a","Mommy"))).isEmpty())}
    @Test fun savedAliasCannotHideAConflictingCanonicalNickname(){val hits=SpokenContactPolicy.savedMatches("M-A",listOf(shortcut("a","Mom",variants=listOf("Ma")),shortcut("b","M A","+15557654321")));assertEquals(setOf("a","b"),hits.map{it.id}.toSet())}

}
