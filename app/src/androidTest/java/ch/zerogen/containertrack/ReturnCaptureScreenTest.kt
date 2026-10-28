package ch.zerogen.containertrack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Static local fixtures exercise managed Return UI without opening the camera or sharing. */
@RunWith(AndroidJUnit4::class)
class ReturnCaptureScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()

    @Test fun optionalInitialPhotosIncreaseCleanTargetAndRemainIndividuallyReviewable() {
        val fixtureDirectory = File(app.cacheDir, "return-capture-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val batch = fixture(fixtureDirectory)
            val vm = TrackViewModel(app, SavedStateHandle())
            compose.setContent {
                ContainerTheme { PhotoScreen(batch, "return-capture-container", 0, vm, false) {} }
            }

            compose.onNodeWithText("3 Anfangsfotos · 1 / 3 Sauberfotos").assertIsDisplayed()
            compose.onNodeWithText("Sauber · 1/3").assertExists()
            capture("28-reference-return-workflow")

            compose.onNodeWithText("Schaden ergänzen · optional").performScrollTo().assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText("Sonstiges ergänzen · optional").performScrollTo().assertIsDisplayed().assertIsEnabled()
            capture("28b-reference-return-optional-photos")

            val initialNames = listOf("0708_vorher_01_inhalt.jpg", "0708_vorher_02_schaden.jpg", "0708_vorher_03_schaden.jpg")
            initialNames.forEach { name ->
                compose.onAllNodesWithText(name).assertCountEquals(1)
                compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
            }
            compose.onNodeWithText(initialNames[1]).performScrollTo().performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Schaden bei Return").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Schaden bei Return").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Dieses Foto löschen").assertExists()
            capture("28c-reference-return-photo-review")

            compose.onNodeWithText("Zur Fotoübersicht").performScrollTo().performClick()
            compose.onNodeWithText("Sauber · 1/3").performScrollTo().performClick()
            compose.onNodeWithText("Sauberfoto aufnehmen").performScrollTo().assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText("0708_nachher_01_leer_sauber.jpg").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Kamera erlauben").assertDoesNotExist()
            compose.onNodeWithText("Foto aufnehmen").assertDoesNotExist()
        } finally {
            fixtureDirectory.deleteRecursively()
        }
    }

    private fun fixture(directory: File): JobWithContainers {
        val at = 1_792_483_200_000L
        val job = JobEntity(id = "return-capture-job", draft = JobDraft(JobKind.RETURN,
            referenceNumber = "ORD-0708", orderValue = "20 ft", quantity = 1, quick = "Nothing", additionalWork = "Empty"),
            createdAt = at, loggerStoppedAt = at, returnWorkflowVersion = 1)
        val container = ContainerEntity(id = "return-capture-container", jobId = job.id,
            sequenceIndex = 1, containerNumber = "0708")
        val bitmap = InstrumentationRegistry.getInstrumentation().context.assets.open("container_front.jpg").use {
            requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 8 }))
        }
        val categories = listOf(ReturnPhotoCategory.CONTENT, ReturnPhotoCategory.DAMAGE, ReturnPhotoCategory.DAMAGE, ReturnPhotoCategory.CLEAN)
        val photos = try {
            categories.mapIndexed { index, category ->
                val file = File(directory, "photo-$index.jpg")
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
                PhotoEntity(id = "return-capture-photo-$index", containerId = container.id, step = index + 1,
                    localPath = file.absolutePath, capturedAt = at + index,
                    returnPhase = if (category == ReturnPhotoCategory.CLEAN) ReturnPhotoPhase.CLEAN else ReturnPhotoPhase.INITIAL,
                    returnCategory = category)
            }
        } finally {
            bitmap.recycle()
        }
        return JobWithContainers(job, listOf(ContainerWithPhotos(container, photos)))
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
