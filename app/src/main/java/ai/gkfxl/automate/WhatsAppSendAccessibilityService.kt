package ai.gkfxl.automate

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * One-shot assist only. It never opens chats or composes messages.
 * The user must enable the service, enable the app setting, arm a specific
 * message for 60 seconds, and open the intended WhatsApp chat themselves.
 */
class WhatsAppSendAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() != "com.whatsapp.w4b") return
        val prefs = getSharedPreferences("whatsapp_safety", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val expires = prefs.getLong("armed_until", 0L)
        val expected = prefs.getString("armed_message", null)?.trim().orEmpty()
        if (expected.isBlank() || System.currentTimeMillis() > expires) {
            prefs.edit().remove("armed_message").putLong("armed_until", 0L).apply()
            return
        }
        val root = rootInActiveWindow ?: return
        if (!containsExactComposerText(root, expected)) return
        val send = findSendButton(root) ?: return
        // Consume the one-shot arm before clicking to prevent duplicate sends.
        prefs.edit().remove("armed_message").putLong("armed_until", 0L).apply()
        send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun containsExactComposerText(node: AccessibilityNodeInfo, expected: String): Boolean {
        val text = node.text?.toString()?.trim()
        if (node.isEditable && text == expected) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = containsExactComposerText(child, expected)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = (node.text?.toString() ?: node.contentDescription?.toString()).orEmpty()
        if (node.isClickable && label.equals("Send", ignoreCase = true)) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSendButton(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() = Unit
}