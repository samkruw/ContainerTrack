package ch.zerogen.containertrack

import androidx.compose.ui.test.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Container 2 / 3 erfassen"))
        compose.onNodeWithText("Container 2 / 3 erfassen").performClick()
        compose.runOnIdle { assertEquals(1,scans) }
    }
    @Test fun tenthContainerAndFilledSlotRemainReachableWithLargeFont() {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val vm=TrackViewModel(app,SavedStateHandle())
        val job=JobEntity(id="ten",draft=JobDraft(JobKind.DELIVERY,"LONG-ORDER-REFERENCE-01234567890123456789","KT-LONG","20 ft",10),createdAt=1L)
        val containers=(1..10).map { n ->
            val c=ContainerEntity(id="c$n",jobId=job.id,sequenceIndex=n,containerNumber=(707+n).toString().padStart(4,'0'))
            ContainerWithPhotos(c,(1..5).map { PhotoEntity(containerId=c.id,step=it,localPath="fixture",capturedAt=1L) })
        }
        var chosen:Pair<String,Int>?=null
        compose.setContent { val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                ContainerTheme { JobScreen(JobWithContainers(job,containers),vm,false,{},{},{},{},onPhoto={id,step->chosen=id to step}) }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(11)
        compose.onNodeWithText("0717 · 5/6 Fotos").performClick()
        compose.onNodeWithText("06 · Befüllt").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("c10" to 6,chosen) }
    }

}
