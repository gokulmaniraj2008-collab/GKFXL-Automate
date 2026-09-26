package ai.gkfxl.automate

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class AutomationRule(val id: Long, val title: String, val hour: Int, val minute: Int, val recurring: Boolean, val enabled: Boolean, val phone: String = "", val message: String = "", val actions: String = "Notification", val condition: String = "Always")

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { GKFXLApp() }
    }
}

@Composable
private fun GKFXLApp() {
    val context = LocalContext.current
    var rules by remember { mutableStateOf(loadRules(context)) }
    var title by remember { mutableStateOf("My reminder") }
    var time by remember { mutableStateOf("19:00") }
    var recurring by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Hello") }
    var actions by remember { mutableStateOf(setOf("Notification", "WhatsApp")) }
    var condition by remember { mutableStateOf("Always") }
    var showConditionMenu by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Your automations stay on this device.") }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF176B55), secondary = Color(0xFF477AAB), background = Color(0xFFF6F8F7), surface = Color.White)) {
        Scaffold(
            topBar = { Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) { Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("GKFXL Automate", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your routines, on your schedule", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } } },
            bottomBar = { NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("◷") }, label = { Text("Rules") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("✉") }, label = { Text("WhatsApp") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("ⓘ") }, label = { Text("Status") })
            } }
        ) { padding ->
            when (tab) {
                0 -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Create a reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Rule name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (24-hour HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = recurring, onCheckedChange = { recurring = it }); Text("Repeat every day") }
                        Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        listOf("Notification", "WhatsApp", "Open URL").forEach { action -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = action in actions, onCheckedChange = { checked -> actions = if (checked) actions + action else actions - action }); Text(action) } }
                        Text("Condition (IF)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Box { OutlinedButton(onClick = { showConditionMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(condition) }; DropdownMenu(expanded = showConditionMenu, onDismissRequest = { showConditionMenu = false }) { listOf("Always", "Battery below 20%", "Battery below 50%", "Battery above 50%").forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { condition = item; showConditionMenu = false }) } } }
                        Text("Optional WhatsApp reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("WhatsApp number (with country code)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message to prepare") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Text("Choose multiple actions and an optional battery condition. WhatsApp opens with your message prepared; you must tap Send. URL actions can be configured in the next engine update.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = {
                            val parts = time.split(":"); val hour = parts.getOrNull(0)?.toIntOrNull(); val minute = parts.getOrNull(1)?.toIntOrNull()
                            if (title.isBlank() || hour == null || minute == null || hour !in 0..23 || minute !in 0..59) status = "Enter a name and valid time, such as 19:00."
                            else {
                                val digits = phone.filter { it.isDigit() }; val cleanPhone = if (digits.startsWith("00")) digits.drop(2) else digits
                                val rule = AutomationRule(System.currentTimeMillis(), title.trim(), hour, minute, recurring, true, cleanPhone, message.trim(), actions.joinToString("|"), condition)
                                rules = rules + rule; saveRules(context, rules); schedule(context, rule)
                                status = "Reminder saved. Check Android alarm and notification permissions."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save and schedule") }
                    } } }
                    item { Text("Your rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
                    if (rules.isEmpty()) item { Text("No rules yet. Create your first reminder above.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(rules, key = { it.id }) { rule -> Card { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(rule.title, fontWeight = FontWeight.SemiBold); Text("%02d:%02d • %s".format(rule.hour, rule.minute, if (rule.recurring) "Daily" else "One time"), color = MaterialTheme.colorScheme.onSurfaceVariant); if (rule.phone.isNotBlank()) Text("WhatsApp: ${rule.phone}", style = MaterialTheme.typography.bodySmall); Text("Actions: ${rule.actions}", style = MaterialTheme.typography.bodySmall); Text("IF: ${rule.condition}", style = MaterialTheme.typography.bodySmall) }
                            Switch(checked = rule.enabled, onCheckedChange = { enabled ->
                                val updated = rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it }; rules = updated; saveRules(context, updated)
                                if (enabled) schedule(context, rule.copy(enabled = true)) else cancel(context, rule)
                            })
                        }
                        OutlinedButton(onClick = { cancel(context, rule); rules = rules.filterNot { it.id == rule.id }; saveRules(context, rules); status = "Rule deleted." }) { Text("Delete") }
                    } } }
                }
                1 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WhatsApp message reader & auto-reply", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Reads notification previews without opening WhatsApp. Auto-reply uses WhatsApp's notification reply action when available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val notificationPrefs = remember { context.getSharedPreferences("whatsapp_notifications", Context.MODE_PRIVATE) }
                    var autoReplyEnabled by remember { mutableStateOf(notificationPrefs.getBoolean("auto_reply_enabled", false)) }
                    var autoReplyText by remember { mutableStateOf(notificationPrefs.getString("auto_reply_text", "Thanks for your message! I'll reply soon.") ?: "") }
                    val latestSender = notificationPrefs.getString("last_sender", null)
                    val latestMessage = notificationPrefs.getString("last_message", null)
                    Card { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable automatic reply", modifier = Modifier.weight(1f))
                            Switch(checked = autoReplyEnabled, onCheckedChange = { enabled ->
                                autoReplyEnabled = enabled
                                notificationPrefs.edit().putBoolean("auto_reply_enabled", enabled).apply()
                            })
                        }
                        OutlinedTextField(value = autoReplyText, onValueChange = { autoReplyText = it; notificationPrefs.edit().putString("auto_reply_text", it).apply() }, label = { Text("Auto-reply message") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedButton(onClick = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }, modifier = Modifier.fillMaxWidth()) { Text("Grant notification access") }
                        if (latestMessage != null) {
                            Text("Latest notification: ${latestSender ?: "Unknown"}", fontWeight = FontWeight.SemiBold)
                            Text(latestMessage, style = MaterialTheme.typography.bodySmall)
                        } else Text("No WhatsApp notification captured yet.", style = MaterialTheme.typography.bodySmall)
                        Text("Safety: replies are limited to one per sender every 10 minutes. Keep auto-reply off until you test with a trusted contact. Notification text may contain private information.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    HorizontalDivider()
                    Text("Prepare a WhatsApp message", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("This opens WhatsApp with your message filled in. You must review it and tap Send.")
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number with country code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Button(onClick = {
                        val digits = phone.filter { it.isDigit() }
                        if (digits.isBlank() || message.isBlank()) status = "Enter a phone number and message."
                        else try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=" + Uri.encode(message)))) } catch (_: Exception) { status = "WhatsApp could not be opened. Install or enable WhatsApp." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Open WhatsApp") }
                    HorizontalDivider()
                    Text("Accessibility send assist (optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("When enabled, the service can tap Send once only if the exact message is visible in WhatsApp. You must open the intended chat yourself. Keep this disabled unless you understand the risk of sending to the wrong chat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var autoSendEnabled by remember { mutableStateOf(context.getSharedPreferences("whatsapp_safety", Context.MODE_PRIVATE).getBoolean("enabled", false)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable send assist", modifier = Modifier.weight(1f))
                        Switch(checked = autoSendEnabled, onCheckedChange = { enabled ->
                            autoSendEnabled = enabled
                            context.getSharedPreferences("whatsapp_safety", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply()
                            if (!enabled) context.getSharedPreferences("whatsapp_safety", Context.MODE_PRIVATE).edit().remove("armed_message").putLong("armed_until", 0L).apply()
                        })
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Open Accessibility Settings") }
                    Button(onClick = {
                        val digits = phone.filter { it.isDigit() }
                        if (!autoSendEnabled) status = "Enable send assist first."
                        else if (digits.isBlank() || message.isBlank()) status = "Enter a phone number and message."
                        else {
                            context.getSharedPreferences("whatsapp_safety", Context.MODE_PRIVATE).edit()
                                .putString("armed_message", message.trim())
                                .putLong("armed_until", System.currentTimeMillis() + 60_000L).apply()
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=" + Uri.encode(message.trim())))) }
                            catch (_: Exception) { status = "Could not open WhatsApp." }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Arm one send (60 seconds) & open WhatsApp") }
                    Text("The one-shot arm expires after 60 seconds and is consumed before a tap. Android may restrict or alter this behavior; test with a trusted contact first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Automatic sending is off until you enable it and grant Accessibility access.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Permissions & status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(status); Text("Exact alarms may require approval in Android Settings. Notifications must be allowed.")
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) try { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) } catch (_: Exception) { status = "Open Settings and allow alarms for GKFXL Automate." }
                            else status = "Separate exact-alarm permission is not required on this Android version."
                        }) { Text("Manage alarm permission") }
                        OutlinedButton(onClick = { status = "Rules are stored locally. Battery settings can affect background delivery." }) { Text("Check status") }
                    } }
                }
            }
        }
    }
}

