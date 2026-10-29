package ch.zerogen.containertrack

import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SelectedPhotoSharingTest {
    @Suppress("DEPRECATION")
    @Test fun finishingRequiresAllPhasesAndEmailPreparationPreservesCompletion(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val db = Room.inMemoryDatabaseBuilder(app, TrackDatabase::class.java).build()
        val repo = TrackRepository(app, db)
        val id = repo.createJob(JobDraft(JobKind.RETURN, "FINISH", orderValue = "20 ft", quantity = 2,
            quick = "Nothing", additionalWork = "Empty"), 123L)
        suspend fun capture(container: String, phase: ReturnPhotoPhase, value: Int) {
            val category = if (phase == ReturnPhotoPhase.INITIAL) ReturnPhotoCategory.CONTENT else ReturnPhotoCategory.CLEAN
            repo.saveReturnPhoto(id, container, phase, category,
                File.createTempFile("finish", ".jpg", app.cacheDir).apply { writeBytes(byteArrayOf(value.toByte())) })
        }
        try {
            val c1 = repo.addContainer(id, "0701")
            capture(c1, ReturnPhotoPhase.INITIAL, 1)
            try { repo.prepareOrderEmail(id); fail("An incomplete order must not be emailed") }
            catch (_: IllegalArgumentException) { }
            val c2 = repo.addContainer(id, "0702")
            capture(c2, ReturnPhotoPhase.INITIAL, 2)
            capture(c1, ReturnPhotoPhase.CLEAN, 3)
            capture(c2, ReturnPhotoPhase.CLEAN, 4)
            val before = db.dao().job(id)!!
            val original = repo.prepareOrderEmail(id)
            val intent = if (original.action == Intent.ACTION_CHOOSER) original.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!! else original
            assertNotNull(intent.component)
            assertNotEquals("com.microsoft.teams", intent.component!!.packageName)
            assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
            assertEquals(4, intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!.size)
            assertEquals("FINISH", intent.getStringExtra(Intent.EXTRA_SUBJECT))
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)!!
            assertTrue(message.contains("Auftrag: FINISH"))
            assertTrue(message.contains("Container: 0701, 0702"))
            assertTrue(message.contains("Fotos: 4"))
            val after = db.dao().job(id)!!
            assertEquals(before.job.completedAt, after.job.completedAt)
            assertEquals(before.job.completionTriggeredAt, after.job.completionTriggeredAt)
            assertNull(after.job.emailSentAt)
            assertNull(db.dao().jobReminder(id)!!.confirmedAt)
        } finally { repo.deleteJob(id); db.close() }
    }

    @Suppress("DEPRECATION")
    @Test fun compatibilityPhotoShareKeepsSelectedOriginalWithoutMarkingEmailPrepared(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val db = Room.inMemoryDatabaseBuilder(app, TrackDatabase::class.java).build()
        val repo = TrackRepository(app, db)
        val id = repo.createJob(JobDraft(JobKind.RETURN, "SELECTED", orderValue = "20 ft", quantity = 2,
            quick = "Nothing", additionalWork = "Empty"), 123L)
        try {
            for (n in 1..2) {
                val container = repo.addContainer(id, "070$n")
                repo.saveReturnPhoto(id, container, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT,
                    File.createTempFile("selected", ".jpg", app.cacheDir).apply { writeBytes(byteArrayOf(n.toByte())) })
            }
            val before = db.dao().job(id)!!.job
            val photo = db.dao().job(id)!!.ordered.last().photos.single()
            val expired = File(app.cacheDir, "shared-photos/selection-expired/$id").apply { mkdirs() }
            File(expired, "0701_01.jpg").writeBytes(byteArrayOf(9))
            assertTrue(expired.setLastModified(System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L))
            val intent = repo.prepareSelectedPhotoShare(setOf(photo.id))
            assertFalse(expired.parentFile!!.exists())
            assertEquals("image/jpeg", intent.type)
            assertEquals(Intent.ACTION_SEND, intent.action)
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)!!
            assertTrue(message.contains("Auftrag: SELECTED"))
            assertTrue(message.contains("Container: 0702"))
            assertTrue(message.contains("Fotos: 1"))
            assertFalse(message.contains("0701"))
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
            assertEquals("0702_vorher_01_inhalt.jpg", uri.lastPathSegment)
            assertArrayEquals(byteArrayOf(2), app.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            val compatibility = repo.prepareSelectedPhotoShare(setOf(photo.id), teamsPackage = "com.microsoft.teams")
            assertNull(compatibility.`package`)
            assertNull(compatibility.component)
            assertEquals(message, compatibility.getStringExtra(Intent.EXTRA_TEXT))
            assertArrayEquals(byteArrayOf(2), app.contentResolver.openInputStream(compatibility.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!)!!.use { it.readBytes() })
            val after = db.dao().job(id)!!.job
            assertEquals(before.completedAt, after.completedAt)
            assertEquals(before.documentationRevision, after.documentationRevision)
            assertNull(after.emailPreparedRevision)
            assertNull(after.emailSentAt)
        } finally { repo.deleteJob(id); db.close() }
    }
}
