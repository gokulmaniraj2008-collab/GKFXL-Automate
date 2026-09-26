package ai.gkfxl.automate

import android.app.Notification
import android.app.Notification.Action
import android.app.NotificationManager
import android.app.RemoteInput
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads WhatsApp notification previews and optionally replies using the notification's
 * supported RemoteInput action. It does not open WhatsApp or bypass its UI.
 */
class WhatsAppNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != WHATSAPP_PACKAGE) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if (text.isBlank() || title.isBlank() || title.equals("WhatsApp", ignoreCase = true)) return

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getStringSet("seen_keys", emptySet())?.toMutableSet() ?: mutableSetOf()
        val key = "${sbn.key}:${text.hashCode()}"
        if (!seen.add(key)) return
        while (seen.size > 100) seen.remove(seen.first())
        prefs.edit().putStringSet("seen_keys", seen).putString("last_sender", title)
            .putString("last_message", text).putLong("last_received", System.currentTimeMillis()).apply()

        if (!prefs.getBoolean("auto_reply_enabled", false)) return
        val cooldownKey = "last_reply_" + title.hashCode()
        if (System.currentTimeMillis() - prefs.getLong(cooldownKey, 0L) < 10 * 60 * 1000L) return

        val replyText = prefs.getString("auto_reply_text", "").orEmpty().trim()
        if (replyText.isBlank()) return
        val action = sbn.notification.actions?.firstOrNull { candidate ->
            candidate.remoteInputs?.isNotEmpty() == true &&
                (candidate.title?.toString()?.contains("reply", true) == true ||
                 candidate.remoteInputs?.any { it.allowFreeFormInput } == true)
        } ?: return

        val remoteInputs = action.remoteInputs ?: return
        val fillIn = android.content.Intent()
        val results = Bundle()
        remoteInputs.forEach { input -> results.putCharSequence(input.resultKey, replyText) }
        RemoteInput.addResultsToIntent(remoteInputs, fillIn, results)
        try {
            action.actionIntent.send(this, 0, fillIn)
            prefs.edit().putLong(cooldownKey, System.currentTimeMillis()).apply()
        } catch (_: Exception) {
            // Unsupported/stale notification actions are ignored; no fallback opens WhatsApp.
        }
    }

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val PREFS = "whatsapp_notifications"
    }
}
