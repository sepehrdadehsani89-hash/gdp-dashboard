package com.jarvis.assistant.notifications

import org.junit.Assert.*
import org.junit.Test

class NotificationAwarenessPolicyTest {
    private val self="com.jarvis.next.phoenix"
    private val whatsapp="com.whatsapp"
    private fun row(pkg:String=whatsapp,title:String="Sender",text:String="Actual preview",at:Long=1,secret:Boolean=false,group:Boolean=false)=NotificationPreview(pkg,title,text,at,secret,group)
    @Test fun noSourceIsReadWithoutExplicitConsent(){assertTrue(NotificationAwarenessPolicy.previews(listOf(row()),self,emptySet()).isEmpty())}
    @Test fun allowedSourceIsRead(){assertEquals(1,NotificationAwarenessPolicy.previews(listOf(row()),self,setOf(whatsapp)).size)}
    @Test fun consentDoesNotExtendToOtherApps(){assertTrue(NotificationAwarenessPolicy.previews(listOf(row("com.instagram.android")),self,setOf(whatsapp)).isEmpty())}
    @Test fun protectedBankAppsStayExcludedEvenIfConfigured(){assertFalse(NotificationAwarenessPolicy.allowed("com.example.banking",self,setOf("com.example.banking")))}
    @Test fun systemUIStaysExcluded(){assertFalse(NotificationAwarenessPolicy.allowed("com.android.systemui",self,setOf("com.android.systemui")))}
    @Test fun ownNotificationsAreNeverSummarized(){assertFalse(NotificationAwarenessPolicy.allowed(self,self,setOf(self)))}
    @Test fun secretVisibilityIsRespected(){assertTrue(NotificationAwarenessPolicy.previews(listOf(row(secret=true)),self,setOf(whatsapp)).isEmpty())}
    @Test fun groupedSummaryDoesNotDuplicateConversationPreviews(){assertTrue(NotificationAwarenessPolicy.previews(listOf(row(group=true)),self,setOf(whatsapp)).isEmpty())}
    @Test fun blankEntriesDoNotBecomeMessages(){assertTrue(NotificationAwarenessPolicy.previews(listOf(row(title="",text="")),self,setOf(whatsapp)).isEmpty())}
    @Test fun missingTitleDoesNotHideActualBody(){assertEquals("Actual preview",NotificationAwarenessPolicy.previews(listOf(row(title="")),self,setOf(whatsapp)).single().text)}
    @Test fun privateCodesAreRedacted(){val r=NotificationAwarenessPolicy.previews(listOf(row(text="verification code: 123456")),self,setOf(whatsapp)).single();assertFalse(r.text.contains("123456"))}
    @Test fun paymentCardNumbersAreRedacted(){val r=NotificationAwarenessPolicy.previews(listOf(row(text="card 4111111111111111")),self,setOf(whatsapp)).single();assertFalse(r.text.contains("4111111111111111"))}
    @Test fun readsAreBounded(){assertEquals(8,NotificationAwarenessPolicy.previews((1..20).map{row(at=it.toLong())},self,setOf(whatsapp)).size)}
    @Test fun mostRecentActivePreviewsAreFirst(){val r=NotificationAwarenessPolicy.previews(listOf(row(at=1),row(at=3),row(at=2)),self,setOf(whatsapp));assertEquals(listOf(3L,2L,1L),r.map{it.at})}
    @Test fun longBodiesAndTitlesAreBounded(){val r=NotificationAwarenessPolicy.previews(listOf(row(title="t".repeat(500),text="x".repeat(1000))),self,setOf(whatsapp)).single();assertEquals(120,r.title.length);assertEquals(220,r.text.length)}
    @Test fun targetedRequestCannotReadAnotherAllowedApp(){val r=NotificationAwarenessPolicy.previews(listOf(row(),row("com.instagram.android")),self,setOf(whatsapp,"com.instagram.android"),setOf(whatsapp));assertEquals(listOf(whatsapp),r.map{it.packageId})}
    @Test fun disconnectedListenerIsNotAnEmptyInbox(){val r=NotificationAwarenessPolicy.result(NotificationReadStatus.DISCONNECTED);assertTrue(r.message.contains("does not mean you have no messages"));assertTrue(r.previews.isEmpty())}
    @Test fun availableEmptySnapshotDoesNotClaimUnreadZero(){assertTrue(NotificationAwarenessPolicy.result(NotificationReadStatus.AVAILABLE).message.contains("not an unread message count"))}
    @Test fun availablePreviewCountIsNotTotalUnread(){assertTrue(NotificationAwarenessPolicy.result(NotificationReadStatus.AVAILABLE,listOf("Message")).message.contains("not a total unread count"))}
    @Test fun sourceCommandIsDetectedPrecisely(){assertEquals("WhatsApp",NotificationAwarenessPolicy.source("Hey Jarvis, catch me up on WhatsApp."))}
    @Test fun appLaunchDoesNotBecomeMessageRead(){assertNull(NotificationAwarenessPolicy.source("Open WhatsApp"))}
    @Test fun questionAboutAccessDoesNotReadMessages(){assertNull(NotificationAwarenessPolicy.source("How do I read my WhatsApp notifications?"))}
    @Test fun negativeRequestIsNotMessageRead(){assertNull(NotificationAwarenessPolicy.source("Do not read my WhatsApp notifications"))}
    @Test fun diagnosticDoesNotLeakPreviews(){assertFalse(NotificationAwarenessPolicy.result(NotificationReadStatus.AVAILABLE,listOf("private message")).toString().contains("private message"))}
    @Test fun generalMessageCatchupUsesActiveNotifications(){assertTrue(NotificationAwarenessPolicy.request("Jarvis, catch me up on my messages."))}
    @Test fun negatedGeneralCatchupDoesNotRead(){assertFalse(NotificationAwarenessPolicy.request("Do not catch me up on my messages"))}
    @Test fun spanishMessageCatchupIsAnExplicitRead(){assertTrue(NotificationAwarenessPolicy.request("Resume mis mensajes"))}

}
