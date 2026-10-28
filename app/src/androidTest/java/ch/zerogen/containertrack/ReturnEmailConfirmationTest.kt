package ch.zerogen.containertrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReturnEmailConfirmationTest {
    @get:Rule val compose = createComposeRule()

    private fun batch(preparedRevision: Long? = null): JobWithContainers {
        val job = JobEntity(id = "return-email-ui", draft = JobDraft(JobKind.RETURN, "RET-EXACT-0708",
            orderValue = "20 ft", quick = "Nothing", additionalWork = "Empty"), createdAt = 1L,
            returnWorkflowVersion = 1, documentationRevision = 2L,
            emailPreparedRevision = preparedRevision, emailPreparedAt = preparedRevision?.let { 2L })
        val container = ContainerEntity(id = "return-email-container", jobId = job.id, sequenceIndex = 1, containerNumber = "0708")
        val initial = PhotoEntity(id = "return-email-initial", containerId = container.id, step = 1, localPath = "fixture", capturedAt = 1L)
        val clean = initial.copy(id = "return-email-clean", step = 2, returnPhase = ReturnPhotoPhase.CLEAN, returnCategory = ReturnPhotoCategory.CLEAN)
        return JobWithContainers(job, listOf(ContainerWithPhotos(container, listOf(initial, clean))))
    }

    @Test fun stalePreparationCannotConfirmAndEmailSubjectIsReadOnly() {
        var prepares = 0
        var confirmations = 0
        compose.setContent { ContainerTheme { UploadScreen(batch(1L), false, { prepares++ }, {}, { confirmations++ }) } }
        compose.onNodeWithText("Betreff: RET-EXACT-0708").assertExists()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("E-Mail gesendet").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("E-Mail vorbereiten").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, prepares)
            assertEquals(0, confirmations)
        }
    }

    @Test fun currentPreparationRequiresAnExplicitConfirmationInTheDialog() {
        var confirmations = 0
        compose.setContent { ContainerTheme { UploadScreen(batch(2L), false, {}, {}, { confirmations++ }) } }
        compose.onNodeWithText("E-Mail gesendet").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("E-Mail gesendet?").assertExists()
        compose.runOnIdle { assertEquals(0, confirmations) }
        compose.onNodeWithText("Zurück").performClick()
        compose.runOnIdle { assertEquals(0, confirmations) }
        compose.onNodeWithText("E-Mail gesendet").performScrollTo().performClick()
        compose.onAllNodesWithText("E-Mail gesendet").onLast().performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
    }
}
