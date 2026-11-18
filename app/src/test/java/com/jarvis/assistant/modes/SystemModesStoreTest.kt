package com.jarvis.assistant.modes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SystemModesStoreTest {
    private class Fixture{var raw:String?=null;var backup:String?=null;var fail=false;fun create()=SystemModes({raw},{if(fail)error("disk");raw=it},{old,new->if(fail)error("disk");backup=old;raw=new})}
    @Test fun newStorePreservesNormalMode(){assertEquals(SystemModeState(),Fixture().create().state.value)}
    @Test fun silentModePersists(){val f=Fixture();f.create().silent(true);assertTrue(f.create().state.value.silent)}
    @Test fun wakeExceptionPersists(){val f=Fixture();f.create().silent(true,true);assertTrue(f.create().state.value.wakeInSilent)}
    @Test fun reminderExceptionPersists(){val f=Fixture();f.create().silent(true,reminders=false);assertFalse(f.create().state.value.keepReminders)}
    @Test fun deepStatePersistsAcrossProcessRecreation(){val f=Fixture();f.create().shutdown();assertTrue(f.create().state.value.deep)}
    @Test fun manualReopenClearsDeepOnly(){val f=Fixture();val s=f.create();s.silent(true,true,false);s.shutdown();s.reopen();assertEquals(SystemModeState(true,false,true,false),s.state.value)}
    @Test fun leavingSilentDoesNotAccidentallyLeaveDeep(){val s=Fixture().create();s.shutdown();s.silent(false);assertTrue(s.state.value.deep)}
    @Test fun deepEntryDoesNotDeleteSilentPreference(){val s=Fixture().create();s.silent(true);s.shutdown();assertTrue(s.state.value.silent)}
    @Test fun deepEntryPreservesReminderChoice(){val s=Fixture().create();s.silent(true,reminders=false);s.shutdown();assertFalse(s.state.value.keepReminders)}
    @Test fun normalModeCanBeRestored(){val s=Fixture().create();s.silent(true);s.silent(false);assertFalse(s.state.value.silent)}
    @Test fun corruptStateFailsClosedForCapture(){val f=Fixture();f.raw="broken";val s=f.create();assertTrue(s.state.value.deep);assertNotNull(s.storageError)}
    @Test fun corruptStateIsNotAutomaticallyOverwritten(){val f=Fixture();f.raw="broken";f.create();assertEquals("broken",f.raw)}
    @Test fun corruptStateRejectsOrdinaryChanges(){val f=Fixture();f.raw="broken";assertThrows(Exception::class.java){f.create().silent(true)};assertEquals("broken",f.raw)}
    @Test fun unknownVersionFailsClosed(){val f=Fixture();f.raw="""{"version":2,"silent":false,"deep":false,"wake":true,"reminders":true}""";assertTrue(f.create().state.value.deep)}
    @Test fun explicitRecoveryRetainsPreviousValue(){val f=Fixture();f.raw="broken";f.create().recoverExplicitly();assertEquals("broken",f.backup)}
    @Test fun explicitRecoveryRestoresOnlyModes(){val f=Fixture();f.raw="broken";val s=f.create();s.recoverExplicitly();assertEquals(SystemModeState(),s.state.value);assertNull(s.storageError)}
    @Test fun failedWriteDoesNotChangeLiveMode(){val f=Fixture();val s=f.create();f.fail=true;assertThrows(IllegalStateException::class.java){s.silent(true)};assertEquals(SystemModeState(),s.state.value)}
    @Test fun failedWriteDoesNotDestroyPersistedChoice(){val f=Fixture();val s=f.create();s.silent(true);val before=f.raw;f.fail=true;assertThrows(IllegalStateException::class.java){s.shutdown()};assertEquals(before,f.raw)}
    @Test fun failedRecoveryDoesNotPretendNormal(){val f=Fixture();f.raw="broken";val s=f.create();f.fail=true;assertThrows(IllegalStateException::class.java){s.recoverExplicitly()};assertTrue(s.state.value.deep)}
    @Test fun serializedStateContainsNoAccountOrPairingData(){val f=Fixture();f.create().shutdown();val keys=JSONObject(f.raw!!).keySet();assertEquals(setOf("version","silent","deep","wake","reminders"),keys)}
}