private fun loadRules(context: Context): List<AutomationRule> = try {
    val array = JSONArray(context.getSharedPreferences("rules", Context.MODE_PRIVATE).getString("items", "[]"))
    (0 until array.length()).map { i -> val o = array.getJSONObject(i); AutomationRule(o.getLong("id"), o.getString("title"), o.getInt("hour"), o.getInt("minute"), o.getBoolean("recurring"), o.getBoolean("enabled"), o.optString("phone", ""), o.optString("message", ""), o.optString("actions", "Notification"), o.optString("condition", "Always")) }
} catch (_: Exception) { emptyList() }

private fun saveRules(context: Context, rules: List<AutomationRule>) {
    val array = JSONArray()
    rules.forEach { r -> array.put(JSONObject().put("id", r.id).put("title", r.title).put("hour", r.hour).put("minute", r.minute).put("recurring", r.recurring).put("enabled", r.enabled).put("phone", r.phone).put("message", r.message).put("actions", r.actions).put("condition", r.condition)) }
    context.getSharedPreferences("rules", Context.MODE_PRIVATE).edit().putString("items", array.toString()).apply()
}

private fun pendingIntent(context: Context, rule: AutomationRule): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("id", rule.id).putExtra("title", rule.title).putExtra("recurring", rule.recurring).putExtra("phone", rule.phone).putExtra("message", rule.message).putExtra("actions", rule.actions).putExtra("condition", rule.condition)
    return PendingIntent.getBroadcast(context, rule.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun schedule(context: Context, rule: AutomationRule) {
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, rule.hour); set(Calendar.MINUTE, rule.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent(context, rule))
        else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent(context, rule))
    } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent(context, rule)) }
}

private fun cancel(context: Context, rule: AutomationRule) {
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent(context, rule))
}
