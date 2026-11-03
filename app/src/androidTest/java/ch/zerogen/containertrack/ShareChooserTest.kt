package ch.zerogen.containertrack

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class ShareChooserTest {
    // Grant before launching MainActivity so reminder setup cannot open a system dialog.
    @get:Rule(order = 0) val notifications: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Suppress("DEPRECATION")
    @Test fun exportShareButtonLaunchesAndroidChooser(): Unit = runBlocking {
        val app = compose.activity.application as ContainerTrackApp
        val reference = "CHOOSER-${UUID.randomUUID()}"
        val job = app.repository.createJob(JobDraft(JobKind.DELIVERY, referenceNumber = reference,
            orderValue = "20 ft"), null)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val launches = ConcurrentLinkedQueue<Intent>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CHOOSER && intent.action != Intent.ACTION_SEND &&
                    intent.action != Intent.ACTION_SEND_MULTIPLE) return null
                launches.add(Intent(intent))
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            val container = app.repository.addContainer(job, "0708")
            for (step in 1..6) {
                val photo = File.createTempFile("chooser-photo", ".jpg", app.cacheDir).apply { writeBytes(byteArrayOf(step.toByte())) }
                app.repository.savePhoto(job, container, step, photo)
            }
            // A reminder opens the actual job details independently of its active-card position.
            compose.runOnUiThread {
                instrumentation.callActivityOnNewIntent(compose.activity,
                    Intent(compose.activity, MainActivity::class.java).putExtra("reminderJobId", job))
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Auftragsdetails").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(reference).assertExists()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export prüfen"))
            compose.onNodeWithText("Export prüfen").assertIsEnabled().performClick()
            compose.onNodeWithText("ZIP teilen").performScrollTo().performClick()
            // Preparing the ZIP and dispatching the share are asynchronous, including on slow emulators.
            compose.waitUntil(60_000) { launches.isNotEmpty() }
            compose.onNodeWithText("ZIP teilen").assertIsEnabled()
            assertEquals(1, launches.size)
            val chooser = launches.single()
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            val share = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertEquals(Intent.ACTION_SEND, share.action)
            assertEquals("application/zip", share.type)
            assertEquals(reference, share.getStringExtra(Intent.EXTRA_SUBJECT))
            val uri = requireNotNull(share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            assertEquals("content", uri.scheme)
            assertEquals("${app.packageName}.exports", uri.authority)
            assertEquals("application/zip", app.contentResolver.getType(uri))
            for (intent in listOf(chooser, share)) {
                assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
            }
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("$reference.zip", cursor.getString(0))
            }
            ZipInputStream(requireNotNull(app.contentResolver.openInputStream(uri))).use { zip ->
                assertEquals("$reference/0708/0708_01.jpg", requireNotNull(zip.nextEntry).name)
                assertArrayEquals(byteArrayOf(1), zip.readBytes())
            }
        } finally {
            // Keep intercepting shares until destruction cancels any pending handoff.
            try { compose.activityRule.scenario.close() }
            finally {
                instrumentation.removeMonitor(monitor)
                app.repository.deleteJob(job)
            }
        }
    }
}
