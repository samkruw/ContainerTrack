package ch.zerogen.containertrack

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderReceiverTest {
    private val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private fun notificationPermission() = Build.VERSION.SDK_INT < 33 ||
        app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun allowNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val command = "pm grant ${app.packageName} android.permission.POST_NOTIFICATIONS"
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
        assertTrue(notificationPermission())
    }

    private fun setNotificationAppOp(allowed: Boolean) {
        val command = "appops set ${app.packageName} POST_NOTIFICATION ${if (allowed) "allow" else "ignore"}"
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
    }

    private suspend fun dispatch(action: String, id: Int) {
        val finished = CompletableDeferred<Unit>()
        app.sendOrderedBroadcast(Intent(app, ReminderReceiver::class.java).setAction(action).putExtra("id", id), null,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) { finished.complete(Unit) }
            }, Handler(Looper.getMainLooper()), Activity.RESULT_OK, null, null)
        withTimeout(10_000) { finished.await() }
    }

    private suspend fun createDueReminder(): Pair<String, LoggerReminder> {
        val reference = "TEST-LOGGER-${UUID.randomUUID()}"
        val job = app.repository.createJob(JobDraft(JobKind.DELIVERY, reference, "ZRH-1", "20 ft", 1, "Nothing", "Empty"), null)
        app.database.dao().insertReminder(LoggerReminder(jobId = job, referenceNumber = reference, dueAt = System.currentTimeMillis() - 1_000))
        return job to app.database.dao().jobReminder(job)!!
    }

    @Test fun dueReceiverPostsNotificationAndActionConfirmsExactlyOnce() = runBlocking {
        allowNotificationPermission()
        setNotificationAppOp(true)
        val (job, reminder) = createDueReminder()
        try {
            dispatch(ReminderAlarms.DUE, reminder.id)
            withTimeout(5_000) { while (notifications.activeNotifications.none { it.id == reminder.id }) delay(50) }
            val notification = notifications.activeNotifications.first { it.id == reminder.id }.notification
            assertEquals("Logger starten", notification.extras.getString("android.title"))
            assertFalse(notification.extras.getString("android.text").orEmpty().contains(reminder.referenceNumber))
            assertEquals("Logger gestartet", notification.actions.single().title.toString())
            if(Build.VERSION.SDK_INT>=31)assertTrue(notification.actions.single().actionIntent.isActivity)
            assertNull(app.database.dao().reminder(reminder.id)!!.confirmedAt)
            dispatch(ReminderAlarms.CONFIRM, reminder.id)
            val confirmed = app.database.dao().reminder(reminder.id)!!.confirmedAt
            assertNotNull(confirmed)
            dispatch(ReminderAlarms.CONFIRM, reminder.id)
            assertEquals(confirmed, app.database.dao().reminder(reminder.id)!!.confirmedAt)
            withTimeout(5_000) { while (notifications.activeNotifications.any { it.id == reminder.id }) delay(50) }
        } finally { app.repository.deleteJob(job) }
    }

    @Test fun returnConfirmationBroadcastCannotDismissTheEmailReminder() = runBlocking {
        val job=app.repository.createJob(JobDraft(JobKind.RETURN,"TEST-RETURN-${UUID.randomUUID()}",
            orderValue="20 ft",quick="Nothing",additionalWork="Empty"),123L)
        try {
            val reminder=app.database.dao().jobReminder(job)!!
            assertEquals(ReminderKind.RETURN_CLOSE,reminder.kind)
            dispatch(ReminderAlarms.CONFIRM,reminder.id)
            assertNull(app.database.dao().reminder(reminder.id)!!.confirmedAt)
            assertNull(app.database.dao().job(job)!!.job.emailSentAt)
            assertTrue(app.database.dao().pendingReminders().any { it.id==reminder.id })
        } finally { app.repository.deleteJob(job) }
    }

    @Test fun deniedNotificationLeavesReminderPending() = runBlocking {
        setNotificationAppOp(false)
        assertFalse(app.alarms.notificationsAllowed())
        val (job, reminder) = createDueReminder()
        try {
            dispatch(ReminderAlarms.DUE, reminder.id)
            assertNull(app.database.dao().reminder(reminder.id)!!.confirmedAt)
            assertTrue(app.database.dao().pendingReminders().any { it.id == reminder.id })
            assertTrue(notifications.activeNotifications.none { it.id == reminder.id })
        } finally { app.repository.deleteJob(job); setNotificationAppOp(true) }
    }
}
