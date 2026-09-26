package ai.gkfxl.automate

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Reads notification previews separately for WhatsApp Messenger and Business. */
class WhatsAppNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // This build intentionally supports WhatsApp Business only.
        if (sbn.packageName != BUSINESS_PACKAGE) return
        val app = "business"
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("${app}_reading_enabled", true)) return

        val extras = sbn.notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val message = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if (message.isBlank() || sender.isBlank() || sender.equals("WhatsApp", true) ||
            sender.equals("WhatsApp Business", true)) return

        prefs.edit().putString("${app}_last_sender", sender)
            .putString("${app}_last_message", message)
            .putLong("${app}_last_received", System.currentTimeMillis()).apply()

        // Automatic replies are restricted to WhatsApp Business and are opt-in.
        if (!prefs.getBoolean("${app}_auto_reply_enabled", false)) return

        val cooldownKey = "${app}_last_reply_" + sender.hashCode()
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(cooldownKey, 0L) < COOLDOWN_MS) return

        val reply = prefs.getString("${app}_reply_text", "").orEmpty().trim()
        if (reply.isBlank()) return

        // Approval mode never sends automatically: retain a draft for the user to review.
        if (prefs.getBoolean("${app}_approval_required", true)) {
            prefs.edit().putString("${app}_pending_sender", sender)
                .putString("${app}_pending_reply", reply)
                .putLong("${app}_pending_at", now).apply()
            return
        }

        val action = sbn.notification.actions?.firstOrNull { candidate ->
            candidate.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: return
        val inputs = action.remoteInputs ?: return
        val fillIn = Intent()
        val results = Bundle()
        inputs.forEach { results.putCharSequence(it.resultKey, reply) }
        RemoteInput.addResultsToIntent(inputs, fillIn, results)
        try {
            action.actionIntent.send(this, 0, fillIn)
            prefs.edit().putLong(cooldownKey, now).apply()
        } catch (_: Exception) {
            // A stale or unsupported notification action is ignored.
        }
    }

    companion object {
        const val BUSINESS_PACKAGE = "com.whatsapp.w4b"
        const val PREFS = "whatsapp_notifications"
        private const val COOLDOWN_MS = 10 * 60 * 1000L
    }
}
