package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class AndroidFinalRecognitionTest {
    private fun kind(text:String="open the clock",score:Float?=.8f,threshold:Float=.55f)=AndroidFinalRecognition.kind(text,score,threshold)
    @Test fun missingConfidenceDoesNotInventAMeasurement(){assertEquals(FinalRecognitionKind.ACCEPTED,kind(score=null))}
    @Test fun androidUnavailableSentinelIsPreserved(){assertEquals(FinalRecognitionKind.ACCEPTED,kind(score=-1f))}
    @Test fun emptyFinalCannotCreateACommand(){assertEquals(FinalRecognitionKind.EMPTY,kind(text=""))}
    @Test fun whitespaceFinalCannotCreateACommand(){assertEquals(FinalRecognitionKind.EMPTY,kind(text=" \n "))}
    @Test fun suppliedHighConfidenceFinalIsAccepted(){assertEquals(FinalRecognitionKind.ACCEPTED,kind(score=.94f))}
    @Test fun exactThresholdIsAccepted(){assertEquals(FinalRecognitionKind.ACCEPTED,kind(score=.55f))}
    @Test fun belowThresholdRequiresRetry(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=.54f))}
    @Test fun suppliedZeroIsLowConfidenceNotMissing(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=0f))}
    @Test fun suppliedOneIsAValidScore(){assertEquals(FinalRecognitionKind.ACCEPTED,kind(score=1f))}
    @Test fun nonSentinelNegativeScoreFailsClosed(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=-.5f))}
    @Test fun excessiveScoreFailsClosed(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=1.1f))}
    @Test fun nanScoreCannotBypassConfidenceGate(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=Float.NaN))}
    @Test fun infiniteScoreCannotBypassConfidenceGate(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=Float.POSITIVE_INFINITY))}
    @Test fun stricterSavedThresholdHasARealEffect(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=.8f,threshold=.9f))}
    @Test fun lowerSavedThresholdHasARealEffect(){assertEquals(FinalRecognitionKind.ACCEPTED,kind(score=.5f,threshold=.45f))}
    @Test fun malformedThresholdDoesNotDisableTheGate(){listOf(Float.NaN,-1f,2f,Float.POSITIVE_INFINITY).forEach{assertEquals(FinalRecognitionKind.UNCERTAIN,kind(threshold=it))}}
    @Test fun missingScoreWithInvalidThresholdStillFailsClosed(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(score=null,threshold=Float.NaN))}
    @Test fun spanishCommandsUseTheSameConfidenceBoundary(){assertEquals(FinalRecognitionKind.UNCERTAIN,kind(text="llama a Alex",score=.2f));assertEquals(FinalRecognitionKind.ACCEPTED,kind(text="abre el reloj",score=.9f))}
    @Test fun metadataLabelsUnavailableConfidenceHonestly(){assertTrue(AndroidFinalRecognition.metadata("question",null,.55f).contains("not supplied by Android"))}
    @Test fun metadataDoesNotExposeRecognizedPrivateContent(){val text="private test name and password";val metadata=AndroidFinalRecognition.metadata(text,.8f,.55f);assertFalse(metadata.contains(text));assertTrue(metadata.contains("80%"))}
}
