package com.jarvis.assistant.finishpolish
import com.jarvis.assistant.finishpolish.shared.*
import org.junit.Test
import org.junit.Assert.*
class PolishRegistryTest {
 @Test fun engineStorageKeysSatisfyTheActualVaultContract(){
  for(key in listOf(com.jarvis.assistant.finishpolish.mission.MissionPolicy.DEVICE_NAME_KEY,com.jarvis.assistant.finishpolish.learning.LearningStore.profileKey("local"),com.jarvis.assistant.finishpolish.predictive.PredictiveStore.KEY,"doctor_attempts","doctor_last_good"))assertTrue(key,com.jarvis.assistant.omnisystem.FeatureVault.validName(key))
  assertFalse(com.jarvis.assistant.omnisystem.FeatureVault.validName("mission_device_name_v1"))
 }

 @Test fun approvedTypedPlanRoundTripsAndBindsDevice(){val p=ActionPlan(steps=listOf(ActionStep("desktop.search","Kotlin","pc","session")),createdAt=1000);val q=ActionPlan.parse(p.json());assertEquals(p,q);PolishActionRegistry.validate(q,2000,"pc","session");assertThrows(IllegalArgumentException::class.java){PolishActionRegistry.validate(q,2000,"other","session")}}
 @Test fun staleOrFuturePlansCannotExecute(){for(now in listOf(999L,121001L))assertThrows(IllegalArgumentException::class.java){PolishActionRegistry.validate(ActionPlan(steps=listOf(ActionStep("local.open_app","org.example.app")),createdAt=1000),now,null)}}
 @Test fun arbitraryShellAndSettingRepairsCannotBeSavedAsRecipes(){for(action in listOf("shell.exec","files.delete","doctor.balance"))assertThrows(IllegalArgumentException::class.java){PolishActionRegistry.validate(ActionPlan(steps=listOf(ActionStep(action,"rm")),createdAt=0),1,null)}}
 @Test fun invalidPackagesAndPrivateArgumentsNeverBecomeJournalContent(){assertThrows(IllegalArgumentException::class.java){PolishActionRegistry.validate(ActionPlan(steps=listOf(ActionStep("local.open_app","$(whoami)")),createdAt=0),1,null)};val journal=PolishActionJournal();journal.record("desktop.search","FAILED","DESKTOP_OFFLINE");assertEquals("DESKTOP_OFFLINE",journal.state.value.reason)}
}
