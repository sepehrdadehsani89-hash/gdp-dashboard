package com.jarvis.assistant.farewell

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.neural.AppMatching
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FarewellCatalogTest {
    private val source get()=listOf(File("src/main/assets/farewells/catalog.tsv"),File("app/src/main/assets/farewells/catalog.tsv")).first{it.isFile}.readText()
    private fun catalog()=FarewellCatalog.parse(source){0}
    @Test fun atLeastFiveHundredAuthoredEntriesExist(){assertTrue(catalog().lines.size>=500)}
    @Test fun eachLanguageHasOneHundredSeventyEntries(){val c=catalog();for(language in listOf("en","es","fa"))assertEquals(170,c.lines.count{it.language==language})}
    @Test fun eachContextHasTenChoicesPerLanguage(){val c=catalog();for(language in listOf("en","es","fa"))for(category in FarewellCategory.entries)assertEquals(10,c.lines.count{it.language==language && it.category==category})}
    @Test fun punctuationOrAccentChangesDoNotCountAsNewEntries(){val lines=catalog().lines;assertEquals(lines.size,lines.map{AppMatching.normalize(it.text)}.distinct().size)}
    @Test fun recentChoicesAreNotRepeated(){val c=catalog();val choices=(1..9).map{c.choose(AppLanguage.EN,FarewellCategory.GENERAL,false)};assertEquals(9,choices.distinct().size)}
    @Test fun catalogCyclesWithoutExhaustion(){val c=catalog();repeat(100){assertTrue(c.choose(AppLanguage.EN,FarewellCategory.SHORT,false).isNotBlank())}}
    @Test fun englishChoiceDoesNotFallBackToSpanish(){val c=catalog();val line=c.choose(AppLanguage.EN,FarewellCategory.BEDTIME,false);assertTrue(c.lines.any{it.language=="en" && it.text==line})}
    @Test fun spanishChoiceKeepsSpanishText(){val c=catalog();val line=c.choose(AppLanguage.ES,FarewellCategory.STUDY,false);assertTrue(c.lines.any{it.language=="es" && it.text==line})}
    @Test fun persianChoiceKeepsPersianText(){val c=catalog();val line=c.choose(AppLanguage.FA,FarewellCategory.WORK,false);assertTrue(c.lines.any{it.language=="fa" && it.text==line})}
    @Test fun courtesyIsOptional(){assertFalse(catalog().choose(AppLanguage.EN,FarewellCategory.GENERAL,false).contains("sir",true))}
    @Test fun englishCourtesyIsAPreference(){assertTrue(catalog().choose(AppLanguage.EN,FarewellCategory.GENERAL,true).startsWith("Certainly, sir."))}
    @Test fun spanishCourtesyIsLocalized(){assertTrue(catalog().choose(AppLanguage.ES,FarewellCategory.GENERAL,true).startsWith("Por supuesto, señor."))}
    @Test fun persianCourtesyIsLocalized(){assertTrue(catalog().choose(AppLanguage.FA,FarewellCategory.GENERAL,true).startsWith("حتماً، قربان."))}
    @Test fun incompleteCatalogIsRejectedRatherThanClaimingFiveHundred(){assertThrows(IllegalArgumentException::class.java){FarewellCatalog.parse("en|GENERAL|Goodbye.")}}
    @Test fun duplicateCatalogEntryIsRejected(){val first=source.lineSequence().first{!it.startsWith("#") && it.isNotBlank()};assertThrows(IllegalArgumentException::class.java){FarewellCatalog.parse(source+"\n"+first)}}
    @Test fun malformedLanguageIsRejected(){assertThrows(IllegalArgumentException::class.java){FarewellCatalog.parse(source+"\nxx|GENERAL|Unsupported language.")}}
    @Test fun malformedCategoryIsRejected(){assertThrows(IllegalArgumentException::class.java){FarewellCatalog.parse(source+"\nen|UNKNOWN|Unknown type.")}}
    @Test fun bedtimeIntentOverridesTheClock(){assertEquals(FarewellCategory.BEDTIME,FarewellCatalog.category("good night",10))}
    @Test fun studyIntentSelectsStudyCategory(){assertEquals(FarewellCategory.STUDY,FarewellCatalog.category("I'm done studying",10))}
    @Test fun workIntentSelectsWorkCategory(){assertEquals(FarewellCategory.WORK,FarewellCatalog.category("I am done working",10))}
    @Test fun genuineThanksSelectsThanksCategory(){assertEquals(FarewellCategory.THANKS,FarewellCatalog.category("Thank you, I'm done",10))}
    @Test fun eveningContextUsesActualHour(){assertEquals(FarewellCategory.EVENING,FarewellCatalog.category("goodbye",20))}
    @Test fun reminderCategoryRequiresVerifiedContextFlag(){assertNotEquals(FarewellCategory.REMINDER,FarewellCatalog.category("goodbye",12));assertEquals(FarewellCategory.REMINDER,FarewellCatalog.category("goodbye",12,reminders=true))}
    @Test fun computerCategoryRequiresAnActualSessionFlag(){assertNotEquals(FarewellCategory.COMPUTER,FarewellCatalog.category("goodbye",12));assertEquals(FarewellCategory.COMPUTER,FarewellCatalog.category("goodbye",12,computer=true))}
    @Test fun catalogDoesNotContainGeneratedPersonalFactPlaceholders(){assertFalse(catalog().lines.any{it.text.contains("{") || it.text.contains("}") || it.text.contains("TODO")})}
    @Test fun farewellBlocksDelayedMicrophoneResume(){val gate=ConversationExitGate();gate.finish();assertTrue(gate.blocked)}
    @Test fun explicitStartCanBeginANewListeningSession(){val gate=ConversationExitGate();gate.finish();gate.explicitStart();assertFalse(gate.blocked)}
    @Test fun repeatedFarewellCannotReopenListening(){val gate=ConversationExitGate();repeat(3){gate.finish()};assertTrue(gate.blocked)}
}
