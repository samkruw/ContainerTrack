package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipInputStream

class ReturnWorkflowTest {
    private fun photo(step: Int, phase: ReturnPhotoPhase = ReturnPhotoPhase.INITIAL,
        category: ReturnPhotoCategory = ReturnPhotoCategory.CONTENT) = PhotoEntity(
        id = "photo-$step", containerId = "container", step = step,
        localPath = "/photos/$step.jpg", capturedAt = step.toLong(),
        returnPhase = phase, returnCategory = category)

    private fun batch(photos: List<PhotoEntity>, version: Int = 1, emailSentAt: Long? = null,
        quantity: Int = 1, exportedAt: Long? = null) = JobWithContainers(
        JobEntity(id = "job", draft = JobDraft(JobKind.RETURN, referenceNumber = "RET-1", quantity = quantity),
            createdAt = 1L, returnWorkflowVersion = version, emailSentAt = emailSentAt, exportedAt = exportedAt),
        listOf(ContainerWithPhotos(ContainerEntity(id = "container", jobId = "job", sequenceIndex = 1,
            containerNumber = "0708"), photos)))

    @Test fun allInitialPhotosNeedMatchingCleanPhotos() {
        val initial = listOf(photo(1), photo(2, category = ReturnPhotoCategory.DAMAGE),
            photo(3, category = ReturnPhotoCategory.OTHER))
        val partial = initial + photo(4, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
        val progress = returnProgress(partial)
        assertEquals(3, progress.initialCount)
        assertEquals(1, progress.cleanCount)
        assertTrue(progress.hasContent)
        assertFalse(progress.complete)
        val complete = partial + photo(5, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN) +
            photo(6, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
        assertTrue(returnProgress(complete).complete)
        assertFalse(returnProgress(complete + photo(7, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)).complete)
    }

    @Test fun contentIsRequiredAndPhaseCategoriesCannotBeMixed() {
        val clean = photo(2, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
        assertFalse(returnProgress(emptyList()).complete)
        assertFalse(returnProgress(listOf(photo(1, category = ReturnPhotoCategory.DAMAGE), clean)).hasContent)
        assertFalse(returnProgress(listOf(photo(1, category = ReturnPhotoCategory.DAMAGE), clean)).complete)
        assertFalse(returnProgress(listOf(photo(1, category = ReturnPhotoCategory.CLEAN), clean)).complete)
        assertFalse(returnProgress(listOf(photo(1), photo(2, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CONTENT))).complete)
        assertFalse(returnProgress(listOf(photo(1), photo(2), clean.copy(step = 3), clean.copy(step = 4))).complete)
    }

    @Test fun stableInterleavedStepsAreAllowedButDuplicateOrNonpositiveStepsAreNot() {
        val interleaved = listOf(photo(1), photo(2, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN),
            photo(5, category = ReturnPhotoCategory.DAMAGE), photo(8, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN))
        assertTrue(returnProgress(interleaved).complete)
        assertFalse(returnProgress(listOf(photo(0), photo(2, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN))).complete)
        assertFalse(returnProgress(listOf(photo(1), photo(1, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN))).complete)
    }

    @Test fun cleanCompletionStillWaitsForExplicitEmailConfirmation() {
        val initial = batch(listOf(photo(1)), exportedAt = 3L)
        assertTrue(initial.isManagedReturn)
        assertTrue(initial.initialDocumentationComplete)
        assertFalse(initial.complete)
        assertEquals(WorkflowPhase.RETURN_READY_FOR_CLEAN, initial.phase)
        val documented = batch(listOf(photo(1), photo(3, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)), exportedAt = 3L)
        assertTrue(documented.complete)
        assertEquals(WorkflowPhase.RETURN_AWAITING_EMAIL, documented.phase)
        assertEquals(WorkflowPhase.COMPLETE, documented.copy(job = documented.job.copy(emailSentAt = 5L)).phase)
        assertFalse(batch(listOf(photo(1)), quantity = 2).initialDocumentationComplete)
        assertFalse(batch(listOf(photo(1, category = ReturnPhotoCategory.DAMAGE))).initialDocumentationComplete)
        assertFalse(batch(listOf(photo(1), photo(2))).initialDocumentationComplete)
    }

    @Test fun completedLegacyReturnsKeepTheirOriginalSinglePhotoRule() {
        val legacy = batch(listOf(photo(1)), version = 0)
        assertFalse(legacy.isManagedReturn)
        assertTrue(legacy.complete)
        assertEquals(WorkflowPhase.COMPLETE, legacy.phase)
        assertEquals("0708_01.jpg", namedPhotos(legacy).single().fileName)
    }

    @Test fun namesUsePhaseOrdinalsWithContentFirstWithoutChangingSteps() {
        val photos = listOf(photo(2, category = ReturnPhotoCategory.DAMAGE),
            photo(8), photo(3, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN),
            photo(10, category = ReturnPhotoCategory.OTHER), photo(11, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN))
        val names = namedPhotos(batch(photos))
        assertEquals(listOf("0708_vorher_01_inhalt.jpg", "0708_vorher_02_schaden.jpg", "0708_vorher_03_zusatz.jpg",
            "0708_nachher_01_leer_sauber.jpg", "0708_nachher_02_leer_sauber.jpg"), names.map { it.fileName })
        assertEquals(listOf(8, 2, 10, 3, 11), names.map { it.photo.step })
        assertTrue(names.all { it.containerNumber == "0708" && it.label.isNotBlank() })
    }

    @Test fun sharingNamesAcceptOnlySupportedBasenames() {
        for (name in listOf("0708_01.jpg", "0708_06.jpg", "0708_vorher_01_inhalt.jpg",
            "0708_vorher_32_schaden.jpg", "0708_vorher_100_zusatz.jpg", "0708_nachher_01_leer_sauber.jpg"))
            assertTrue(name, isSharePhotoName(name))
        for (name in listOf("../0708_01.jpg", "/0708_01.jpg", "0708_07.jpg", "708_01.jpg",
            "0708_vorher_00_inhalt.jpg", "0708_vorher_001_inhalt.jpg", "0708_nachher_01_inhalt.jpg",
            "0708_vorher_01_leer_sauber.jpg", "0708_01.jpg\n", "0708_01.JPG"))
            assertFalse(name, isSharePhotoName(name))
    }

    @Test fun closeReminderAlwaysUsesCreationTime() {
        assertEquals(86_401_000L, returnReminderDueAt(1_000L))
    }

    @Test fun managedZipUsesSameNamesAndRejectsUnbalancedPhasesBeforeWriting() {
        val file = File.createTempFile("return-photo", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            val photos = listOf(ExportPhoto(1, file), ExportPhoto(4, file, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.DAMAGE),
                ExportPhoto(2, file, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN),
                ExportPhoto(7, file, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN))
            val draft = JobDraft(JobKind.RETURN, referenceNumber = "RET-1")
            val out = ByteArrayOutputStream()
            writeExportZip(out, draft, listOf(ExportContainer("0708", photos)), "{}", LocalDate.of(2026, 1, 2), managedReturn = true)
            val names = mutableListOf<String>()
            ZipInputStream(out.toByteArray().inputStream()).use { zip ->
                while (true) names += (zip.nextEntry ?: break).name
            }
            assertEquals(listOf("RET-1/0708/0708_vorher_01_inhalt.jpg", "RET-1/0708/0708_vorher_02_schaden.jpg",
                "RET-1/0708/0708_nachher_01_leer_sauber.jpg", "RET-1/0708/0708_nachher_02_leer_sauber.jpg",
                "RET-1/RET-1_EXCEL.txt", "RET-1/RET-1_META.json"), names)
            val incomplete = ByteArrayOutputStream()
            try {
                writeExportZip(incomplete, draft, listOf(ExportContainer("0708", photos.dropLast(1))), "{}", LocalDate.now(), managedReturn = true)
                fail("Unbalanced Return accepted")
            } catch (_: IllegalArgumentException) { assertEquals(0, incomplete.size()) }
        } finally { file.delete() }
    }
}
