package ch.zerogen.containertrack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Regression: important capture actions must fit the viewport without a scroll gesture. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AdaptiveCaptureLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()

    private data class Viewport(val width: Int, val height: Int, val fontScale: Float = 1f)
    private val viewports = listOf(
        Viewport(320, 480), Viewport(360, 640), Viewport(640, 320), Viewport(320, 480, 1.5f),
        // Also reserve space for the real activity's compact toolbar and system bars.
        Viewport(320, 384), Viewport(360, 544), Viewport(640, 224), Viewport(320, 384, 1.5f),
        Viewport(640, 224, 1.5f), Viewport(320, 360, 1.5f)
    )

    @Test fun cameraPermissionActionsFitShortPreviewWithLargeText() {
        val sizes = listOf(Viewport(320, 96, 1.5f), Viewport(320, 120, 1.5f), Viewport(320, 64, 1.5f))
        var viewport by mutableStateOf(sizes.first())
        var allowed = 0
        var settings = 0
        compose.setContent {
            TestViewport(viewport) {
                CameraPermissionContent(onAllow = { allowed++ }, onSettings = { settings++ })
            }
        }
        sizes.forEachIndexed { index, size ->
            compose.runOnIdle { viewport = size }
            val allow = compose.onNode(hasText("Kamera erlauben") and hasClickAction())
            val openSettings = compose.onNode(hasText("Einstellungen") and hasClickAction())
            listOf(allow, openSettings).forEach { action ->
                action.assertIsEnabled().assertHeightIsAtLeast(48.dp)
                assertInsideViewport(action, "Permission action at $size")
            }
            assertNoOverlap(allow.getUnclippedBoundsInRoot(), openSettings.getUnclippedBoundsInRoot(), "Permission actions at $size")
            compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
            if (size.height < 90) compose.onNodeWithText("Kamerazugriff benötigt").assertDoesNotExist()
            allow.performClick()
            openSettings.performClick()
            compose.runOnIdle { assertEquals(index + 1, allowed); assertEquals(index + 1, settings) }
        }
    }

    @Test fun shutterAndPhotoActionsStayVisibleAsViewportChanges() {
        var viewport by mutableStateOf(viewports.first())
        var captures = 0
        var photoLists = 0
        compose.setContent {
            TestViewport(viewport) {
                CaptureWorkspace(
                    header = { CaptureContextHeader("Container 0708", "ORDER-2026-0708 · Lieferung", "Foto 1 / 6") },
                    media = { modifier ->
                        CameraCaptureLayout(modifier = modifier, onCapture = { captures++ }) {
                            Box(Modifier.fillMaxSize().background(Color.DarkGray))
                        }
                    },
                    actions = {
                        Button(onClick = { photoLists++ }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Fotos (3)")
                        }
                    }
                )
            }
        }
        viewports.forEachIndexed { index, size ->
            compose.runOnIdle { viewport = size }
            val shutter = compose.onNodeWithTag("camera-shutter")
            val photos = compose.onNode(hasText("Fotos (3)") and hasClickAction())
            assertInsideViewport(shutter, size.toString())
            assertInsideViewport(photos, size.toString())
            shutter.assertHeightIsAtLeast(48.dp)
            photos.assertHeightIsAtLeast(48.dp)
            val preview = compose.onNodeWithTag("camera-preview").getUnclippedBoundsInRoot()
            assertTrue("A visible preview remains at $size: $preview", preview.right > preview.left && preview.bottom > preview.top)
            assertNoOverlap(preview, shutter.getUnclippedBoundsInRoot(), "Preview and shutter at $size")
            assertNoOverlap(shutter.getUnclippedBoundsInRoot(), photos.getUnclippedBoundsInRoot(), "Capture actions at $size")
            shutter.performClick()
            photos.performClick()
            compose.runOnIdle { assertEquals(index + 1, captures); assertEquals(index + 1, photoLists) }
        }
    }

    @Test fun deliveryPendingPhotoCanBeConfirmedRetakenOrDiscardedWithoutScrolling() = pendingPhotoFits(JobKind.DELIVERY)

    @Test fun returnPendingPhotoCanBeConfirmedRetakenOrDiscardedWithoutScrolling() = pendingPhotoFits(JobKind.RETURN)

    private fun pendingPhotoFits(kind: JobKind) {
        val id = "adaptive-${UUID.randomUUID()}"
        val vm = TrackViewModel(app, SavedStateHandle())
        val batch = JobWithContainers(
            JobEntity(id = id, draft = JobDraft(kind, referenceNumber = "ORDER-2026-0708", orderValue = "20 ft", quantity = 1,
                quick = "Nothing", additionalWork = "Empty"), createdAt = 1_792_483_200_000L,
                returnWorkflowVersion = if (kind == JobKind.RETURN) 1 else 0),
            listOf(ContainerWithPhotos(ContainerEntity(id = "$id-container", jobId = id, sequenceIndex = 1,
                containerNumber = "0708"), emptyList())))
        val containerId = batch.containers.single().container.id
        val captured = File(app.cacheDir, "$id.jpg")
        val bitmap = InstrumentationRegistry.getInstrumentation().context.assets.open("container_front.jpg").use {
            requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 8 }))
        }
        captured.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
        bitmap.recycle()
        var viewport by mutableStateOf(viewports.first())
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                if (kind == JobKind.RETURN) vm.stageReturnPhoto(id, containerId, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT, null, captured)
                else vm.stagePhoto(id, containerId, 1, captured)
            }
            compose.waitUntil(10_000) { vm.pendingPhoto(id, containerId).value != null }
            compose.setContent {
                TestViewport(viewport) { PhotoScreen(batch, containerId, 0, vm, false) {} }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Vorschau", substring = true).fetchSemanticsNodes().isNotEmpty() }
            viewports.forEach { size ->
                compose.runOnIdle { viewport = size }
                val actions = listOf("Foto verwenden", "Erneut aufnehmen", "Aufnahme verwerfen").map { label ->
                    compose.onNode(actionMatcher(label)).also {
                        it.assertIsEnabled().assertHeightIsAtLeast(48.dp)
                        assertInsideViewport(it, "$kind / $label / $size")
                    }.getUnclippedBoundsInRoot()
                }
                actions.forEachIndexed { index, first -> actions.drop(index + 1).forEach { second ->
                    assertNoOverlap(first, second, "$kind actions at $size")
                } }
                val preview = compose.onNodeWithContentDescription("Vorschau", substring = true)
                assertInsideViewport(preview, "$kind preview at $size")
                val previewBounds = preview.getUnclippedBoundsInRoot()
                assertTrue("Preview has positive height at $size", previewBounds.bottom > previewBounds.top)
                if (kind == JobKind.RETURN) {
                    listOf(compose.onNodeWithText("Anfang · 0"), compose.onNodeWithText("Sauber · 0/0"),
                        compose.onNodeWithContentDescription("Return-Fotohinweise")).forEach { action ->
                        assertInsideViewport(action, "Return phase/info action at $size")
                        action.assertHeightIsAtLeast(48.dp)
                    }
                }
                compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
                if (size == Viewport(320, 480) || size == Viewport(640, 224) ||
                    size == Viewport(320, 384, 1.5f) || size == Viewport(640, 224, 1.5f) || size == Viewport(320, 360, 1.5f)) {
                    capture("adaptive-${kind.name.lowercase()}-${size.width}x${size.height}-font${size.fontScale}")
                }
            }
            compose.onNode(actionMatcher("Aufnahme verwerfen")).performClick()
            compose.runOnIdle { assertNull(vm.pendingPhoto(id, containerId).value) }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                vm.discardPhoto(id, containerId)
                vm.viewModelScope.cancel()
            }
            captured.delete()
            File(app.filesDir, "pending/$id").deleteRecursively()
        }
    }

    private fun actionMatcher(label: String): SemanticsMatcher {
        val shortLabel = when (label) {
            "Aufnahme verwerfen" -> "Verwerfen"
            "Erneut aufnehmen" -> "Neu aufnehmen"
            else -> label
        }
        return (hasText(label) or hasText(shortLabel) or hasContentDescription(label)) and hasClickAction()
    }

    private fun capture(name: String) {
        val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag("capture-test-viewport").captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    @Composable private fun TestViewport(viewport: Viewport, content: @Composable () -> Unit) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(viewport.width.dp, viewport.height.dp))) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(viewport.fontScale)) {
                ContainerTheme {
                    Surface(Modifier.fillMaxSize().testTag("capture-test-viewport")) { content() }
                }
            }
        }
    }

    private fun assertInsideViewport(node: SemanticsNodeInteraction, label: String) {
        node.assertIsDisplayed()
        val viewport = compose.onNodeWithTag("capture-test-viewport").getUnclippedBoundsInRoot()
        val bounds = node.getUnclippedBoundsInRoot()
        val tolerance = 1.dp
        assertTrue("$label must fit entirely in $viewport, actual $bounds",
            bounds.left >= viewport.left - tolerance && bounds.top >= viewport.top - tolerance &&
                bounds.right <= viewport.right + tolerance && bounds.bottom <= viewport.bottom + tolerance)
    }

    private fun assertNoOverlap(first: DpRect, second: DpRect, label: String) {
        assertTrue("$label overlap: $first / $second",
            first.right <= second.left + 1.dp || second.right <= first.left + 1.dp ||
                first.bottom <= second.top + 1.dp || second.bottom <= first.top + 1.dp)
    }
}
