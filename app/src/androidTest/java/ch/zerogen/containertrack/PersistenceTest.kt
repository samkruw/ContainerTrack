package ch.zerogen.containertrack

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun draft(kind: JobKind, count: Int = 1) = JobDraft(kind, "TEST-${UUID.randomUUID()}", "ZRH-123", "20 ft", count, "Nothing", "Empty")

    @Test fun cancellationAfterPhotoCommitKeepsReferencedFile() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(JobKind.RETURN), System.currentTimeMillis())
        val container = repo.addContainer(job, "0708")
        val captured = File.createTempFile("capture", ".jpg", context.cacheDir).apply { writeBytes(ByteArray(100_000) { 7 }) }
        try {
            val saving = launch(Dispatchers.IO) { repo.savePhoto(job, container, 1, captured) }
            withTimeout(10_000) { db.dao().observeJobs().first { rows -> rows.any { it.containers.any { c -> c.photos.isNotEmpty() } } } }
            saving.cancel(); saving.join()
            val saved = db.dao().job(job)!!.containers.single().photos.single()
            assertTrue(File(saved.localPath).isFile)
            assertEquals(100_000L, File(saved.localPath).length())
        } finally { captured.delete(); repo.deleteJob(job); db.close() }
    }

    @Test fun restartRestoresJobPhotosAndPendingReminder() = runBlocking {
        val name = "test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, TrackDatabase::class.java, name).build()
        var repo = TrackRepository(context, db)
        val job = repo.createJob(draft(JobKind.DELIVERY), null)
        val container = repo.addContainer(job, "0708")
        val photo = File.createTempFile("capture", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            repo.savePhoto(job, container, 1, photo)
            db.dao().insertReminder(LoggerReminder(jobId = job, referenceNumber = "TEST", dueAt = 2_000_000))
            db.close()
            db = Room.databaseBuilder(context, TrackDatabase::class.java, name).build()
            repo = TrackRepository(context, db)
            val loaded = db.dao().job(job)!!
            assertEquals("0708", loaded.containers.single().container.containerNumber)
            assertEquals(1, loaded.containers.single().photos.single().step)
            assertTrue(File(loaded.containers.single().photos.single().localPath).isFile)
            assertEquals(2_000_000L, db.dao().pendingReminders().single().dueAt)
            repo.deleteJob(job)
            assertNull(db.dao().job(job))
            assertTrue(db.dao().pendingReminders().isEmpty())
            assertFalse(File(context.filesDir, "photos/$job").exists())
        } finally { photo.delete(); db.close(); context.deleteDatabase(name); File(context.filesDir, "photos/$job").deleteRecursively() }
    }

    @Test fun deliveryCannotSkipAndDeletionResetsFollowingSteps() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(JobKind.DELIVERY, 2), null)
        val container = repo.addContainer(job, "0708")
        val invalid = File.createTempFile("capture", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(9)) }
        try {
            try { repo.savePhoto(job, container, 2, invalid); fail("Skipped photo step") } catch (_: IllegalArgumentException) { }
            for (step in 1..6) {
                val photo = File.createTempFile("capture", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(step.toByte())) }
                repo.savePhoto(job, container, step, photo)
            }
            assertNotNull(db.dao().job(job)!!.containers.single().container.completedAt)
            try { repo.addContainer(job, "0708"); fail("Duplicate accepted") } catch (_: IllegalArgumentException) { }
            repo.deletePhotos(job, container, 4)
            val loaded = db.dao().job(job)!!
            assertEquals(listOf(1, 2, 3), loaded.containers.single().photos.map { it.step }.sorted())
            assertNull(loaded.containers.single().container.completedAt)
            assertFalse(loaded.complete)
        } finally { invalid.delete(); repo.deleteJob(job); db.close() }
    }

    @Test fun firstCompletionCreatesOneTimerAndReexportPreservesIt() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(JobKind.DELIVERY), null)
        val container = repo.addContainer(job, "0708")
        val export1 = File(context.cacheDir, "test-${UUID.randomUUID()}.zip")
        val export2 = File(context.cacheDir, "test-${UUID.randomUUID()}.zip")
        try {
            for (step in 1..6) {
                val photo = File.createTempFile("capture", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(step.toByte())) }
                repo.savePhoto(job, container, step, photo)
            }
            val before = System.currentTimeMillis()
            val first = repo.export(job, Uri.fromFile(export1))!!
            val firstAt = db.dao().job(job)!!.job.exportedAt!!
            assertTrue(firstAt >= before)
            assertEquals(db.dao().job(job)!!.job.completionTriggeredAt!! + 1_800_000L, first.dueAt)
            val second = repo.export(job, Uri.fromFile(export2))!!
            assertEquals(first.id, second.id)
            assertEquals(first.dueAt, second.dueAt)
            assertEquals(firstAt, db.dao().job(job)!!.job.exportedAt)
            assertEquals(1, db.dao().pendingReminders().size)
            val names = mutableListOf<String>()
            var metadata: JSONObject? = null
            ZipInputStream(export1.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    names += entry.name
                    if (entry.name.endsWith("_META.json")) metadata = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                }
            }
            assertEquals(8, names.size)
            assertTrue(names.any { it.endsWith("/0708/0708_06.jpg") })
            assertEquals(1, metadata!!.getInt("quantity"))
            val archived = metadata!!.getJSONArray("containers").getJSONObject(0)
            assertEquals("0708", archived.getString("containerNumber"))
            assertEquals(6, archived.getJSONArray("photos").length())
            assertEquals("0708_06.jpg", archived.getJSONArray("photos").getJSONObject(5).getString("fileName"))
        } finally { repo.deleteJob(job); db.close(); export1.delete(); export2.delete() }
    }

    @Test fun returnRequiresLoggerStopAndHasExactlyOnePhoto() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        var job: String? = null
        try {
            val form = draft(JobKind.RETURN)
            try { repo.createJob(form, null); fail("Logger gate skipped") } catch (_: IllegalArgumentException) { }
            val stopped = System.currentTimeMillis()
            val id = repo.createJob(form, stopped); job = id
            val container = repo.addContainer(id, "0001")
            val photo = File.createTempFile("capture", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(1)) }
            repo.savePhoto(id, container, 1, photo)
            val result = db.dao().job(id)!!
            assertTrue(result.complete)
            assertEquals(stopped, result.job.loggerStoppedAt)
            assertEquals(form.referenceNumber, result.job.draft.kt)
            assertTrue(db.dao().pendingReminders().isEmpty())
        } finally { job?.let { repo.deleteJob(it) }; db.close() }
    }
}
