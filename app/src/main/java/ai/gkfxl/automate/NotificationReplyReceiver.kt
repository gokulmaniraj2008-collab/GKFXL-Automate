package ai.gkfxl.automate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reserved for explicit notification-action handling; no background UI launching. */
class NotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reply is sent through WhatsApp's own notification RemoteInput action.
    }
}
