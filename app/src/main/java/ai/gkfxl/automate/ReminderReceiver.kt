package ai.gkfxl.automate

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "GKFXL reminder"
        val id = intent.getLongExtra("id", 1L)
        val recurring = intent.getBooleanExtra("recurring", false)
        val channelId = "gkfxl_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val phone = intent.getStringExtra("phone") ?: ""
        val message = intent.getStringExtra("message") ?: ""
        val openIntent = if (phone.isNotBlank()) Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=" + Uri.encode(message))) else Intent(context, MainActivity::class.java)
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val openAction = PendingIntent.getActivity(context, id.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body = if (phone.isNotBlank()) "Tap to open WhatsApp with your message ready to send" else "Your scheduled reminder"
        manager.notify(id.hashCode(), NotificationCompat.Builder(context, channelId).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(body).setContentIntent(openAction).setAutoCancel(true).build())
        if (recurring) {
            val items = org.json.JSONArray(context.getSharedPreferences("rules", Context.MODE_PRIVATE).getString("items", "[]"))
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.optLong("id") == id && item.optBoolean("enabled", true)) {
                    val next = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, item.optInt("hour", 9)); set(Calendar.MINUTE, item.optInt("minute", 0)); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_YEAR, 1) }
                    val nextIntent = Intent(context, ReminderReceiver::class.java).putExtra("id", id).putExtra("title", title).putExtra("recurring", true).putExtra("phone", item.optString("phone", "")).putExtra("message", item.optString("message", ""))
                    val pi = PendingIntent.getBroadcast(context, id.hashCode(), nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    try { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pi) } catch (_: Exception) { }
                    break
                }
            }
        }
    }
}
