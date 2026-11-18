package com.jarvis.assistant.neural

import com.jarvis.assistant.rebirth.*
import org.junit.Assert.*
import org.junit.Test

/** Visible-app fixtures; no claim that Android launched a real package on a handset. */
class AppDiscoveryRepairTest {
    private fun app(pkg:String,label:String)=DiscoveredApp(pkg,label,AppDiscoveryPolicy.names(pkg))
    private fun resolve(text:String,vararg apps:DiscoveredApp)=SemanticAutocorrector.correct(text,emptyList(),apps.toList(),emptyMap())
    @Test fun wakePrefixedGmailResolvesLocalizedLabel(){assertEquals("com.google.android.gm",resolve("Hey Jarvis, open Gmail",app("com.google.android.gm","Correo")).targetPackage)}
    @Test fun wakePrefixedWhatsAppDispatchesExactCandidate(){assertEquals("com.whatsapp",resolve("Hey Jarvis open WhatsApp",app("com.whatsapp","WhatsApp")).targetPackage)}
    @Test fun spanishWakePrefixPreservesIntent(){assertEquals("com.android.chrome",resolve("Oye Jarvis abre Chrome",app("com.android.chrome","Navegador")).targetPackage)}
    @Test fun localizedChromeHasCanonicalName(){assertEquals("com.android.chrome",resolve("open Chrome",app("com.android.chrome","کروم")).targetPackage)}
    @Test fun localizedInstagramHasCanonicalName(){assertEquals("com.instagram.android",resolve("open Instagram",app("com.instagram.android","اینستاگرام")).targetPackage)}
    @Test fun localizedYouTubeHasCanonicalName(){assertEquals("com.google.android.youtube",resolve("open YouTube",app("com.google.android.youtube","یوتیوب")).targetPackage)}
    @Test fun localizedMapsHasCanonicalName(){assertEquals("com.google.android.apps.maps",resolve("open Maps",app("com.google.android.apps.maps","نقشه‌ها")).targetPackage)}
    @Test fun localizedSpotifyHasCanonicalName(){assertEquals("com.spotify.music",resolve("open Spotify",app("com.spotify.music","اسپاتیفای")).targetPackage)}
    @Test fun xiaomiCalculatorUsesActualPackage(){assertEquals("com.miui.calculator",resolve("open Calculator",app("com.miui.calculator","Calculadora")).targetPackage)}
    @Test fun xiaomiCameraUsesActualPackage(){assertEquals("com.android.camera",resolve("open Camera",app("com.android.camera","Cámara")).targetPackage)}
    @Test fun twoRealCameraAppsRequireClarification(){val r=resolve("open Camera",app("com.android.camera","Cámara"),app("com.google.android.GoogleCamera","Cámara Google"));assertNull(r.targetPackage);assertEquals(CorrectionKind.CLARIFY,r.kind)}
    @Test fun missingAppDoesNotProduceAnInventedPackage(){assertNull(resolve("open Gmail",app("com.whatsapp","WhatsApp")).targetPackage)}
    @Test fun unknownPackageReceivesNoInventedCanonicalName(){assertTrue(AppDiscoveryPolicy.names("com.example.unknown").isEmpty())}
    @Test fun disabledApplicationsAreNotEligible(){assertFalse(AppDiscoveryPolicy.eligible(false,true,true))}
    @Test fun disabledLauncherActivitiesAreNotEligible(){assertFalse(AppDiscoveryPolicy.eligible(true,false,true))}
    @Test fun nonexportedActivitiesAreNotEligible(){assertFalse(AppDiscoveryPolicy.eligible(true,true,false))}
    @Test fun enabledExportedLauncherIsEligible(){assertTrue(AppDiscoveryPolicy.eligible(true,true,true))}
    @Test fun questionsAboutOpeningAppsAreNotLaunchCommands(){assertNull(AppMatching.commandTarget("how do I open Gmail"))}
    @Test fun compoundLaunchesAreNotSilentlyCombined(){assertNull(AppMatching.commandTarget("open Gmail and then WhatsApp"))}
    @Test fun pinnedAliasStillUsesOnlyAnActualVisibleTarget(){val r=SemanticAutocorrector.correct("open school mail",emptyList(),listOf(app("com.google.android.gm","Gmail")),mapOf("school mail" to "com.google.android.gm"));assertEquals("com.google.android.gm",r.targetPackage)}
    @Test fun missingPinnedAliasNeverSwitchesToDifferentApp(){val r=SemanticAutocorrector.correct("open school mail",emptyList(),listOf(app("com.whatsapp","School mail")),mapOf("school mail" to "com.google.android.gm"));assertNull(r.targetPackage);assertEquals(CorrectionKind.CLARIFY,r.kind)}
}
