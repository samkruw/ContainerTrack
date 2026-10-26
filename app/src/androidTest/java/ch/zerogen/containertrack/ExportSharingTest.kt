package ch.zerogen.containertrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class ExportSharingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun draft(kind: JobKind) = JobDraft(kind, "SHARE-${UUID.randomUUID()}", "ZRH-123", "20 ft", 1, "Nothing", "Empty")

    private suspend fun complete(repo: TrackRepository, job: String, kind: JobKind) {
        val container = repo.addContainer(job, "0708")
        for (step in 1..kind.photoCount) {
            val photo = File.createTempFile("share-photo", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(step.toByte())) }
            repo.savePhoto(job, container, step, photo)
        }
    }

    @Suppress("DEPRECATION")
    private fun uri(intent: Intent): Uri = requireNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM))

    private fun entries(uri: Uri): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(requireNotNull(context.contentResolver.openInputStream(uri))).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; result[entry.name] = zip.readBytes() }
        }
        return result
    }

    @Test fun deliverySharesReadableZipAndPreservesTimerAcrossShareAndSave() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val form = draft(JobKind.DELIVERY).copy(quick = "", additionalWork = "")
        val job = repo.createJob(form, null)
        val saved = File(context.cacheDir, "share-save-${UUID.randomUUID()}.zip")
        try {
            complete(repo, job, form.kind)
            // Also cover delivery records created with the older app version.
            val old = db.dao().job(job)!!.job
            db.dao().updateJob(old.copy(draft = old.draft.copy(kt = "LEGACY-KT", quick = "LEGACY-QUICK", additionalWork = "LEGACY-WORK")))
            val intent = repo.prepareShare(job)
            val attachment = uri(intent)
            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("application/zip", intent.type)
            assertEquals("application/zip", context.contentResolver.getType(attachment))
            assertEquals("content", attachment.scheme)
            assertEquals("${context.packageName}.exports", attachment.authority)
            assertEquals(form.referenceNumber, intent.getStringExtra(Intent.EXTRA_SUBJECT))
            assertEquals(attachment, intent.clipData!!.getItemAt(0).uri)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.contentResolver.query(attachment, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use {
                assertTrue(it.moveToFirst()); assertEquals("${form.referenceNumber}.zip", it.getString(0))
            }
            val firstAt = db.dao().job(job)!!.job.exportedAt!!
            val firstReminder = db.dao().pendingReminders().single()
            assertEquals(db.dao().job(job)!!.job.completionTriggeredAt!! + 1_800_000L, firstReminder.dueAt)
            val archive = entries(attachment)
            assertEquals(8, archive.size)
            assertArrayEquals(byteArrayOf(6), archive.getValue("${form.referenceNumber}/0708/0708_06.jpg"))
            val meta = JSONObject(archive.getValue("${form.referenceNumber}/${form.referenceNumber}_META.json").toString(Charsets.UTF_8))
            assertFalse(meta.has("kt"))
            assertEquals(2,meta.getInt("schemaVersion"))
            assertEquals("", meta.getString("quick")); assertEquals("", meta.getString("additionalWork"))
            val row = archive.getValue("${form.referenceNumber}/${form.referenceNumber}_EXCEL.txt").toString(Charsets.UTF_8).split("\r\n")[1]
            assertTrue(row.endsWith("\t1\t\t0708\t"))
            assertFalse(row.contains("LEGACY-KT"))
            assertTrue(row.contains(form.referenceNumber))
            val second = repo.prepareShare(job)
            assertNotEquals(attachment, uri(second))
            assertEquals(firstAt, db.dao().job(job)!!.job.exportedAt)
            assertEquals(firstReminder, db.dao().pendingReminders().single())
            assertEquals(archive.keys, entries(attachment).keys) // Earlier mail attachment still readable.
            repo.export(job, Uri.fromFile(saved))
            assertEquals(firstAt, db.dao().job(job)!!.job.exportedAt)
            assertEquals(firstReminder, db.dao().pendingReminders().single())
        } finally { repo.deleteJob(job); db.close(); saved.delete() }
        assertFalse(File(context.cacheDir, "shared-exports/$job").exists())
    }

    @Test fun returnShareContainsExtraFieldsAndCreatesNoLoggerReminder() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val form = draft(JobKind.RETURN)
        val job = repo.createJob(form, 123L)
        try {
            complete(repo, job, form.kind)
            val archive = entries(uri(repo.prepareShare(job)))
            assertEquals(3, archive.size)
            val meta = JSONObject(archive.getValue("${form.referenceNumber}/${form.referenceNumber}_META.json").toString(Charsets.UTF_8))
            assertFalse(meta.has("kt"))
            assertEquals(2,meta.getInt("schemaVersion"))
            assertEquals("Nothing", meta.getString("quick")); assertEquals("Empty", meta.getString("additionalWork"))
            assertTrue(archive.getValue("${form.referenceNumber}/${form.referenceNumber}_EXCEL.txt").toString(Charsets.UTF_8).endsWith("\tEmpty\t0708\tNothing\r\n"))
            assertNotNull(db.dao().job(job)!!.job.exportedAt)
            assertTrue(db.dao().pendingReminders().isEmpty())
        } finally { repo.deleteJob(job); db.close() }
    }

    @Test fun incompleteShareDoesNotMarkExportOrStartTimer() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context, db)
        val job = repo.createJob(draft(JobKind.DELIVERY), null)
        try {
            try { repo.prepareShare(job); fail("Incomplete job shared") } catch (_: IllegalStateException) { }
            assertNull(db.dao().job(job)!!.job.exportedAt)
            assertTrue(db.dao().pendingReminders().isEmpty())
            assertFalse(File(context.cacheDir, "shared-exports/$job").exists())
        } finally { repo.deleteJob(job); db.close() }
    }

    @Test fun providerRejectsInternalFilesOutsideExportCache() {
        val photo = File.createTempFile("private-photo", ".jpg", context.filesDir)
        try {
            try { zipShareIntent(context, photo); fail("Private file exposed") } catch (_: IllegalArgumentException) { }
            val provider = context.packageManager.resolveContentProvider("${context.packageName}.exports", 0)!!
            assertFalse(provider.exported)
            assertTrue(provider.grantUriPermissions)
        } finally { photo.delete() }
    }
}
