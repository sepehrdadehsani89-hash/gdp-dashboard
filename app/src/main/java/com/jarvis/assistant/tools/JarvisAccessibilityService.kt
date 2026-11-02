package com.jarvis.assistant.tools

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

/** On-demand screen access and explicit, target-bound live controls. */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { current = WeakReference(this) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() {
        if (current.get() === this) current.clear()
        super.onDestroy()
    }

    companion object {
        @Volatile private var current = WeakReference<JarvisAccessibilityService>(null)
        val connected: Boolean get() = current.get() != null
        fun activePackage():String? = current.get()?.rootInActiveWindow?.let { root->try{root.packageName?.toString()}finally{@Suppress("DEPRECATION")root.recycle()} }

        fun snapshot(context: Context, forCapture: Boolean = false): ScreenReadResult {
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
                    if (forCapture || node.isFocused || node.isAccessibilityFocused) sensitiveFocusedField = true
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
            if (lines.isEmpty()) return ScreenReadResult(null, ScreenFailure.UNAVAILABLE, packageName)
            return ScreenReadResult(lines.joinToString("\n").take(7_000), null, packageName)
        }

        private fun liveRoot(context: Context): AccessibilityNodeInfo {
            check(!context.getSystemService(KeyguardManager::class.java).isDeviceLocked) { "Unlock the phone first." }
            val root = current.get()?.rootInActiveWindow ?: error("Enable Screen access and open the target app first.")
            val pkg = root.packageName?.toString().orEmpty()
            check(pkg.isNotBlank() && !ToolPolicy.protectedPackage(pkg)) { "Protected screen." }
            return root
        }

        private fun target(root: AccessibilityNodeInfo, request: LiveRequest): AccessibilityNodeInfo {
            val matches = mutableListOf<AccessibilityNodeInfo>()
            var visited = 0
            fun visit(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 25 || visited++ > 250 || !node.isVisibleToUser || node.isPassword) return
                if (request is LiveRequest.Edit && node.isFocused && node.isEditable && node.isEnabled) matches += node
                if (request is LiveRequest.Tap && node.isClickable && node.isEnabled &&
                    listOf(node.text, node.contentDescription).any { ToolPolicy.normalize(it?.toString().orEmpty()) == ToolPolicy.normalize(request.label) }) matches += node
                for (i in 0 until minOf(node.childCount, 40)) node.getChild(i)?.let { visit(it, depth + 1) }
            }
            visit(root, 0)
            check(matches.size == 1) { "Need exactly one visible named control, or one focused editable draft. Focus it and try again." }
            val node = matches.single()
            val labels = listOf(node.text, node.contentDescription, node.hintText, node.viewIdResourceName).joinToString(" ") { it?.toString().orEmpty() }
            check(LiveControlPolicy.allowedLabel(labels)) { "Operate sending, destructive and permission controls yourself." }
            return node
        }

        private fun identity(root: AccessibilityNodeInfo, node: AccessibilityNodeInfo): String {
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            // Bind approval to the visible window contents as well as the target.
            var visited = 0
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            fun visit(item: AccessibilityNodeInfo, depth: Int) {
                if (depth > 25 || visited++ > 250 || !item.isVisibleToUser) return
                check(!item.isPassword) { "Password screen. Operate it yourself." }
                digest.update(listOf(item.viewIdResourceName, item.text, item.contentDescription).joinToString("|").toByteArray(Charsets.UTF_8))
                for (index in 0 until minOf(item.childCount, 40)) item.getChild(index)?.let { visit(it, depth + 1) }
            }
            visit(root, 0)
            val screenHash = digest.digest().joinToString("") { "%02x".format(it) }
            return listOf(root.packageName, root.windowId, node.viewIdResourceName, bounds.toShortString(), node.text, node.contentDescription, screenHash).joinToString("|")
        }

        fun prepareLive(context: Context, request: LiveRequest): String {
            if (request is LiveRequest.Tap) check(LiveControlPolicy.allowedLabel(request.label)) { "Operate sending and destructive controls yourself." }
            val root = liveRoot(context)
            val node = target(root, request)
            return org.json.JSONObject().put("edit", request is LiveRequest.Edit)
                .put("value", when (request) { is LiveRequest.Edit -> request.text; is LiveRequest.Tap -> request.label })
                .put("identity", identity(root, node)).toString()
        }

        fun executeLive(context: Context, payload: String): Boolean {
            val json = org.json.JSONObject(payload)
            val request = if (json.getBoolean("edit")) LiveRequest.Edit(json.getString("value")) else LiveRequest.Tap(json.getString("value"))
            val root = liveRoot(context)
            val node = target(root, request)
            check(identity(root, node) == json.getString("identity")) { "Screen changed. Ask again." }
            return when (request) {
                is LiveRequest.Tap -> {
                    check(LiveControlPolicy.allowedLabel(request.label))
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                is LiveRequest.Edit -> node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, request.text)
                })
            }
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
