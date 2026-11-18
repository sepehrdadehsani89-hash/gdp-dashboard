package com.jarvis.assistant.google

import com.jarvis.assistant.brain.ProviderConfig
import com.jarvis.assistant.brain.ProviderCredentialMode
import org.junit.Assert.*
import org.junit.Test

class GoogleSummaryPolicyTest {
    private val now=1700000000000L;private val provider=ProviderConfig("openai","gpt-test")
    private val result=GoogleReadResult(GoogleProduct.GMAIL,"Actual authorized private excerpt")
    private val binding="a".repeat(64)
    private fun review(r:GoogleReadResult=result,p:ProviderConfig=provider)=GoogleSummaryPolicy.review(r,"account-one",3,p,now,binding)
    private fun check(v:GoogleSummaryReview=review(),r:GoogleReadResult=result,account:String="account-one",epoch:Long=3,p:ProviderConfig=provider,at:Long=now,unlocked:Boolean=true,confirmed:Boolean=true,key:String=binding)=GoogleSummaryPolicy.requireAllowed(v,r,account,epoch,p,at,unlocked,confirmed,key)
    @Test fun explicitCurrentReadCanBeSummarized(){check()}
    @Test fun noConfirmationMeansNoSharing(){assertThrows(IllegalArgumentException::class.java){check(confirmed=false)}}
    @Test fun lockMeansNoSharing(){assertThrows(IllegalArgumentException::class.java){check(unlocked=false)}}
    @Test fun accountSwitchInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(account="account-two")}}
    @Test fun grantEpochChangeInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(epoch=4)}}
    @Test fun providerSwitchInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(p=ProviderConfig("gemini","gemini-test"))}}
    @Test fun modelSwitchInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(p=provider.copy(model="other-model"))}}
    @Test fun credentialModeSwitchInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(p=provider.copy(mode=ProviderCredentialMode.CHATGPT_PLAN))}}
    @Test fun providerAccountChangeInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(key="b".repeat(64))}}
    @Test fun privateExcerptChangeInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(r=GoogleReadResult(GoogleProduct.GMAIL,"A different private excerpt"))}}
    @Test fun productSwitchInvalidatesReview(){assertThrows(IllegalArgumentException::class.java){check(r=GoogleReadResult(GoogleProduct.DRIVE,result.summary))}}
    @Test fun expiredReviewCannotAuthorizeARequest(){assertThrows(IllegalArgumentException::class.java){check(at=now+300001)}}
    @Test fun backwardClockCannotReviveAReview(){assertThrows(IllegalArgumentException::class.java){check(at=now-1)}}
    @Test fun exactReviewExpiryBoundaryIsHandled(){check(at=now+300000)}
    @Test fun emptySourceIsNotReviewed(){assertThrows(IllegalArgumentException::class.java){review(GoogleReadResult(GoogleProduct.GMAIL,""))}}
    @Test fun unsupportedConsumerPhotosAccessIsNotInvented(){assertThrows(IllegalArgumentException::class.java){review(GoogleReadResult(GoogleProduct.PHOTOS,"decorative"))}}
    @Test fun mapsHistoryIsNotInvented(){assertThrows(IllegalArgumentException::class.java){review(GoogleReadResult(GoogleProduct.MAPS,"decorative"))}}
    @Test fun consumerKeepAccessIsNotInvented(){assertThrows(IllegalArgumentException::class.java){review(GoogleReadResult(GoogleProduct.KEEP,"decorative"))}}
    @Test fun excerptIsBounded(){assertEquals(8000,GoogleSummaryPolicy.excerpt(GoogleReadResult(GoogleProduct.GMAIL,"x".repeat(20000))).length)}
    @Test fun excerptIsActualSourceText(){assertEquals(result.summary,GoogleSummaryPolicy.excerpt(result))}
    @Test fun promptTreatsMailInstructionsAsUntrusted(){val p=GoogleSummaryPolicy.prompt(result);assertTrue(p.contains("untrusted data"));assertTrue(p.contains("do not request tools"));assertTrue(p.contains(result.summary))}
    @Test fun reviewDiagnosticsDoNotRevealPrivateContent(){val s=review().toString();assertFalse(s.contains(result.summary));assertFalse(s.contains("account-one"));assertFalse(s.contains(binding))}
    @Test fun localBrainIsAnExplicitSeparateDestination(){val p=ProviderConfig("local","qwen-test",ProviderCredentialMode.LOCAL);val v=review(p=p);check(v=v,p=p)}
    @Test fun emptyCredentialBindingCannotBeApproved(){assertThrows(IllegalArgumentException::class.java){GoogleSummaryPolicy.review(result,"account-one",3,provider,now,"")}}
}
