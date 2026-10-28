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
    @Test fun oneSelectedPhotoPreparesItsWholeOrderAndPreviewShowsOriginal() {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val file=File.createTempFile("gallery", ".jpg",app.cacheDir)
        val bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) };bitmap.recycle()
        val job=JobEntity(id="gallery-job",draft=JobDraft(JobKind.RETURN,"RET-GALLERY",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),createdAt=1L,returnWorkflowVersion=1)
        val container=ContainerEntity(id="gallery-container",jobId=job.id,sequenceIndex=1,containerNumber="0708")
        val photo=PhotoEntity(id="gallery-photo",containerId=container.id,step=1,localPath=file.path,capturedAt=1L)
        val clean=photo.copy(id="gallery-clean",step=2,returnPhase=ReturnPhotoPhase.CLEAN,returnCategory=ReturnPhotoCategory.CLEAN)
        val missingJob=job.copy(id="missing-job",draft=job.draft.copy(referenceNumber="MISSING"))
        val missingContainer=container.copy(id="missing-container",jobId=missingJob.id)
        val missing=photo.copy(id="missing",containerId=missingContainer.id,localPath=file.path+"-missing")
        var shared:String?=null
        try {
            compose.setContent { ContainerTheme { Surface { GalleryScreen(listOf(
                JobWithContainers(job,listOf(ContainerWithPhotos(container,listOf(photo,clean)))),
                JobWithContainers(missingJob,listOf(ContainerWithPhotos(missingContainer,listOf(missing)))))
                ,emptyList(),onShare={shared=it.jobId}) } } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("RET-GALLERY · 0708").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("MISSING · 0708").assertDoesNotExist()
            compose.onNodeWithText("RET-GALLERY · 0708").performClick()
            compose.onNodeWithText("0708_vorher_01_inhalt.jpg").assertExists()
            compose.onNodeWithText("0708_nachher_01_leer_sauber.jpg").assertExists()
            compose.onNodeWithText("Auftrag per E-Mail").assertIsNotEnabled()
            compose.onNodeWithText("Auftragsinfos kopieren").assertIsNotEnabled()
            compose.onNodeWithContentDescription("Foto 1 auswählen").performClick()
            compose.onNodeWithText("1 von 2 Fotos ausgewählt").assertExists()
            compose.onNodeWithText("Die E-Mail enthält alle 2 Fotos aller Container dieses Auftrags.").assertExists()
            compose.onNodeWithText("Auftrag per E-Mail").assertIsEnabled().performClick()
            compose.runOnIdle { assertEquals(job.id,shared) }
            compose.onNodeWithText("Auftragsinfos kopieren").assertIsEnabled().performClick()
            compose.runOnIdle {
                val clipboard=app.getSystemService(android.content.ClipboardManager::class.java)
                val text=clipboard.primaryClip!!.getItemAt(0).text.toString()
                org.junit.Assert.assertTrue(text.contains("Auftrag: RET-GALLERY"))
                org.junit.Assert.assertTrue(text.contains("Container: 0708"))
                org.junit.Assert.assertTrue(text.contains("Fotos: 1"))
            }
            compose.onNodeWithContentDescription("Inhalt bei Return").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Foto in Vollansicht").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Foto in Vollansicht").assertIsDisplayed()
            compose.onNodeWithText("Alle 2 Fotos dieses Auftrags werden angehängt.").assertExists()
            compose.onAllNodesWithText("Auftrag per E-Mail").onLast().performClick()
            compose.runOnIdle { assertEquals(job.id,shared) }
        } finally {file.delete()}
    }
    @Test fun mixedOrderSelectionStaysDisabledWhenAContainerIsOpened() {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val file=File.createTempFile("gallery-orders", ".jpg",app.cacheDir)
        val bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) };bitmap.recycle()
        val jobs=(1..2).map { index ->
            val job=JobEntity(id="gallery-job-$index",draft=JobDraft(JobKind.RETURN,"RET-GALLERY-$index",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),createdAt=1L,returnWorkflowVersion=1)
            val container=ContainerEntity(id="gallery-container-$index",jobId=job.id,sequenceIndex=1,containerNumber="070$index")
            val initial=PhotoEntity(id="initial-$index",containerId=container.id,step=1,localPath=file.path,capturedAt=1L)
            val clean=initial.copy(id="clean-$index",step=2,returnPhase=ReturnPhotoPhase.CLEAN,returnCategory=ReturnPhotoCategory.CLEAN)
            JobWithContainers(job,listOf(ContainerWithPhotos(container,listOf(initial,clean))))
        }
        var shares=0
        try {
            compose.setContent { ContainerTheme { Surface { GalleryScreen(jobs,emptyList(),{shares++}) } } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("RET-GALLERY-2 · 0702").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodes(isToggleable())[0].performClick()
            compose.onAllNodes(isToggleable())[1].performClick()
            compose.onNodeWithText("4 von 4 Fotos ausgewählt").assertExists()
            compose.onNodeWithText("Auftrag per E-Mail").assertIsNotEnabled()
            compose.onNodeWithText("RET-GALLERY-1 · 0701").performClick()
            compose.onNodeWithText("Auftrag per E-Mail").assertIsNotEnabled()
            compose.onNodeWithText("Für eine E-Mail bitte Fotos aus genau einem Auftrag auswählen.").assertExists()
            compose.runOnIdle { assertEquals(0,shares) }
        } finally { file.delete() }
    }
    @Test fun settingsRequiresNoAccountAndOffersNoRoles() {
        compose.setContent { ContainerTheme { Surface { DeviceSettingsScreen() } } }
        compose.onNodeWithText("Einstellungen").assertIsDisplayed()
        compose.onNodeWithText("Mit Microsoft anmelden").assertDoesNotExist()
        compose.onNodeWithText("Admin",substring=true).assertDoesNotExist()
    }
}
