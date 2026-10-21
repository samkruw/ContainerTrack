package ch.zerogen.containertrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class TrackRepository(private val context: Context, val database: TrackDatabase) {
    private val dao = database.dao()
    private val mutations = Mutex()
    private val alarms = ReminderAlarms(context)
    val jobs = dao.observeJobs()
    val reminders = dao.observeReminders()

    suspend fun createJob(draft: JobDraft, stoppedAt: Long?): String = withContext(Dispatchers.IO) {
        require(draft.errors(stoppedAt).isEmpty()) { "Bitte alle Pflichtfelder prüfen." }
        val normalized = draft.copy(referenceNumber = draft.referenceNumber.trim(),
            kt = if (draft.kind == JobKind.RETURN) draft.referenceNumber.trim() else draft.kt.trim(),
            quick = if (draft.kind == JobKind.RETURN) draft.quick else "",
            additionalWork = if (draft.kind == JobKind.RETURN) draft.additionalWork else "")
        val entity = JobEntity(draft = normalized, createdAt = System.currentTimeMillis(), loggerStoppedAt = stoppedAt)
        dao.insertJob(entity)
        entity.id
    }

    suspend fun addContainer(jobId: String, number: String): String = withContext(Dispatchers.IO) {
        mutations.withLock { database.withTransaction {
            val batch = requireNotNull(dao.job(jobId))
            check(batch.job.exportedAt == null) { "Exportierte Aufträge sind abgeschlossen." }
            require(batch.containers.size < batch.job.draft.quantity) { "Alle Container sind bereits erfasst." }
            require(batch.containers.all { it.container.completedAt != null }) { "Zuerst den aktuellen Container abschliessen." }
            require(containerError(number, batch.containers.map { it.container.containerNumber }.toSet()) == null) {
                containerError(number, batch.containers.map { it.container.containerNumber }.toSet()) ?: "Ungültige Nummer."
            }
            val container = ContainerEntity(jobId = jobId, sequenceIndex = batch.containers.size + 1, containerNumber = number)
            dao.insertContainer(container)
            dao.updateJob(batch.job.copy(updatedAt = System.currentTimeMillis()))
            container.id
        } }
    }

    suspend fun renameContainer(jobId: String, containerId: String, number: String) = withContext(Dispatchers.IO) {
        mutations.withLock { database.withTransaction {
            val batch = requireNotNull(dao.job(jobId))
            check(batch.job.exportedAt == null) { "Exportierte Aufträge sind abgeschlossen." }
            val container = batch.containers.first { it.container.id == containerId }.container
            require(containerError(number, batch.containers.filter { it.container.id != containerId }.map { it.container.containerNumber }.toSet()) == null) {
                "Exakt 4 Ziffern verwenden; Duplikate sind nicht erlaubt."
            }
            dao.updateContainer(container.copy(containerNumber = number))
            dao.updateJob(batch.job.copy(updatedAt = System.currentTimeMillis()))
        } }
    }

    suspend fun savePhoto(jobId: String, containerId: String, step: Int, captured: File) = withContext(Dispatchers.IO) {
        mutations.withLock {
            require(captured.isFile && captured.length() > 0) { "Aufnahme fehlt. Bitte erneut aufnehmen." }
            val directory = File(context.filesDir, "photos/$jobId/$containerId").apply { mkdirs() }
            val destination = File(directory, "${UUID.randomUUID()}.jpg")
            val staged = File(directory, "${destination.name}.tmp")
            var committed = false
            try {
                captured.inputStream().use { input -> staged.outputStream().use { output -> input.copyTo(output); output.fd.sync() } }
                check(staged.renameTo(destination)) { "Foto konnte nicht gespeichert werden." }
                var replaced: PhotoEntity? = null
                withContext(NonCancellable) {
                  database.withTransaction {
                    val batch = requireNotNull(dao.job(jobId))
                    check(batch.job.exportedAt == null) { "Exportierte Aufträge sind abgeschlossen." }
                    val item = batch.containers.first { it.container.id == containerId }
                    require(step in 1..batch.job.draft.kind.photoCount)
                    require((1 until step).all { previous -> item.photos.any { it.step == previous } }) { "Vorherige Fotos fehlen." }
                    replaced = item.photos.find { it.step == step }
                    val at = System.currentTimeMillis()
                    dao.putPhoto(PhotoEntity(containerId = containerId, step = step, localPath = destination.absolutePath, capturedAt = at))
                    val complete = (item.photos.map { it.step }.toSet() + step).size == batch.job.draft.kind.photoCount
                    dao.updateContainer(item.container.copy(completedAt = if (complete) at else null))
                    val updated = requireNotNull(dao.job(jobId))
                    dao.updateJob(batch.job.copy(updatedAt = at, completedAt = if (updated.complete) at else null))
                  }
                  committed = true
                  replaced?.let { File(it.localPath).delete() }
                  captured.delete()
                }
            } catch (e: Exception) {
                staged.delete()
                if (!committed) destination.delete()
                throw e
            }
        }
    }

    suspend fun deletePhotos(jobId: String, containerId: String, fromStep: Int) = withContext(Dispatchers.IO) {
        mutations.withLock {
            val deleted = database.withTransaction {
                val batch = requireNotNull(dao.job(jobId))
                check(batch.job.exportedAt == null) { "Exportierte Aufträge sind abgeschlossen." }
                val item = batch.containers.first { it.container.id == containerId }
                val paths = item.photos.filter { it.step >= fromStep }.map { it.localPath }
                dao.deletePhotosFrom(containerId, fromStep)
                dao.updateContainer(item.container.copy(completedAt = null))
                dao.updateJob(batch.job.copy(updatedAt = System.currentTimeMillis(), completedAt = null))
                paths
            }
            deleted.forEach { File(it).delete() }
        }
    }

    suspend fun export(jobId: String, uri: Uri): LoggerReminder? = withContext(Dispatchers.IO) {
        mutations.withLock {
            val batch = requireNotNull(dao.job(jobId))
            check(batch.complete) { "Alle Container und Pflichtfotos müssen vollständig sein." }
            val preparedAt = System.currentTimeMillis()
            val zipFile = File(context.cacheDir, "${UUID.randomUUID()}.zip")
            try {
                writeArchive(batch, zipFile, preparedAt)
                val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Exportziel ist nicht verfügbar.")
                output.use { target -> zipFile.inputStream().use { it.copyTo(target) }; target.flush() }
                finishExport(jobId)
            } finally { zipFile.delete() }
        }
    }

    /** The chooser may be cancelled; a successfully prepared ZIP still counts as an export. */
    suspend fun prepareShare(jobId: String): Intent = withContext(Dispatchers.IO) {
        mutations.withLock {
            val batch = requireNotNull(dao.job(jobId))
            check(batch.complete) { "Alle Container und Pflichtfotos müssen vollständig sein." }
            pruneSharedExports(System.currentTimeMillis())
            val directory = File(context.cacheDir, "shared-exports/$jobId/${UUID.randomUUID()}")
            check(directory.mkdirs()) { "ZIP konnte nicht vorbereitet werden." }
            val zipFile = File(directory, "${batch.job.draft.referenceNumber}.zip")
            var committed = false
            try {
                writeArchive(batch, zipFile, System.currentTimeMillis())
                val intent = zipShareIntent(context, zipFile)
                withContext(NonCancellable) {
                    finishExport(jobId)
                    committed = true
                }
                // Keep the attachment after chooser dismissal: mail clients read it asynchronously.
                intent
            } finally {
                if (!committed) directory.deleteRecursively()
            }
        }
    }

    private fun writeArchive(batch: JobWithContainers, file: File, preparedAt: Long) {
        val containers = batch.ordered.map { item -> ExportContainer(item.container.containerNumber,
            item.photos.sortedBy { it.step }.map { ExportPhoto(it.step, File(it.localPath)) }) }
        file.outputStream().use { output ->
            writeExportZip(output, batch.job.draft, containers, metadata(batch, preparedAt),
                Instant.ofEpochMilli(preparedAt).atZone(ZoneId.systemDefault()).toLocalDate())
        }
    }

    private suspend fun finishExport(jobId: String): LoggerReminder? = withContext(NonCancellable) {
        val now = System.currentTimeMillis()
        val reminder = database.withTransaction {
            val job = requireNotNull(dao.job(jobId)).job
            dao.updateJob(job.copy(exportedAt = firstExportAt(job.exportedAt, now), updatedAt = now))
            if (job.draft.kind == JobKind.DELIVERY) {
                dao.insertReminder(LoggerReminder(jobId = job.id, referenceNumber = job.draft.referenceNumber,
                    dueAt = reminderDueAt(firstExportAt(job.exportedAt, now))))
                dao.jobReminder(job.id)
            } else null
        }
        reminder?.let { alarms.schedule(it) }
        File(context.filesDir, "pending/$jobId").deleteRecursively()
        reminder
    }

    private fun pruneSharedExports(now: Long) {
        val cutoff = now - 7 * 24 * 60 * 60 * 1000L
        File(context.cacheDir, "shared-exports").listFiles()?.forEach { jobDirectory ->
            jobDirectory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.deleteRecursively() }
            if (jobDirectory.listFiles()?.isEmpty() == true) jobDirectory.delete()
        }
    }

    suspend fun deleteJob(jobId: String): LoggerReminder? = withContext(Dispatchers.IO) {
        mutations.withLock {
            val reminder = dao.jobReminder(jobId)
            withContext(NonCancellable) {
                dao.deleteJob(jobId)
                reminder?.let { alarms.cancel(it.id) }
                File(context.filesDir, "photos/$jobId").deleteRecursively()
                File(context.filesDir, "pending/$jobId").deleteRecursively()
                File(context.cacheDir, "shared-exports/$jobId").deleteRecursively()
            }
            reminder
        }
    }

    suspend fun confirmReminder(id: Int) = withContext(Dispatchers.IO + NonCancellable) {
        dao.confirmReminder(id, System.currentTimeMillis())
        alarms.cancel(id)
    }

    private fun metadata(batch: JobWithContainers, preparedAt: Long): String {
        val job = batch.job
        val containers = JSONArray()
        for (item in batch.ordered) {
            val photos = JSONArray()
            for (photo in item.photos.sortedBy { it.step }) {
                photos.put(JSONObject()
                    .put("step", photo.step)
                    .put("fileName", photoName(item.container.containerNumber, photo.step))
                    .put("capturedAt", photo.capturedAt))
            }
            containers.put(JSONObject()
                .put("containerNumber", item.container.containerNumber)
                .put("sequenceIndex", item.container.sequenceIndex)
                .put("completedAt", item.container.completedAt ?: JSONObject.NULL)
                .put("photos", photos))
        }
        return JSONObject().apply {
        put("schemaVersion", 1); put("id", job.id); put("type", job.draft.kind.name)
        put("referenceNumber", job.draft.referenceNumber); put("kt", job.draft.kt)
        put("orderValue", job.draft.orderValue); put("quantity", job.draft.quantity)
        put("quick", if (job.draft.kind == JobKind.RETURN) job.draft.quick else "")
        put("additionalWork", if (job.draft.kind == JobKind.RETURN) job.draft.additionalWork else "")
        put("createdAt", job.createdAt); put("updatedAt", job.updatedAt)
        put("completedAt", job.completedAt ?: JSONObject.NULL)
        put("exportedAt", job.exportedAt ?: JSONObject.NULL); put("archiveCreatedAt", preparedAt)
        put("loggerStoppedAt", job.loggerStoppedAt ?: JSONObject.NULL)
        put("containers", containers)
        }.toString(2)
    }
}
