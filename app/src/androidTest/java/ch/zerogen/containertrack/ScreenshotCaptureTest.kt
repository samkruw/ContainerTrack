package ch.zerogen.containertrack

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Capture real Compose screens on a fixed emulator. Baseline comparison is tools/compare_screenshots.py. */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()
    private val form = JobDraft(JobKind.DELIVERY, "ORD-0708", "ZRH-0123", "20 ft", 1, "Nothing", "Empty")
    private fun snapshot(kind: JobKind = JobKind.DELIVERY, complete: Boolean = false): JobWithContainers {
        val job = JobEntity(id = "screenshot", draft = form.copy(kind = kind), createdAt = 1_792_483_200_000L)
        val container = ContainerEntity(id = "container", jobId = job.id, sequenceIndex = 1, containerNumber = "0708")
        val photos = if (complete) (1..kind.photoCount).map { PhotoEntity(id = "$it", containerId = container.id, step = it, localPath = "test-only", capturedAt = job.createdAt) } else emptyList()
        return JobWithContainers(job, listOf(ContainerWithPhotos(container, photos)))
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val image = compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun start() {
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { HomeScreen(emptyList(), {}, {}) } } }
        capture("01-start")
    }
    @Test fun deliverySetup() {
        val vm = TrackViewModel(app, SavedStateHandle())
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { SetupScreen(vm, false) {} } } }
        capture("02-delivery-setup")
    }
    @Test fun scanPermissionFallback() {
        val vm = TrackViewModel(app, SavedStateHandle())
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { ScanScreen(snapshot(), vm, false) {} } } }
        capture("03-ocr-scan")
    }
    @Test fun deliveryPhoto() {
        val vm = TrackViewModel(app, SavedStateHandle())
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { PhotoScreen(snapshot(), "container", 0, vm, false) {} } } }
        capture("04-delivery-photo")
    }
    @Test fun returnLoggerGate() {
        val vm = TrackViewModel(app, SavedStateHandle())
        vm.begin(JobKind.RETURN)
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { SetupScreen(vm, false) {} } } }
        capture("05-return-logger")
    }
    @Test fun returnSetup() {
        val vm = TrackViewModel(app, SavedStateHandle(mapOf("formKind" to JobKind.RETURN.name, "formStoppedAt" to 1_792_483_200_000L)))
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { SetupScreen(vm, false) {} } } }
        capture("06-return-setup")
    }
    @Test fun exportReview() {
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { ExportReviewScreen(snapshot(complete = true), false, {}) {} } } }
        capture("07-export")
        compose.onNodeWithText("ZIP teilen").performScrollTo()
        capture("07b-export-actions")
    }
    @Test fun loggerDue() {
        compose.setContent { ContainerTheme {
            Surface(color = Color.Transparent) { Text("Aufträge", Modifier.padding(16.dp)) }
            AlertDialog(onDismissRequest = {}, title = { Text("Logger jetzt starten") },
                text = { Text("Auftrag ORD-0708\nBitte Logger starten und anschliessend bestätigen.") },
                confirmButton = { TextButton(onClick = {}) { Text("Logger gestartet – bestätigen") } })
        } }
        capture("08-logger-due")
    }
    @Test fun jobDetail() {
        val vm = TrackViewModel(app, SavedStateHandle())
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) {
            JobScreen(snapshot(), vm, false, {}, {}, {}, {})
        } } }
        capture("09-job-detail")
    }
    @Test fun jobsHistory() {
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { JobsScreen(listOf(snapshot()), {}) } } }
        capture("10-history")
    }
    @Test fun returnPhoto() {
        val vm = TrackViewModel(app, SavedStateHandle())
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { PhotoScreen(snapshot(JobKind.RETURN), "container", 0, vm, false) {} } } }
        capture("11-return-photo")
    }
    @Test fun permissions() {
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) { MoreScreen() } } }
        capture("12-permissions")
    }
    @Test fun activeLogger() {
        val now = 1_792_483_200_000L
        compose.setContent { ContainerTheme { Surface(color = Color.Transparent) {
            ReminderStrip(LoggerReminder(jobId = "screenshot", referenceNumber = "ORD-0708", dueAt = now + 900_000L), 0, now)
        } } }
        capture("13-logger-active")
    }

}
