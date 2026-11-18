package com.jarvis.assistant.companion

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DesktopRegistryTest {
    private fun pair(id:String=UUID.randomUUID().toString(),session:String=UUID.randomUUID().toString())=DesktopPair("https://192.168.1.2:8765","a".repeat(64),id,UUID.randomUUID().toString(),session,"b".repeat(43))
    private class Fixture {var raw:String?=null;var exists=false;var legacy:String?=null;var legacyPresent=false;var writesFail=false
        fun registry()=DesktopRegistry({raw},{exists},{check(!writesFail);raw=it;exists=true},{legacy},{legacyPresent},{legacy=null;legacyPresent=false})}
    @Test fun emptyRegistryDoesNotInventTarget(){assertNull(Fixture().registry().active())}
    @Test fun legacyPairMigratesWithoutChangingCredentials(){val f=Fixture();val p=pair();f.legacy=p.json();f.legacyPresent=true;assertEquals(p,f.registry().active()!!.pair);assertEquals(p.json(),f.legacy)}
    @Test fun migratedPairSurvivesRestart(){val f=Fixture();val p=pair();f.legacy=p.json();f.registry().snapshot();f.legacy=null;assertEquals(p,f.registry().active()!!.pair)}
    @Test fun undecryptableRegistryDoesNotFallBackToLegacy(){val f=Fixture();f.exists=true;f.legacy=pair().json();assertThrows(IllegalStateException::class.java){f.registry().snapshot()};assertNull(f.raw)}
    @Test fun undecryptableLegacyIsNotOverwritten(){val f=Fixture();f.legacyPresent=true;assertThrows(IllegalStateException::class.java){f.registry().add(pair())};assertNull(f.raw)}
    @Test fun corruptRegistryIsPreserved(){val f=Fixture();f.raw="broken";assertThrows(Exception::class.java){f.registry().add(pair())};assertEquals("broken",f.raw)}
    @Test fun twentyDistinctTargetsCanBeSaved(){val r=Fixture().registry();repeat(20){r.add(pair())};assertEquals(20,r.snapshot().devices.size)}
    @Test fun twentyFirstTargetIsRejectedWithoutReplacement(){val f=Fixture();val r=f.registry();repeat(20){r.add(pair())};val before=f.raw;assertThrows(IllegalStateException::class.java){r.add(pair())};assertEquals(before,f.raw)}
    @Test fun rePairingSameDeviceRotatesOnlyItsSession(){val r=Fixture().registry();val p=pair();r.add(p,"Laptop");val new=pair(p.deviceId);r.add(new);assertEquals(1,r.snapshot().devices.size);assertEquals(new,r.active()!!.pair);assertEquals("Laptop",r.active()!!.label)}
    @Test fun addingTargetPreservesEveryExistingPair(){val r=Fixture().registry();val p=pair();r.add(p,"First");r.add(pair(),"Second");assertEquals(p,r.snapshot().devices.first{it.label=="First"}.pair)}
    @Test fun selectingExactLabelRestoresThatTargetsOwnCredentials(){val r=Fixture().registry();val a=pair();val b=pair();r.add(a,"Laptop");r.add(b,"Office");assertEquals(a,r.select("Laptop").pair)}
    @Test fun aliasSelectsOneApprovedTarget(){val r=Fixture().registry();val a=pair();r.add(a);r.rename(a.deviceId,"Laptop",setOf("my study machine"));assertEquals(a,r.select("my study machine").pair)}
    @Test fun unknownNameDoesNotChangeActiveTarget(){val r=Fixture().registry();r.add(pair());val before=r.active();assertThrows(IllegalStateException::class.java){r.select("unpaired machine")};assertEquals(before,r.active())}
    @Test fun duplicateLabelsCannotBeAdded(){val r=Fixture().registry();r.add(pair(),"Laptop");assertThrows(IllegalArgumentException::class.java){r.add(pair(),"laptop")}}
    @Test fun aliasCannotHijackAnotherTargetsName(){val r=Fixture().registry();val a=pair();r.add(a,"Laptop");val b=pair();r.add(b,"Desktop");assertThrows(IllegalArgumentException::class.java){r.rename(b.deviceId,"Desktop",setOf("Laptop"))}}
    @Test fun removingSelectedTargetDoesNotSilentlySelectAnother(){val r=Fixture().registry();val a=pair();r.add(pair());r.add(a);r.remove(a.deviceId);assertNull(r.active());assertEquals(1,r.snapshot().devices.size)}
    @Test fun removingOtherTargetPreservesCurrent(){val r=Fixture().registry();val a=pair();val b=pair();r.add(a);r.add(b);r.remove(a.deviceId);assertEquals(b,r.active()!!.pair)}
    @Test fun forgettingMigratedPairDoesNotResurrectLegacyGrant(){val f=Fixture();val p=pair();f.legacy=p.json();f.legacyPresent=true;val r=f.registry();r.snapshot();r.remove(p.deviceId);assertNull(f.registry().active());assertNull(f.legacy)}
    @Test fun staleRevokeCannotRemoveANewPairing(){val r=Fixture().registry();val a=pair();r.add(a);val b=pair(a.deviceId);r.add(b);assertThrows(IllegalStateException::class.java){r.remove(a.deviceId,a.sessionId)};assertEquals(b,r.active()!!.pair)}
    @Test fun writeFailurePreservesPreviousSavedTargets(){val f=Fixture();val r=f.registry();val a=pair();r.add(a);f.writesFail=true;assertThrows(IllegalStateException::class.java){r.add(pair())};assertEquals(a,r.active()!!.pair)}
    @Test fun reviewedCommandRejectsDifferentComputer(){val a=pair();val b=pair();assertFalse(DesktopRegistry.reviewed(b,a.deviceId,a.sessionId))}
    @Test fun reviewedCommandRejectsRotatedSession(){val a=pair();val b=pair(a.deviceId);assertFalse(DesktopRegistry.reviewed(b,a.deviceId,a.sessionId))}
    @Test fun reviewedCommandRejectsMissingTargetBinding(){val a=pair();assertFalse(DesktopRegistry.reviewed(a,null,null))}
    @Test fun reviewedCommandAcceptsOriginalComputerAndSession(){val a=pair();assertTrue(DesktopRegistry.reviewed(a,a.deviceId,a.sessionId))}
    @Test fun voiceSelectionCannotExecuteArbitraryCommands(){assertNull(DesktopRegistry.selectionCommand("run powershell on computer Laptop"));assertEquals("Laptop",DesktopRegistry.selectionCommand("switch to computer Laptop"))}
    @Test fun diagnosticDoesNotExposePairTokenOrLabel(){val text=DesktopConnection("private label",pair()).toString();assertFalse(text.contains("b".repeat(43)));assertFalse(text.contains("private label"))}
}
