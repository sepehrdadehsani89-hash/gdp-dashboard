package com.jarvis.assistant.voice
import org.junit.Assert.*
import org.junit.Test
class ListeningStartupPolicyTest {
    private fun decide(enabled:Boolean=true,permission:Boolean=true,model:Boolean=true,visible:Boolean=true,locked:Boolean=false,running:Boolean=false,busy:Boolean=false)=ListeningStartupPolicy.decide(enabled,permission,model,visible,locked,running,busy)
    @Test fun approvedVisibleStartup(){assertEquals(ListeningStartupAction.START_WAKE,decide())}
    @Test fun stopChoicePreserved(){assertEquals(ListeningStartupAction.NONE,decide(enabled=false))}
    @Test fun deniedPermissionNoCapture(){assertEquals(ListeningStartupAction.NONE,decide(permission=false))}
    @Test fun revokedPermissionNoRestart(){assertEquals(ListeningStartupAction.NONE,decide(permission=false,model=true))}
    @Test fun backgroundCannotStartMic(){assertEquals(ListeningStartupAction.NONE,decide(visible=false))}
    @Test fun lockedCannotStartMic(){assertEquals(ListeningStartupAction.NONE,decide(locked=true))}
    @Test fun runningServiceNotDuplicated(){assertEquals(ListeningStartupAction.NONE,decide(running=true))}
    @Test fun speechOwnershipPreventsStartup(){assertEquals(ListeningStartupAction.NONE,decide(busy=true))}
    @Test fun missingModelNeedsExplicitDownload(){assertEquals(ListeningStartupAction.MODEL_REQUIRED,decide(model=false))}
    @Test fun missingModelNoPromptBeforeOptIn(){assertEquals(ListeningStartupAction.NONE,decide(enabled=false,model=false))}
    @Test fun missingModelNoPromptBeforePermission(){assertEquals(ListeningStartupAction.NONE,decide(permission=false,model=false))}
    @Test fun missingModelNoBackgroundDialog(){assertEquals(ListeningStartupAction.NONE,decide(visible=false,model=false))}
    @Test fun missingModelNoLockedDialog(){assertEquals(ListeningStartupAction.NONE,decide(locked=true,model=false))}
    @Test fun missingModelNoSpeechOverlap(){assertEquals(ListeningStartupAction.NONE,decide(busy=true,model=false))}
    @Test fun noPromptOverRunningService(){assertEquals(ListeningStartupAction.NONE,decide(running=true,model=false))}
    @Test fun completionCanReleaseStartup(){assertEquals(ListeningStartupAction.NONE,decide(busy=true));assertEquals(ListeningStartupAction.START_WAKE,decide(busy=false))}
    @Test fun leavingForegroundCancelsEligibility(){assertEquals(ListeningStartupAction.START_WAKE,decide());assertEquals(ListeningStartupAction.NONE,decide(visible=false))}
    @Test fun permissionGrantAloneInsufficient(){assertEquals(ListeningStartupAction.NONE,decide(enabled=false,permission=true))}
    @Test fun installedBrainDoesNotReplaceSpeechModel(){assertEquals(ListeningStartupAction.MODEL_REQUIRED,decide(model=false))}
    @Test fun everyUnsafeCombinationFailsClosed(){for(bits in 0 until 128){val e=bits and 1!=0;val p=bits and 2!=0;val m=bits and 4!=0;val v=bits and 8!=0;val l=bits and 16!=0;val r=bits and 32!=0;val b=bits and 64!=0;val action=decide(e,p,m,v,l,r,b);if(action==ListeningStartupAction.START_WAKE)assertTrue(e&&p&&m&&v&&!l&&!r&&!b);if(action==ListeningStartupAction.MODEL_REQUIRED)assertTrue(e&&p&&!m&&v&&!l&&!r&&!b)}}
}
