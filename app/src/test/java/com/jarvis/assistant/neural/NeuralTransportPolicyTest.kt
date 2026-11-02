package com.jarvis.assistant.neural

import com.jarvis.assistant.local.ModelDownloadPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class NeuralTransportPolicyTest {
    @Test fun resumedDownloadMustBindBothRangeAndVerifiedTotal(){assertEquals(25L,DownloadResumePolicy.initialBytes(25,100,206,"bytes 25-99/100",75));assertTrue(runCatching{DownloadResumePolicy.initialBytes(25,100,206,"bytes 25-90/100",66)}.isFailure);assertTrue(runCatching{DownloadResumePolicy.initialBytes(25,100,206,"bytes 25-99/200",75)}.isFailure)}
    @Test fun serverMayRestartButCannotAppendAnUnboundResponse(){assertEquals(0L,DownloadResumePolicy.initialBytes(25,100,200,null,100));assertTrue(runCatching{DownloadResumePolicy.initialBytes(25,100,200,null,75)}.isFailure);assertTrue(runCatching{DownloadResumePolicy.initialBytes(0,100,206,"bytes 0-99/100",100)}.isFailure)}
    @Test fun selectedCandidateHasIndependentRepositoryAndSizeBounds(){val p=BrainProfiles.balanced;val j=JSONObject().put("id",p.repository).put("sha","a".repeat(40)).put("cardData",JSONObject().put("license","apache-2.0")).put("siblings",org.json.JSONArray().put(JSONObject().put("rfilename",p.file).put("lfs",JSONObject().put("size",1_100_000_000L).put("sha256","b".repeat(64)))))
        assertEquals(1_100_000_000L,ModelDownloadPolicy.parseOfficialMetadata(j.toString(),p).size);assertTrue(runCatching{ModelDownloadPolicy.parseOfficialMetadata(j.toString(),BrainProfiles.lite)}.isFailure)
    }
    @Test fun publicResearchRejectsPrivateAndCarrierInternalAddresses(){listOf("127.0.0.1","10.0.0.1","169.254.1.1","192.168.2.1","100.64.0.1","::1","fc00::1").forEach{assertFalse(it,ResearchPolicy.publicAddress(InetAddress.getByName(it)))};assertTrue(ResearchPolicy.publicAddress(InetAddress.getByName("8.8.8.8")))}
    @Test fun sourceTextDoesNotRenderScriptsOrInventAPublicationDate(){val text=ResearchPolicy.readable("<h1>Actual title</h1><script>steal()</script><p>Source &amp; evidence.</p>");assertTrue(text.contains("Actual title"));assertTrue(text.contains("Source & evidence"));assertFalse(text.contains("steal"))}
    @Test fun malformedSourceMarkupIsBoundedAndDoesNotExposeScript(){assertEquals("before",ResearchPolicy.readable("before<script>"+"<".repeat(100000)));assertEquals("before",ResearchPolicy.readable("before<"+"x".repeat(100000)))}
    @Test fun localOnlyRequestsOverrideCloudAndIncludeThreeLanguages(){assertTrue(ContextPrivacyPolicy.localOnly("offline only: explain gravity"));assertTrue(ContextPrivacyPolicy.localOnly("solo sin nube"));assertTrue(ContextPrivacyPolicy.localOnly("فقط روی گوشی پاسخ بده"));assertFalse(ContextPrivacyPolicy.localOnly("what time is it"))}
    @Test fun legacyActionDefinitionsCannotBecomeModelAuthority(){assertTrue("live_control" in ToolRegistry.definitions);assertTrue(ToolRegistry.definitions.getValue("live_control").confirmation);assertTrue(runCatching{ToolRegistry.parse("""{"tools":[{"id":"live_control","arguments":{"bound_target_payload":"untrusted"}}]}""")}.isFailure)}
}
