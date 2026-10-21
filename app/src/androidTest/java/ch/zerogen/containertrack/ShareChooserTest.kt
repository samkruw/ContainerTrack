package ch.zerogen.containertrack

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ShareChooserTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun exportShareButtonLaunchesAndroidChooser() = runBlocking {
        val app = compose.activity.application as ContainerTrackApp
        val reference = "CHOOSER-${UUID.randomUUID()}"
        val job = app.repository.createJob(JobDraft(JobKind.RETURN, referenceNumber = reference,
            orderValue = "20 ft", quick = "Nothing", additionalWork = "Empty"), 123L)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_CHOOSER),
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            val container = app.repository.addContainer(job, "0708")
            val photo = File.createTempFile("chooser-photo", ".jpg", app.cacheDir).apply { writeBytes(byteArrayOf(1)) }
            app.repository.savePhoto(job, container, 1, photo)
            compose.onNodeWithText("Export").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(reference).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(reference).performClick()
            compose.onNodeWithText("ZIP teilen").performScrollTo().performClick()
            compose.waitUntil(10_000) { monitor.hits > 0 }
            assertEquals(1, monitor.hits)
            compose.onNodeWithText("ZIP teilen").assertIsEnabled()
        } finally {
            instrumentation.removeMonitor(monitor)
            app.repository.deleteJob(job)
        }
    }
}
