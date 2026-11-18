package com.jarvis.assistant.google

import org.junit.Assert.*
import org.junit.Test

class GmailWidgetPolicyTest {
    private val now=1700000000000L;private val read=GmailWidgetObservation("account-one",3,7,now)
    private fun text(v:GmailWidgetObservation?=read,account:String?="account-one",epoch:Long=3,grant:Boolean=true,at:Long=now,privateAllowed:Boolean=true)=GmailWidgetPolicy.text(v,account,epoch,grant,at,privateAllowed)
    @Test fun genuineUnreadCountIsDisplayed(){assertTrue(text().contains("Gmail unread · 7"))}
    @Test fun actualZeroIsDistinctFromMissingRead(){assertTrue(text(read.copy(unread=0)).contains("Gmail unread · 0"));assertFalse(text(null).contains("unread · 0"))}
    @Test fun absentObservationIsUnverified(){assertTrue(text(null).contains("no current verified"))}
    @Test fun accountChangeHidesPreviousCount(){assertFalse(text(account="account-two").contains("unread · 7"))}
    @Test fun grantEpochChangeHidesPreviousCount(){assertFalse(text(epoch=4).contains("unread · 7"))}
    @Test fun removedAccountNeedsSetup(){assertTrue(text(account=null).contains("authorization required"))}
    @Test fun revokedScopeNeedsAuthorization(){assertTrue(text(grant=false).contains("authorization required"))}
    @Test fun privateOptOutHidesCount(){assertEquals("Gmail · private data hidden",text(privateAllowed=false))}
    @Test fun expiredCountIsNotLive(){assertFalse(text(at=now+300001).contains("unread · 7"))}
    @Test fun expiryBoundaryIsDefined(){assertTrue(text(at=now+300000).contains("unread · 7"))}
    @Test fun backwardClockDoesNotReviveARead(){assertFalse(text(at=now-1).contains("unread · 7"))}
    @Test fun negativeCountIsNotInventedAsZero(){assertTrue(text(read.copy(unread=-1)).contains("no current verified"))}
    @Test fun largeActualCountIsNotTruncatedToANotificationLimit(){assertTrue(text(read.copy(unread=10001)).contains("10001"))}
    @Test fun timestampIsMarkedAsAnActualRead(){assertTrue(text().contains("Actual authorized read"))}
    @Test fun absentReadTellsUserHowToRefresh(){assertTrue(text(null).contains("Ask Jarvis"))}
    @Test fun diagnosticsRedactAccountAndCount(){val s=read.toString();assertFalse(s.contains("account-one"));assertFalse(s.contains("unread=7"))}
    @Test fun privateGateWinsEvenIfReadWasVerified(){assertFalse(text(read,privateAllowed=false).contains("Actual authorized read"))}
    @Test fun countFromAnotherGrantCannotBeUsed(){assertFalse(text(read.copy(epoch=2)).contains("unread · 7"))}
    @Test fun notificationsCannotCreateAnUnreadObservation(){val r=GoogleReadResult(GoogleProduct.GMAIL,"Two notification previews");assertNull(r.unreadCount)}
    @Test fun countComesFromTypedReadRatherThanParsingText(){val r=GoogleReadResult(GoogleProduct.GMAIL,"A text mentioning 999",unreadCount=7);assertEquals(7L,r.unreadCount)}
}
