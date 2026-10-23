package ch.zerogen.containertrack

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class GalleryProfileTest {
    @get:Rule val compose=createComposeRule()
    @Test fun realLocalPhotoOpensAndSharesTheSelectedPhotoId() {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val file=File.createTempFile("gallery", ".jpg",app.cacheDir)
        val bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) };bitmap.recycle()
        val job=JobEntity(id="gallery-job",draft=JobDraft(JobKind.RETURN,"RET-GALLERY",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),createdAt=1L)
        val container=ContainerEntity(id="gallery-container",jobId=job.id,sequenceIndex=1,containerNumber="0708")
        val photo=PhotoEntity(id="gallery-photo",containerId=container.id,step=1,localPath=file.path,capturedAt=1L)
        val missing=photo.copy(id="missing",step=2,localPath=file.path+"-missing")
        var shared:String?=null
        var teamsShared=emptySet<String>()
        try {
            compose.setContent { ContainerTheme { Surface { GalleryScreen(listOf(JobWithContainers(job,listOf(ContainerWithPhotos(container,listOf(photo,missing))))),emptyList(),onShare={shared=it.photoId},onTeamsShareSelected={teamsShared=it}) } } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("RET-GALLERY · 0708").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("RET-GALLERY · 0708").performClick()
            compose.onNodeWithText("0708_02.jpg").assertDoesNotExist()
            compose.onNodeWithText("In Teams teilen").assertIsNotEnabled()
            compose.onNodeWithContentDescription("Foto 1 auswählen").performClick()
            compose.onNodeWithText("In Teams teilen").assertIsEnabled().performClick()
            compose.runOnIdle { assertEquals(setOf("gallery-photo"),teamsShared) }
            compose.onNodeWithContentDescription("0708 Foto 1").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Foto in Vollansicht").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Foto in Vollansicht").assertIsDisplayed()
            compose.onNodeWithText("Foto teilen").performClick()
            compose.runOnIdle { assertEquals("gallery-photo",shared) }
        } finally {file.delete()}
    }
    @Test fun settingsRequiresNoAccountAndOffersNoRoles() {
        compose.setContent { ContainerTheme { Surface { DeviceSettingsScreen() } } }
        compose.onNodeWithText("Einstellungen").assertIsDisplayed()
        compose.onNodeWithText("Mit Microsoft anmelden").assertDoesNotExist()
        compose.onNodeWithText("Admin",substring=true).assertDoesNotExist()
    }
}
