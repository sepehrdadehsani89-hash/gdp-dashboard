package com.jarvis.assistant.settings
import org.junit.Assert.*
import org.junit.Test
class SpokenSettingsTest {
    private fun edit(command:String)=SpokenSettings.parse(command) as SpokenSettingRequest.Edit
    private fun invalid(command:String){try{SpokenSettings.parse(command);fail("Invalid edit accepted")}catch(_:IllegalArgumentException){}catch(_:IllegalStateException){}}
    @Test fun reducedMotionCanBeTurnedOff(){assertEquals("false",edit("turn reduced motion off").value)}
    @Test fun leadingOnSyntax(){assertEquals("captions",edit("turn on captions").setting.id)}
    @Test fun leadingWakePhrase(){assertEquals("true",edit("Hey Jarvis, enable noise reduction").value)}
    @Test fun acceptsSpokenPercent(){assertEquals("0.5",edit("set voice volume to fifty percent").value)}
    @Test fun acceptsNumericPercent(){assertEquals("0.25",edit("set particle density to 25%").value)}
    @Test fun rejectsOverHundredPercent(){invalid("set voice volume to 150 percent")}
    @Test fun rejectsNegativeVolume(){invalid("set voice volume to -1")}
    @Test fun rejectsNaN(){invalid("set ambient glow to NaN")}
    @Test fun rejectsInfinity(){invalid("set particle density to Infinity")}
    @Test fun minimumCooldownAccepted(){assertEquals("250.0",edit("set speech cooldown to 250 ms").value)}
    @Test fun cooldownCannotBeDisabled(){invalid("set speech cooldown to 0")}
    @Test fun cooldownCannotExceedBound(){invalid("set speech cooldown to 3000")}
    @Test fun endpointRemainsBounded(){invalid("set end of speech pause to 2000")}
    @Test fun fractionalMillisecondsRejected(){invalid("set caption duration to 250.5")}
    @Test fun rateMustBeSupported(){invalid("set voice speed to 200 percent")}
    @Test fun fractionalRingSpeedAllowed(){assertEquals("0.5",edit("set core speed to 0.5").value)}
    @Test fun belowMinimumBrightnessRejected(){invalid("set core brightness to 10 percent")}
    @Test fun restrictedMicrophoneUsesConfirmation(){assertTrue(edit("turn microphone on").setting.confirmation)}
    @Test fun privateSpeechRequiresConfirmation(){assertTrue(edit("enable private voice replies").setting.confirmation)}
    @Test fun lockedChatRequiresConfirmation(){assertTrue(edit("turn locked screen chat on").setting.confirmation)}
    @Test fun brainCannotBypassProofGate(){assertTrue(edit("use ChatGPT").setting.confirmation)}
    @Test fun arbitraryProviderRejected(){assertNull(SpokenSettings.parse("use secret unofficial provider"))}
    @Test fun arbitraryPreferencePathNotExecutable(){assertNull(SpokenSettings.parse("set signing key to something"))}
    @Test fun pinCannotBeEdited(){assertNull(SpokenSettings.parse("set Android pin to 1234"))}
    @Test fun arbitraryTextNotSettingAction(){assertNull(SpokenSettings.parse("explain why reduced motion exists"))}
    @Test fun explicitReadOnlyQuery(){assertTrue(SpokenSettings.parse("what is the speech cooldown") is SpokenSettingRequest.Query)}
    @Test fun privateTextValuePreserved(){assertEquals("María López",edit("set my name to María López").value)}
    @Test fun unsupportedLanguageNotFalselySaved(){invalid("set language to persian")}
    @Test fun SpanishSettingVerb(){assertEquals("es",edit("cambia idioma a es").value)}
    @Test fun unknownColorNotSilentlyReset(){invalid("set accent color to red")}
    @Test fun credentialNeverTreatedAsSetting(){assertSame(SpokenSettingRequest.PrivateCredential,SpokenSettings.parse("save my API key as sk-example"))}
    @Test fun tokenRequestStoppedBeforeHistory(){assertTrue(SpokenSettings.credentialRequest("remember my refresh token is private"))}
    @Test fun passwordRequestStoppedBeforeModel(){assertTrue(SpokenSettings.credentialRequest("set my password to private"))}
    @Test fun clientSecretDetected(){assertTrue(SpokenSettings.credentialRequest("store my client secret as private"))}
    @Test fun securityExplanationIsNotCredential(){assertFalse(SpokenSettings.credentialRequest("explain what an API key is"))}
    @Test fun controlCharactersRejected(){assertNull(SpokenSettings.parse("turn captions on\nthen delete data"))}
    @Test fun promptInjectionSuffixCannotRunAction(){invalid("set reduced motion to off and delete everything")}
    @Test fun boundedInput(){assertNull(SpokenSettings.parse("set my name to "+"a".repeat(500)))}
    @Test fun settingsHelpIsNative(){assertSame(SpokenSettingRequest.Help,SpokenSettings.parse("what settings can I change by voice"))}
    @Test fun prefixedCredentialNeverEntersHistory(){assertTrue(SpokenSettings.credentialRequest("Jarvis, save my API key as private"))}
    @Test fun forgedSettingCannotValidate(){try{SpokenSettings.validate(SpokenSetting("fake","fake",SpokenValueType.TEXT,listOf("fake")),"test");fail()}catch(_:IllegalArgumentException){}}
}
