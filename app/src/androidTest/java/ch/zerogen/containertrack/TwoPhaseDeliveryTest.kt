package ch.zerogen.containertrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TwoPhaseDeliveryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun nextContainerIsReachableAfterFivePhotos() {
        val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val vm = TrackViewModel(app,SavedStateHandle())
        val job = JobEntity(id="phase-ui",draft=JobDraft(JobKind.DELIVERY,"PHASE-UI","ZRH","20 ft",3),createdAt=1L)
        val container = ContainerEntity(id="phase-container",jobId=job.id,sequenceIndex=1,containerNumber="0708")
        val photos = (1..5).map { PhotoEntity(containerId=container.id,step=it,localPath="fixture",capturedAt=1L) }
        var scans=0
        compose.setContent { ContainerTheme { JobScreen(JobWithContainers(job,listOf(ContainerWithPhotos(container,photos))),vm,false,{scans++},{},{},{}) } }
        compose.onNodeWithText("Container 2 / 3 erfassen").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,scans) }
    }
}
