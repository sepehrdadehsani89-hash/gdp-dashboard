package com.jarvis.assistant

import android.app.UiAutomation
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.tools.JarvisAccessibilityService

/** Dedicated emulator cleanup: release test-owned access before the runner kills the process. */
fun releaseTestScreenAccess() {
    val i = InstrumentationRegistry.getInstrumentation()
    val automation = i.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    val component = "${i.targetContext.packageName}/${JarvisAccessibilityService::class.java.name}"
    automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS)
    try {
        val others = Settings.Secure.getString(i.targetContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').filter { it.isNotBlank() && it != component }
        Settings.Secure.putString(i.targetContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, others.joinToString(":"))
        Settings.Secure.putInt(i.targetContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, if (others.isEmpty()) 0 else 1)
    } finally { automation.dropShellPermissionIdentity() }
    val end = System.currentTimeMillis() + 10_000
    while (JarvisAccessibilityService.connected && System.currentTimeMillis() < end) Thread.sleep(100)
}
