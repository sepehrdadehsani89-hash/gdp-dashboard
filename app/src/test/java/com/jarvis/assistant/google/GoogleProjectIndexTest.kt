package com.jarvis.assistant.google

import org.junit.Assert.*
import org.junit.Test

class GoogleProjectIndexTest {
    private class Fixture {var raw:String?=null;var exists=false;var fail=false;var now=1000L
        fun index()=GoogleProjectIndex({raw},{exists},{check(!fail);raw=it;exists=true},{now})}
    private val a=GoogleServicesPolicy.accountId("a@example.com")
    private val b=GoogleServicesPolicy.accountId("b@example.com")
    private fun page(product:GoogleProduct=GoogleProduct.DRIVE,text:String="Actual file metadata")=GoogleReadResult(product,text)
    @Test fun emptyIndexDoesNotInventProjects(){val s=Fixture().index();assertTrue(s.list(a).isEmpty());assertTrue(s.summarize(a).contains("No Google project snapshots"))}
    @Test fun snapshotPersistsOnlyAfterExplicitIndexCall(){val f=Fixture();val s=f.index();assertNull(f.raw);s.index(a,page());assertEquals("Actual file metadata",f.index().list(a).single().summary)}
    @Test fun accountPartitionsAreSeparate(){val s=Fixture().index();s.index(a,page());assertTrue(s.list(b).isEmpty())}
    @Test fun accountDeletePreservesOthers(){val s=Fixture().index();s.index(a,page());s.index(b,page());s.delete(a);assertEquals(1,s.list(b).size);assertTrue(s.list(a).isEmpty())}
    @Test fun serviceDeletePreservesOtherServices(){val s=Fixture().index();s.index(a,page());s.index(a,page(GoogleProduct.TASKS));s.delete(a,GoogleProduct.DRIVE);assertEquals(GoogleProduct.TASKS,s.list(a).single().product)}
    @Test fun refreshReplacesOnlyThatAccountsServiceSnapshot(){val s=Fixture().index();s.index(a,page());s.index(a,page(text="New metadata"));assertEquals(1,s.list(a).size);assertEquals("New metadata",s.list(a).single().summary)}
    @Test fun sourceTimestampIsRecorded(){val f=Fixture();val s=f.index();f.now=2222;s.index(a,page());assertEquals(2222L,s.list(a).single().at)}
    @Test fun summariesDiscloseStaleness(){val s=Fixture().index();s.index(a,page());assertTrue(s.summarize(a).contains("may be stale"));assertTrue(s.summarize(a).contains("no live refresh"))}
    @Test fun gmailIsNotIndexedByProjectActions(){assertThrows(IllegalArgumentException::class.java){Fixture().index().index(a,page(GoogleProduct.GMAIL))}}
    @Test fun contactsAreNotIndexedByProjectActions(){assertThrows(IllegalArgumentException::class.java){Fixture().index().index(a,page(GoogleProduct.CONTACTS))}}
    @Test fun chatIsNotIndexedByProjectActions(){assertThrows(IllegalArgumentException::class.java){Fixture().index().index(a,page(GoogleProduct.CHAT))}}
    @Test fun mapsAreNotIndexedByProjectActions(){assertThrows(IllegalArgumentException::class.java){Fixture().index().index(a,page(GoogleProduct.MAPS))}}
    @Test fun docsExplicitSnapshotIsAllowed(){val s=Fixture().index();s.index(a,page(GoogleProduct.DOCS));assertEquals(GoogleProduct.DOCS,s.list(a).single().product)}
    @Test fun sheetsExplicitSnapshotIsAllowed(){val s=Fixture().index();s.index(a,page(GoogleProduct.SHEETS));assertEquals(1,s.list(a).size)}
    @Test fun slidesExplicitSnapshotIsAllowed(){val s=Fixture().index();s.index(a,page(GoogleProduct.SLIDES));assertEquals(1,s.list(a).size)}
    @Test fun calendarExplicitSnapshotIsAllowed(){val s=Fixture().index();s.index(a,page(GoogleProduct.CALENDAR));assertEquals(1,s.list(a).size)}
    @Test fun snapshotContentIsBounded(){val s=Fixture().index();s.index(a,page(text="a".repeat(100_000)));assertEquals(6000,s.list(a).single().summary.length)}
    @Test fun invalidAccountIdCannotWriteSnapshot(){val f=Fixture();assertThrows(IllegalArgumentException::class.java){f.index().index("a@example.com",page())};assertNull(f.raw)}
    @Test fun corruptExistingIndexIsPreserved(){val f=Fixture();f.raw="broken";f.exists=true;assertThrows(Exception::class.java){f.index().index(a,page())};assertEquals("broken",f.raw)}
    @Test fun undecryptableExistingIndexIsPreserved(){val f=Fixture();f.exists=true;assertThrows(IllegalStateException::class.java){f.index().delete(a)};assertNull(f.raw)}
    @Test fun failedWriteDoesNotPretendToSave(){val f=Fixture();f.fail=true;assertThrows(IllegalStateException::class.java){f.index().index(a,page())};assertNull(f.raw)}
    @Test fun deletingLastSnapshotLeavesAnExplicitEmptyIndex(){val f=Fixture();val s=f.index();s.index(a,page());s.delete(a);assertEquals("[]",f.raw)}
}
