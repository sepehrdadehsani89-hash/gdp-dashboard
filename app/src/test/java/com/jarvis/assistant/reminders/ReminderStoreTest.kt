package com.jarvis.assistant.reminders

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ReminderStoreTest {
    private class Fixture {var raw:String?=null;var exists=false;var now=1000L;var fail=false;fun store()=ReminderStore({raw},{exists},{check(!fail);raw=it;exists=true},{now})}
    private fun ReminderStore.create(title:String="Read notes",at:Long=61_000,id:String?=null)=create(title,at,"UTC",id)
    @Test fun emptyStoreHasNoReminders(){assertTrue(Fixture().store().list().isEmpty())}
    @Test fun createdReminderPersistsAcrossRestart(){val f=Fixture();val row=f.store().create();assertEquals(row,f.store().list().single())}
    @Test fun futureTimeIsValidated(){assertThrows(IllegalArgumentException::class.java){Fixture().store().create(at=999)}}
    @Test fun pastYearLimitIsEnforced(){assertThrows(IllegalArgumentException::class.java){Fixture().store().create(at=1000+REMINDER_YEAR+1)}}
    @Test fun blankTitleIsNotSaved(){val f=Fixture();assertThrows(IllegalArgumentException::class.java){f.store().create(title=" ")};assertNull(f.raw)}
    @Test fun controlsCannotBeSavedAsTitle(){assertThrows(IllegalArgumentException::class.java){Fixture().store().create(title="private\ntext")}}
    @Test fun invalidZoneCannotBeSaved(){assertThrows(Exception::class.java){Fixture().store().create("title",61_000,"No/SuchZone")}}
    @Test fun newReminderIsScheduledNotDelivered(){assertEquals(ReminderStatus.SCHEDULED,Fixture().store().create().status)}
    @Test fun editsKeepStableIdentityAndRotateRevision(){val s=Fixture().store();val a=s.create();val b=s.create(title="Edited",id=a.id);assertEquals(a.id,b.id);assertNotEquals(a.revision,b.revision);assertEquals(1,s.list().size)}
    @Test fun staleWorkerCannotClaimEditedReminder(){val s=Fixture().store();val a=s.create();s.create(id=a.id);assertFalse(s.mark(a.id,a.revision,ReminderStatus.DELIVERING,ReminderStatus.SCHEDULED))}
    @Test fun workerClaimIsSingleUse(){val s=Fixture().store();val a=s.create();assertTrue(s.mark(a.id,a.revision,ReminderStatus.DELIVERING,ReminderStatus.SCHEDULED));assertFalse(s.mark(a.id,a.revision,ReminderStatus.DELIVERING,ReminderStatus.SCHEDULED))}
    @Test fun deliveryMarkRequiresClaimedRevision(){val s=Fixture().store();val a=s.create();assertFalse(s.mark(a.id,a.revision,ReminderStatus.NOTIFIED,ReminderStatus.DELIVERING))}
    @Test fun deletingReminderPreventsInflightNotification(){val s=Fixture().store();val a=s.create();s.mark(a.id,a.revision,ReminderStatus.DELIVERING);s.remove(a.id);var shown=false;assertFalse(s.ifCurrent(a.id,a.revision,ReminderStatus.DELIVERING){shown=true});assertFalse(shown)}
    @Test fun currentClaimAllowsExactlyItsOwnSideEffect(){val s=Fixture().store();val a=s.create();s.mark(a.id,a.revision,ReminderStatus.DELIVERING);var shown=false;assertTrue(s.ifCurrent(a.id,a.revision,ReminderStatus.DELIVERING){shown=true});assertTrue(shown)}
    @Test fun unknownDeleteDoesNotChangeStorage(){val f=Fixture();f.store().create();val before=f.raw;assertFalse(f.store().remove(UUID.randomUUID().toString()));assertEquals(before,f.raw)}
    @Test fun editUnknownIdentityDoesNotOverwrite(){val f=Fixture();f.store().create();val before=f.raw;assertThrows(IllegalArgumentException::class.java){f.store().create(id=UUID.randomUUID().toString())};assertEquals(before,f.raw)}
    @Test fun corruptStoreIsPreserved(){val f=Fixture();f.raw="broken";f.exists=true;assertThrows(Exception::class.java){f.store().create()};assertEquals("broken",f.raw)}
    @Test fun unavailableDecryptionIsNotEmptyStore(){val f=Fixture();f.exists=true;assertThrows(IllegalStateException::class.java){f.store().create()};assertNull(f.raw)}
    @Test fun failedWriteDoesNotPretendToSave(){val f=Fixture();f.fail=true;assertThrows(IllegalStateException::class.java){f.store().create()};assertNull(f.raw)}
    @Test fun upcomingOnlyContainsScheduledFutureEntries(){val f=Fixture();val s=f.store();val a=s.create(at=61_000);val b=s.create(at=121_000);s.mark(b.id,b.revision,ReminderStatus.NOTIFIED);assertEquals(listOf(a),s.upcoming(200_000))}
    @Test fun upcomingDoesNotIncludeExpiredEvents(){val f=Fixture();val s=f.store();s.create();f.now=70_000;assertTrue(s.upcoming(200_000).isEmpty())}
    @Test fun dueRequiresCorrectRevision(){val s=Fixture().store();val a=s.create();assertFalse(ReminderPolicy.due(a,UUID.randomUUID().toString(),100_000))}
    @Test fun futureEntryIsNotDue(){val a=Fixture().store().create();assertFalse(ReminderPolicy.due(a,a.revision,1000))}
    @Test fun notifiedEntryIsNotDueAgain(){val a=Fixture().store().create().copy(status=ReminderStatus.NOTIFIED);assertFalse(ReminderPolicy.due(a,a.revision,100_000))}
    @Test fun diagnosticsDoNotContainPrivateReminderTitle(){assertFalse(Fixture().store().create(title="My private project").toString().contains("My private project"))}
    @Test fun clockRecoveryCannotOverwriteCompletedDelivery(){val s=Fixture().store();val row=s.create();s.mark(row.id,row.revision,ReminderStatus.NOTIFIED);assertFalse(s.mark(row.id,row.revision,ReminderStatus.MISSED,ReminderStatus.SCHEDULED));assertEquals(ReminderStatus.NOTIFIED,s.list().single().status)}
    @Test fun interruptedClaimCanBeMarkedUnconfirmedWithoutSuccess(){val s=Fixture().store();val row=s.create();s.mark(row.id,row.revision,ReminderStatus.DELIVERING);assertTrue(s.mark(row.id,row.revision,ReminderStatus.DELIVERY_UNCONFIRMED,ReminderStatus.DELIVERING));assertNotEquals(ReminderStatus.NOTIFIED,s.list().single().status)}
    @Test fun deletionWaitsForClaimedSideEffectAndThenRemovesRow(){val s=Fixture().store();val row=s.create();val entered=java.util.concurrent.CountDownLatch(1);val release=java.util.concurrent.CountDownLatch(1);val pool=java.util.concurrent.Executors.newFixedThreadPool(2)
        try{val notify=pool.submit<Boolean>{s.ifCurrent(row.id,row.revision,ReminderStatus.SCHEDULED){entered.countDown();check(release.await(2,java.util.concurrent.TimeUnit.SECONDS));s.mark(row.id,row.revision,ReminderStatus.NOTIFIED,ReminderStatus.SCHEDULED)}};assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS));val deletion=pool.submit<Boolean>{s.remove(row.id)};assertFalse(deletion.isDone);release.countDown();assertTrue(notify.get(2,java.util.concurrent.TimeUnit.SECONDS));assertTrue(deletion.get(2,java.util.concurrent.TimeUnit.SECONDS));assertTrue(s.list().isEmpty())}finally{release.countDown();pool.shutdownNow()}}

}
