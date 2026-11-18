package com.jarvis.assistant.google

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class GoogleConnectionsStoreTest {
    private class Fixture {var raw:String?=null;var exists=false;var failWrite=false;var now=1000L
        fun store()=GoogleConnectionsStore({raw},{exists},{check(!failWrite);raw=it;exists=true},{now})}
    private val gmail=GoogleServicesPolicy.definition(GoogleProduct.GMAIL).scope!!
    private fun Fixture.granted():Pair<GoogleConnectionsStore,GoogleAccountConnection>{val s=store();val row=s.addAccount("owner@example.com");s.grant(row.id,GoogleProduct.GMAIL,listOf(gmail));return s to row}
    @Test fun emptyStorageIsUnconnected(){assertNull(Fixture().store().selected())}
    @Test fun metadataSurvivesRestartWithoutClaimingVerification(){val f=Fixture();val(s,row)=f.granted();s.markVerified(row.id,GoogleProduct.GMAIL,s.epoch);val restored=f.store();assertEquals(setOf(GoogleProduct.GMAIL),restored.selected()!!.services);assertFalse(restored.currentlyVerified(row.id,GoogleProduct.GMAIL));assertEquals(1000L,restored.selected()!!.lastVerified[GoogleProduct.GMAIL])}
    @Test fun addingAnAccountDoesNotGrantAService(){val s=Fixture().store();assertTrue(s.addAccount("a@example.com").services.isEmpty())}
    @Test fun identitiesAreCaseInsensitive(){assertEquals(GoogleServicesPolicy.accountId("A@example.com"),GoogleServicesPolicy.accountId("a@example.com"))}
    @Test fun accountsRemainSeparate(){val s=Fixture().store();val a=s.addAccount("a@example.com");s.grant(a.id,GoogleProduct.GMAIL,listOf(gmail));val b=s.addAccount("b@example.com");assertTrue(b.services.isEmpty());s.select(a.id);assertEquals(setOf(GoogleProduct.GMAIL),s.selected()!!.services)}
    @Test fun wrongScopeDoesNotGrantMail(){val f=Fixture();val s=f.store();val a=s.addAccount("a@example.com");assertThrows(IllegalArgumentException::class.java){s.grant(a.id,GoogleProduct.GMAIL,listOf("profile"))};assertTrue(s.selected()!!.services.isEmpty())}
    @Test fun pendingConsentCannotGrantToAnUnselectedAccount(){val s=Fixture().store();val a=s.addAccount("a@example.com");s.addAccount("b@example.com");assertThrows(IllegalStateException::class.java){s.grant(a.id,GoogleProduct.GMAIL,listOf(gmail))}}
    @Test fun APIlessServicesCannotBeMarkedAuthorized(){val s=Fixture().store();val a=s.addAccount("a@example.com");assertThrows(IllegalArgumentException::class.java){s.grant(a.id,GoogleProduct.KEEP,listOf("profile"))}}
    @Test fun aStaleReadCannotVerifyAfterAccountSwitch(){val f=Fixture();val(s,a)=f.granted();val epoch=s.epoch;s.addAccount("b@example.com");assertThrows(IllegalStateException::class.java){s.markVerified(a.id,GoogleProduct.GMAIL,epoch)}}
    @Test fun aRemovedServiceCannotBeVerifiedByAnInflightRead(){val f=Fixture();val(s,a)=f.granted();val epoch=s.epoch;s.removeService(a.id,GoogleProduct.GMAIL);assertThrows(IllegalStateException::class.java){s.markVerified(a.id,GoogleProduct.GMAIL,epoch)}}
    @Test fun onlyActualReadAddsCurrentVerification(){val f=Fixture();val(s,a)=f.granted();assertFalse(s.currentlyVerified(a.id,GoogleProduct.GMAIL));s.markVerified(a.id,GoogleProduct.GMAIL,s.epoch);assertTrue(s.currentlyVerified(a.id,GoogleProduct.GMAIL))}
    @Test fun verificationExpiresAfterAnHour(){val f=Fixture();val(s,a)=f.granted();s.markVerified(a.id,GoogleProduct.GMAIL,s.epoch);f.now+=3_600_001;assertFalse(s.currentlyVerified(a.id,GoogleProduct.GMAIL))}
    @Test fun clockRollbackDoesNotKeepGreenStatus(){val f=Fixture();val(s,a)=f.granted();s.markVerified(a.id,GoogleProduct.GMAIL,s.epoch);f.now=0;assertFalse(s.currentlyVerified(a.id,GoogleProduct.GMAIL))}
    @Test fun failedReadClearsGreenWithoutDeletingGrant(){val f=Fixture();val(s,a)=f.granted();s.markVerified(a.id,GoogleProduct.GMAIL,s.epoch);s.markFailure(a.id,GoogleProduct.GMAIL);assertFalse(s.currentlyVerified(a.id,GoogleProduct.GMAIL));assertTrue(GoogleProduct.GMAIL in s.selected()!!.services)}
    @Test fun localRemovalPreservesOtherAccounts(){val s=Fixture().store();val a=s.addAccount("a@example.com");val b=s.addAccount("b@example.com");s.removeAccount(a.id);assertEquals(b.id,s.selected()!!.id);assertEquals(1,s.state.value.accounts.size)}
    @Test fun removeSelectedPicksRemainingMetadataOnly(){val s=Fixture().store();val a=s.addAccount("a@example.com");val b=s.addAccount("b@example.com");s.removeAccount(b.id);assertEquals(a.id,s.selected()!!.id)}
    @Test fun noGrantRemainsAfterLastRemoval(){val f=Fixture();val(s,a)=f.granted();s.removeAccount(a.id);assertNull(f.store().selected())}
    @Test fun corruptStorageIsNotOverwritten(){val f=Fixture();f.raw="invalid";f.exists=true;val s=f.store();assertNotNull(s.state.value.error);assertThrows(Exception::class.java){s.addAccount("a@example.com")};assertEquals("invalid",f.raw)}
    @Test fun undecryptableStorageIsNotOverwritten(){val f=Fixture();f.exists=true;val s=f.store();assertNotNull(s.state.value.error);assertThrows(IllegalStateException::class.java){s.addAccount("a@example.com")};assertNull(f.raw)}
    @Test fun storageFailureDoesNotPublishUnsavedConnection(){val f=Fixture();val s=f.store();f.failWrite=true;assertThrows(IllegalStateException::class.java){s.addAccount("a@example.com")};assertTrue(s.state.value.accounts.isEmpty())}
    @Test fun accountLimitIsEnforcedBeforeOverwriting(){val f=Fixture();val s=f.store();repeat(10){s.addAccount("a$it@example.com")};val before=f.raw;assertThrows(IllegalStateException::class.java){s.addAccount("other@example.com")};assertEquals(before,f.raw)}
    @Test fun noCredentialsOrMailAreSerialized(){val f=Fixture();f.granted();val keys=JSONObject(f.raw!!).getJSONArray("accounts").getJSONObject(0).keys().asSequence().toSet();assertEquals(setOf("id","email","teacherEmail","services","lastVerified"),keys);assertFalse(f.raw!!.contains("Bearer"))}
    @Test fun savedSenderFilterSurvivesRestart(){val f=Fixture();val(s,a)=f.granted();s.setTeacher(a.id,"teacher@example.com");assertEquals("teacher@example.com",f.store().selected()!!.teacherEmail)}
    @Test fun senderCanBeClearedWithoutRemovingMailGrant(){val f=Fixture();val(s,a)=f.granted();s.setTeacher(a.id,"teacher@example.com");s.setTeacher(a.id,"");assertNull(s.selected()!!.teacherEmail);assertTrue(GoogleProduct.GMAIL in s.selected()!!.services)}
    @Test fun senderQuerySyntaxCannotBeInjected(){val f=Fixture();val(s,a)=f.granted();assertThrows(IllegalArgumentException::class.java){s.setTeacher(a.id,"\"a\"@example.com")}}
    @Test fun addingSameAccountDoesNotDuplicateIt(){val s=Fixture().store();s.addAccount("A@example.com");s.addAccount("a@example.com");assertEquals(1,s.state.value.accounts.size)}
    @Test fun diagnosticStringsDoNotRevealEmail(){assertFalse(Fixture().store().addAccount("private@example.com").toString().contains("private@example.com"))}
}
