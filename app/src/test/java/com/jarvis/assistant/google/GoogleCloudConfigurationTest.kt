package com.jarvis.assistant.google

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GoogleCloudConfigurationTest {
    private class Fixture{var raw:String?=null;var present=false;var failWrite=false;var removed=false;val store=GoogleCloudConfigurationStore({raw},{present},{if(failWrite)error("disk");raw=it;present=true},{removed=true;raw=null;present=false})}
    private val valid=GoogleCloudConfiguration("jarvis-project-123","1234567890-abcdefghijklmnop.apps.googleusercontent.com","AIza"+"T".repeat(35))
    @Test fun initialStateIsConfigurationOnly(){assertEquals(GoogleCloudConfiguration(),Fixture().store.load())}
    @Test fun validProjectCanBeSaved(){val f=Fixture();f.store.save(valid);assertEquals(valid.projectId,f.store.load().projectId)}
    @Test fun apiKeyCanBeConfiguredWithoutAnOAuthSecret(){val f=Fixture();f.store.save(valid.copy(projectId="",androidClientId=""));assertEquals(valid.apiKey,f.store.load().apiKey)}
    @Test fun projectMinimumLengthEnforced(){assertThrows(IllegalArgumentException::class.java){valid.copy(projectId="short").validated()}}
    @Test fun projectMaximumLengthEnforced(){assertThrows(IllegalArgumentException::class.java){valid.copy(projectId="a".repeat(31)).validated()}}
    @Test fun projectUppercaseRejected(){assertThrows(IllegalArgumentException::class.java){valid.copy(projectId="Jarvis-project").validated()}}
    @Test fun projectCannotStartWithDigit(){assertThrows(IllegalArgumentException::class.java){valid.copy(projectId="1jarvis-project").validated()}}
    @Test fun projectCannotEndWithHyphen(){assertThrows(IllegalArgumentException::class.java){valid.copy(projectId="jarvis-project-").validated()}}
    @Test fun projectCannotContainAUrl(){assertThrows(IllegalArgumentException::class.java){valid.copy(projectId="https://project.example").validated()}}
    @Test fun clientIdUsesGoogleSuffix(){assertThrows(IllegalArgumentException::class.java){valid.copy(androidClientId="1234567890-abcdefghijklmnop.example.com").validated()}}
    @Test fun clientSecretIsNotAcceptedAsClientId(){assertThrows(IllegalArgumentException::class.java){valid.copy(androidClientId="GOCSPX-not-a-client-id").validated()}}
    @Test fun googlePasswordIsNotAnApiKey(){assertThrows(IllegalArgumentException::class.java){valid.copy(apiKey="not-a-password").validated()}}
    @Test fun oauthAccessTokenIsNotAnApiKey(){assertThrows(IllegalArgumentException::class.java){valid.copy(apiKey="ya29.example-access-token").validated()}}
    @Test fun apiKeyWrongLengthRejected(){assertThrows(IllegalArgumentException::class.java){valid.copy(apiKey="AIza"+"A".repeat(34)).validated()}}
    @Test fun apiKeyControlsRejected(){assertThrows(IllegalArgumentException::class.java){valid.copy(apiKey="AIza"+"A".repeat(34)+"\n").validated()}}
    @Test fun completeConfigurationRoundTrips(){val f=Fixture();f.store.save(valid);assertEquals(valid,f.store.load())}
    @Test fun undecryptableExistingConfigurationIsPreserved(){val f=Fixture();f.present=true;assertThrows(IllegalStateException::class.java){f.store.save(valid)};assertNull(f.raw)}
    @Test fun malformedExistingConfigurationIsPreserved(){val f=Fixture();f.raw="bad";f.present=true;assertThrows(Exception::class.java){f.store.save(valid)};assertEquals("bad",f.raw)}
    @Test fun unsupportedVersionIsPreserved(){val f=Fixture();f.store.save(valid);f.raw=JSONObject(f.raw!!).put("version",2).toString();val before=f.raw;assertThrows(IllegalArgumentException::class.java){f.store.save(valid)};assertEquals(before,f.raw)}
    @Test fun writeFailurePreservesPreviousValues(){val f=Fixture();f.store.save(valid);val before=f.raw;f.failWrite=true;assertThrows(IllegalStateException::class.java){f.store.save(valid.copy(projectId="new-project"))};assertEquals(before,f.raw)}
    @Test fun explicitRemoveOnlyTouchesThisStore(){val f=Fixture();f.store.save(valid);f.store.clear();assertTrue(f.removed);assertEquals(GoogleCloudConfiguration(),f.store.load())}
    @Test fun diagnosticStringDoesNotRevealApiKey(){assertFalse(valid.toString().contains(valid.apiKey));assertFalse(valid.toString().contains(valid.androidClientId))}
}
