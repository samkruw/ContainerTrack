package ch.zerogen.containertrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun batch(kind: JobKind): JobWithContainers {
        val job = JobEntity(draft = JobDraft(kind, "ORD-SHARE", "ZRH-1", "20 ft", 1, "Nothing", "Empty"), createdAt = 1L,
            returnWorkflowVersion = if (kind == JobKind.RETURN) 1 else 0)
        val container = ContainerEntity(jobId = job.id, sequenceIndex = 1, containerNumber = "0708")
        val photos = if (kind == JobKind.RETURN) listOf(
            PhotoEntity(containerId=container.id,step=1,localPath="fixture",capturedAt=1L),
            PhotoEntity(containerId=container.id,step=2,localPath="fixture",capturedAt=1L,
                returnPhase=ReturnPhotoPhase.CLEAN,returnCategory=ReturnPhotoCategory.CLEAN))
            else (1..kind.photoCount).map { PhotoEntity(containerId = container.id, step = it, localPath = "fixture", capturedAt = 1L) }
        return JobWithContainers(job, listOf(ContainerWithPhotos(container, photos)))
    }

    @Test fun deliveryHidesExtrasAndOffersIndependentShareAndSaveActions() {
        var shares = 0; var saves = 0
        compose.setContent { ContainerTheme { ExportReviewScreen(batch(JobKind.DELIVERY), false, { shares++ }, { saves++ }) } }
        compose.onNodeWithText("Quick").assertDoesNotExist()
        compose.onNodeWithText("additional Work").assertDoesNotExist()
        compose.onNodeWithText("ZIP teilen").performScrollTo().performClick()
        compose.onNodeWithText("ZIP speichern").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, shares); assertEquals(1, saves) }
    }

    @Test fun returnShowsExtrasAndBusyStatePreventsDuplicateExport() {
        compose.setContent { ContainerTheme { ExportReviewScreen(batch(JobKind.RETURN), true, {}, {}) } }
        compose.onNodeWithText("additional Work").assertExists()
        compose.onNodeWithText("Empty").assertExists()
        compose.onNodeWithText("Quick").assertExists()
        compose.onNodeWithText("Nothing").assertExists()
        compose.onNodeWithText("ZIP wird erstellt …").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("ZIP speichern").performScrollTo().assertIsNotEnabled()
    }
}
