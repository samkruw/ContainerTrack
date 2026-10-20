package ch.zerogen.containertrack

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class ReminderAlarms(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    fun createChannel() {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Logger starten", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Lokale Erinnerung 30 Minuten nach dem ersten Delivery-Export"
        })
    }
    fun exactAllowed(): Boolean = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    fun notificationsAllowed(): Boolean = notifications.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun pending(id: Int, action: String): PendingIntent = PendingIntent.getBroadcast(context, id,
        Intent(context, ReminderReceiver::class.java).setAction(action).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(reminder: LoggerReminder) {
        if (reminder.confirmedAt != null) return
        val at = maxOf(reminder.dueAt, System.currentTimeMillis() + 500L)
        val operation = pending(reminder.id, DUE)
        if (exactAllowed()) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation); return }
            catch (_: SecurityException) { /* Systemzugriff kann zwischen Prüfung und Aufruf entzogen werden. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
    }

    fun cancel(id: Int) { manager.cancel(pending(id, DUE)); notifications.cancel(id) }

    fun notifyDue(reminder: LoggerReminder) {
        if (!notificationsAllowed()) return
        val launch = PendingIntent.getActivity(context, reminder.id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_container)
            .setContentTitle("Logger starten")
            .setContentText("Auftrag ${reminder.referenceNumber} · JETZT")
            .setContentIntent(launch)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .addAction(R.drawable.ic_container, "Logger gestartet", pending(reminder.id, CONFIRM))
            .build()
        try { notifications.notify(reminder.id, notification) } catch (_: SecurityException) { }
    }

    companion object {
        const val CHANNEL = "logger"
        const val DUE = "ch.zerogen.containertrack.LOGGER_DUE"
        const val CONFIRM = "ch.zerogen.containertrack.LOGGER_CONFIRM"
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ReminderAlarms.DUE, ReminderAlarms.CONFIRM)) return
        val id = intent.getIntExtra("id", -1)
        if (id < 1) return
        val pending = goAsync()
        val app = context.applicationContext as ContainerTrackApp
        app.scope.launch {
            try {
                val reminder = app.database.dao().reminder(id) ?: return@launch
                if (reminder.confirmedAt != null) return@launch
                if (intent.action == ReminderAlarms.CONFIRM) {
                    app.repository.confirmReminder(id)
                } else if (reminder.dueAt <= System.currentTimeMillis()) app.alarms.notifyDue(reminder)
                else app.alarms.schedule(reminder)
            } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) return
        val pending = goAsync()
        val app = context.applicationContext as ContainerTrackApp
        app.scope.launch {
            try { app.database.dao().pendingReminders().forEach { app.alarms.schedule(it) } }
            finally { pending.finish() }
        }
    }
}
