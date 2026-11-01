package com.jarvis.assistant.tools

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

/** Read-only, on-demand screen access. No event text is retained or uploaded. */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { current = WeakReference(this) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() {
        if (current.get() === this) current.clear()
        super.onDestroy()
    }

    companion object {
        private var current = WeakReference<JarvisAccessibilityService>(null)
        val connected: Boolean get() = current.get() != null

        fun snapshot(context: Context): ScreenReadResult {
            if (context.getSystemService(KeyguardManager::class.java).isDeviceLocked) return ScreenReadResult(null, ScreenFailure.LOCKED)
            val service = current.get() ?: return ScreenReadResult(null, ScreenFailure.PERMISSION)
            val root = service.rootInActiveWindow ?: return ScreenReadResult(null, ScreenFailure.UNAVAILABLE)
            val packageName = root.packageName?.toString().orEmpty()
            if (packageName.isBlank() || ToolPolicy.protectedPackage(packageName)) return ScreenReadResult(null, ScreenFailure.PROTECTED)
            val lines = LinkedHashSet<String>()
            var nodes = 0
            var sensitiveFocusedField = false
            fun visit(node: AccessibilityNodeInfo?, depth: Int) {
                if (node == null || depth > 25 || nodes++ > 250 || lines.size >= 70) return
                if (node.isPassword) {
                    if (node.isFocused || node.isAccessibilityFocused) sensitiveFocusedField = true
                    return
                }
                if (!node.isVisibleToUser) return
                val label = node.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                    ?: node.contentDescription?.toString()?.trim().orEmpty()
                if (label.isNotBlank()) lines += ToolPolicy.sensitiveText(label).take(250)
                for (index in 0 until minOf(node.childCount, 40)) visit(node.getChild(index), depth + 1)
            }
            visit(root, 0)
            if (sensitiveFocusedField) return ScreenReadResult(null, ScreenFailure.PROTECTED)
            if (lines.isEmpty()) return ScreenReadResult(null, ScreenFailure.UNAVAILABLE)
            return ScreenReadResult(lines.joinToString("\n").take(7_000), null, packageName)
        }

        fun navigate(context: Context, home: Boolean): Boolean {
            if (context.getSystemService(KeyguardManager::class.java).isDeviceLocked) return false
            val service = current.get() ?: return false
            val packageName = service.rootInActiveWindow?.packageName?.toString().orEmpty()
            if (ToolPolicy.protectedPackage(packageName)) return false
            return service.performGlobalAction(if (home) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
        }
    }
}

enum class ScreenFailure { LOCKED, PERMISSION, PROTECTED, UNAVAILABLE }
data class ScreenReadResult(val text: String?, val failure: ScreenFailure?, val packageName: String = "")
