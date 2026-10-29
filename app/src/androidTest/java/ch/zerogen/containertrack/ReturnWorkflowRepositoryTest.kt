package ch.zerogen.containertrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReturnWorkflowRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun draft(quantity: Int = 1) = JobDraft(JobKind.RETURN, "RET-${UUID.randomUUID()}",
        orderValue = "20 ft", quantity = quantity, quick = "Nothing", additionalWork = "Empty")

    private suspend fun photo(repo: TrackRepository, job: String, container: String,
        phase: ReturnPhotoPhase, category: ReturnPhotoCategory, value: Int = 1, replaceId: String? = null) {
        val captured = File.createTempFile("return-workflow", ".jpg", context.cacheDir)
            .apply { writeBytes(byteArrayOf(value.toByte(), 42)) }
        try { repo.saveReturnPhoto(job, container, phase, category, captured, replaceId) }
        finally { captured.delete() }
    }

    private suspend fun rejected(block: suspend () -> Unit) {
        try { block(); fail("The invalid workflow transition was accepted") }
        catch (_: IllegalArgumentException) { }
        catch (_: IllegalStateException) { }
    }

    @Test fun threeInitialPhotosNeedThreeCleanPhotosAndEveryContainerNeedsContentFirst(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(2), 123L)
        try {
            assertEquals(1, db.dao().job(job)!!.job.returnWorkflowVersion)
            val first = repo.addContainer(job, "0708")
            photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT)
            photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.DAMAGE)
            photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.OTHER)
            assertFalse(db.dao().job(job)!!.complete)
            rejected { photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT) }
            rejected { photo(repo, job, first, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN) }
            val second = repo.addContainer(job, "0710")
            rejected { photo(repo, job, first, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN) }
            assertTrue(db.dao().job(job)!!.containers.all { item -> item.photos.none { it.returnPhase == ReturnPhotoPhase.CLEAN } })
            photo(repo, job, second, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT)
            assertTrue(db.dao().job(job)!!.initialDocumentationComplete)
            photo(repo, job, second, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
            for (count in 1..2) {
                photo(repo, job, first, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
                val batch = db.dao().job(job)!!
                assertFalse(batch.complete)
                assertEquals(count, returnProgress(batch.containers.first { it.container.id == first }.photos).cleanCount)
            }
            photo(repo, job, first, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
            val complete = db.dao().job(job)!!
            assertTrue(complete.complete)
            assertEquals(WorkflowPhase.RETURN_AWAITING_EMAIL, complete.phase)
            assertEquals(ReturnProgress(3, 3, true, true), returnProgress(complete.containers.first { it.container.id == first }.photos))
            rejected { photo(repo, job, first, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN) }
            assertNull(db.dao().uploadJob(job))
            assertTrue(db.dao().uploadPhotos(job).isEmpty())
        } finally { repo.deleteJob(job); db.close() }
    }

    @Test fun returnReminderStartsAtCreationAndOnlyExplicitEmailConfirmationClosesIt(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(), 123L)
        val delivery = repo.createJob(JobDraft(JobKind.DELIVERY, "DEL-${UUID.randomUUID()}", orderValue = "20 ft"), null)
        try {
            val created = db.dao().job(job)!!.job.createdAt
            val reminder = db.dao().jobReminder(job)!!
            assertEquals(ReminderKind.RETURN_CLOSE, reminder.kind)
            assertEquals(created + 24 * 60 * 60 * 1000L, reminder.dueAt)
            assertNull(reminder.confirmedAt)
            val loggerId = db.dao().insertReminder(LoggerReminder(jobId = delivery, referenceNumber = "DEL", dueAt = created + 1_800_000L)).toInt()
            rejected { repo.confirmReminder(reminder.id) }
            rejected { repo.confirmEmailSent(job) }
            rejected { repo.prepareOrderEmail(job) }
            assertNull(db.dao().job(job)!!.job.emailPreparedRevision)
            val container = repo.addContainer(job, "0708")
            photo(repo, job, container, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT)
            photo(repo, job, container, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
            assertTrue(db.dao().job(job)!!.complete)
            assertEquals(reminder, db.dao().jobReminder(job))
            rejected { repo.confirmEmailSent(job) }
            rejected { repo.confirmReminder(reminder.id) }
            repo.prepareOrderEmail(job)
            val prepared = db.dao().job(job)!!.job
            assertEquals(prepared.documentationRevision, prepared.emailPreparedRevision)
            assertNotNull(prepared.emailPreparedAt)
            assertNull(prepared.emailSentAt)
            assertEquals(reminder, db.dao().jobReminder(job))
            repo.reconcilePendingEffects()
            assertEquals(reminder.dueAt, db.dao().jobReminder(job)!!.dueAt)
            repo.confirmEmailSent(job)
            val sent = db.dao().job(job)!!.job.emailSentAt
            assertNotNull(sent)
            assertEquals(WorkflowPhase.COMPLETE, db.dao().job(job)!!.phase)
            assertNotNull(db.dao().jobReminder(job)!!.confirmedAt)
            assertNull(db.dao().reminder(loggerId)!!.confirmedAt)
            repo.confirmEmailSent(job)
            assertEquals(sent, db.dao().job(job)!!.job.emailSentAt)
            assertTrue(db.dao().pendingReminders().none { it.jobId == job })
            assertTrue(db.dao().pendingReminders().any { it.id == loggerId })
        } finally { repo.deleteJob(job); repo.deleteJob(delivery); db.close() }
    }

    @Test fun editsInvalidatePreparedEmailAndSentDocumentationIsReadOnly(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(), 123L)
        try {
            val container = repo.addContainer(job, "0708")
            photo(repo, job, container, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT)
            photo(repo, job, container, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
            repo.prepareOrderEmail(job)
            val firstRevision = db.dao().job(job)!!.job.documentationRevision
            val clean = db.dao().job(job)!!.containers.single().photos.single { it.returnPhase == ReturnPhotoPhase.CLEAN }
            photo(repo, job, container, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN, 2, clean.id)
            assertTrue(db.dao().job(job)!!.job.documentationRevision > firstRevision)
            assertNull(db.dao().job(job)!!.job.emailPreparedRevision)
            assertNull(db.dao().job(job)!!.job.emailPreparedAt)
            rejected { repo.confirmEmailSent(job) }
            repo.prepareOrderEmail(job)
            val beforeRename = db.dao().job(job)!!.job.documentationRevision
            repo.renameContainer(job, container, "0710")
            assertTrue(db.dao().job(job)!!.job.documentationRevision > beforeRename)
            assertNull(db.dao().job(job)!!.job.emailPreparedRevision)
            rejected { repo.confirmEmailSent(job) }
            repo.prepareOrderEmail(job)
            val replacement = db.dao().job(job)!!.containers.single().photos.single { it.returnPhase == ReturnPhotoPhase.CLEAN }
            repo.deleteReturnPhoto(job, container, replacement.id)
            assertFalse(db.dao().job(job)!!.complete)
            assertNull(db.dao().job(job)!!.job.emailPreparedRevision)
            rejected { repo.confirmEmailSent(job) }
            rejected { repo.prepareOrderEmail(job) }
            photo(repo, job, container, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
            repo.prepareOrderEmail(job)
            repo.confirmEmailSent(job)
            val sent = db.dao().job(job)!!
            val saved = sent.containers.single().photos.first()
            rejected { repo.renameContainer(job, container, "0712") }
            rejected { repo.deleteReturnPhoto(job, container, saved.id) }
            rejected { photo(repo, job, container, saved.returnPhase, saved.returnCategory, replaceId = saved.id) }
            assertEquals(sent, db.dao().job(job))
        } finally { repo.deleteJob(job); db.close() }
    }

    @Suppress("DEPRECATION")
    private fun emailTargets(intent: Intent): List<Intent> = if (intent.action == Intent.ACTION_CHOOSER) {
        listOf(requireNotNull(intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))) +
            intent.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS).orEmpty().map { it as Intent }
    } else listOf(intent)

    @Suppress("DEPRECATION")
    private fun streams(intent: Intent): List<Uri> = if (intent.action == Intent.ACTION_SEND) {
        listOf(requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)))
    } else requireNotNull(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))

    @Test fun emailContainsEveryNamedOriginalFromEveryContainerWithExactReferenceSubject(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val form = draft(2)
        val job = repo.createJob(form, 123L)
        try {
            val first = repo.addContainer(job, "0708")
            photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT, 1)
            photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.DAMAGE, 2)
            photo(repo, job, first, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.OTHER, 3)
            val second = repo.addContainer(job, "0710")
            photo(repo, job, second, ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT, 4)
            for (value in 5..7) photo(repo, job, first, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN, value)
            photo(repo, job, second, ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN, 8)
            val batch = db.dao().job(job)!!
            val expected = listOf(
                "0708_vorher_01_inhalt.jpg", "0708_vorher_02_schaden.jpg", "0708_vorher_03_zusatz.jpg",
                "0708_nachher_01_leer_sauber.jpg", "0708_nachher_02_leer_sauber.jpg", "0708_nachher_03_leer_sauber.jpg",
                "0710_vorher_01_inhalt.jpg", "0710_nachher_01_leer_sauber.jpg")
            val expectedBytes = listOf(1, 2, 3, 5, 6, 7, 4, 8)
            val intent = repo.prepareOrderEmail(job)
            val targets = emailTargets(intent)
            assertTrue(targets.isNotEmpty())
            targets.forEach { target ->
                assertNotNull(target.component)
                assertNotEquals("com.microsoft.teams", target.component!!.packageName)
                assertEquals(Intent.ACTION_SEND_MULTIPLE, target.action)
                assertEquals("image/jpeg", target.type)
                assertEquals(form.referenceNumber, target.getStringExtra(Intent.EXTRA_SUBJECT))
                assertFalse(target.hasExtra(Intent.EXTRA_EMAIL))
                assertTrue(target.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                assertEquals(0, target.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val uris = streams(target)
                assertEquals(8, uris.size)
                assertEquals(8, target.clipData!!.itemCount)
                uris.forEachIndexed { index, uri ->
                    assertEquals(uri, target.clipData!!.getItemAt(index).uri)
                    assertEquals("content", uri.scheme)
                    assertEquals("${context.packageName}.exports", uri.authority)
                    assertEquals("image/jpeg", context.contentResolver.getType(uri))
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
                        assertTrue(cursor.moveToFirst()); assertEquals(expected[index], cursor.getString(0))
                    }
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        assertArrayEquals(byteArrayOf(expectedBytes[index].toByte(), 42), input.readBytes())
                    }
                }
            }
            val launches = mutableListOf<Intent>()
            launchShareIntent(intent) { launches += it }
            assertEquals(listOf(intent), launches)
            val after = db.dao().job(job)!!
            assertEquals(batch.job.documentationRevision, after.job.documentationRevision)
            assertEquals(after.job.documentationRevision, after.job.emailPreparedRevision)
            assertNull(after.job.emailSentAt)
            assertNull(db.dao().jobReminder(job)!!.confirmedAt)
        } finally { repo.deleteJob(job); db.close() }
    }

    @Test fun legacySinglePhotoEntryPointCannotBypassManagedReturnDocumentation(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(), 123L)
        val captured = File.createTempFile("return-legacy-bypass", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(1)) }
        try {
            val container = repo.addContainer(job, "0708")
            rejected { repo.savePhoto(job, container, 1, captured) }
            assertTrue(db.dao().job(job)!!.containers.single().photos.isEmpty())
            assertFalse(db.dao().job(job)!!.complete)
        } finally { captured.delete(); repo.deleteJob(job); db.close() }
    }
}
