package com.jarvis.assistant.setup

import org.junit.Assert.*
import org.junit.Test

class NexusNavigationPolicyTest {
    @Test fun cloudConfigurationUsesItsActualScreen(){assertEquals(NexusDestination.GOOGLE_CLOUD,NexusNavigationPolicy.parse("Open Google Cloud Configuration"))}
    @Test fun configureCloudDoesNotInventAProject(){assertEquals(NexusDestination.GOOGLE_CLOUD,NexusNavigationPolicy.parse("Configure Google Cloud"))}
    @Test fun servicePermissionsUseOfficialAccountManagement(){assertEquals(NexusDestination.GOOGLE_SERVICES,NexusNavigationPolicy.parse("Manage Google permissions"))}
    @Test fun manageAccountsUsesExistingServices(){assertEquals(NexusDestination.GOOGLE_SERVICES,NexusNavigationPolicy.parse("Manage Google accounts"))}
    @Test fun wallpaperStudioIsExplicit(){assertEquals(NexusDestination.WALLPAPER_STUDIO,NexusNavigationPolicy.parse("Open Wallpaper Studio"))}
    @Test fun colorStudioIsPartOfActualEditor(){assertEquals(NexusDestination.WALLPAPER_STUDIO,NexusNavigationPolicy.parse("Open Color Studio"))}
    @Test fun widgetLibraryIsPartOfActualEditor(){assertEquals(NexusDestination.WALLPAPER_STUDIO,NexusNavigationPolicy.parse("Open Widget Library"))}
    @Test fun editorIntentDoesNotMutateAutomatically(){assertEquals(NexusDestination.WALLPAPER_STUDIO,NexusNavigationPolicy.parse("Edit my wallpaper"))}
    @Test fun computerQrOpensReviewedPairing(){assertEquals(NexusDestination.COMPUTERS,NexusNavigationPolicy.parse("Scan computer QR"))}
    @Test fun pairingDiagnosticsHaveARealDestination(){assertEquals(NexusDestination.COMPUTERS,NexusNavigationPolicy.parse("Open pairing diagnostics"))}
    @Test fun reconfigurationUsesNewWizard(){assertEquals(NexusDestination.SETUP,NexusNavigationPolicy.parse("Reconfigure Jarvis"))}
    @Test fun modeSettingsDoNotApplyMode(){assertEquals(NexusDestination.SYSTEM_MODES,NexusNavigationPolicy.parse("Open Silent Mode"))}
    @Test fun deepSettingsDoNotExecuteShutdown(){assertEquals(NexusDestination.SYSTEM_MODES,NexusNavigationPolicy.parse("Open Deep Shutdown"))}
    @Test fun commandLabIsExplicit(){assertEquals(NexusDestination.COMMAND_LAB,NexusNavigationPolicy.parse("Open command test lab"))}
    @Test fun wakePrefixAndPunctuationAreNormalized(){assertEquals(NexusDestination.WALLPAPER_STUDIO,NexusNavigationPolicy.parse("Hey Jarvis, open Wallpaper Studio."))}
    @Test fun spanishEditorNavigationWorks(){assertEquals(NexusDestination.WALLPAPER_STUDIO,NexusNavigationPolicy.parse("Abre el estudio de fondos"))}
    @Test fun negatedEditorRequestDoesNotOpen(){assertNull(NexusNavigationPolicy.parse("Do not open Wallpaper Studio"))}
    @Test fun questionDoesNotNavigate(){assertNull(NexusNavigationPolicy.parse("What is Wallpaper Studio?"))}
    @Test fun arbitraryShellIsNotANavigationTool(){assertNull(NexusNavigationPolicy.parse("run powershell rm everything"))}
    @Test fun ordinaryGmailLaunchIsNotShadowed(){assertNull(NexusNavigationPolicy.parse("Open Gmail"))}
    @Test fun existingChromeCommandIsNotShadowed(){assertNull(NexusNavigationPolicy.parse("Open Chrome on my computer"))}
    @Test fun urlCannotBecomeADestination(){assertNull(NexusNavigationPolicy.parse("https://example.com/execute"))}
}
