package ch.zerogen.containertrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
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

class TrackRepository(private val context: Context, val database: TrackDatabase,
    private val uploadScheduler: UploadScheduler = UploadScheduler {},
    private val accountId: () -> String? = { null }) {
    private val dao = database.dao()
    private val mutations = Mutex()
    private val alarms = ReminderAlarms(context)
    val jobs = dao.observeJobs()
    val reminders = dao.observeReminders()

    suspend fun createJob(draft: JobDraft, stoppedAt: Long?, initialNumber: String? = null): String = withContext(Dispatchers.IO) {
        require(initialNumber.isNullOrBlank() || containerError(initialNumber,emptySet())==null)
        require(draft.errors(stoppedAt).isEmpty()) { "Bitte alle Pflichtfelder prüfen." }
        val normalized = draft.copy(referenceNumber = draft.referenceNumber.trim(),
            kt = if (draft.kind == JobKind.RETURN) draft.referenceNumber.trim() else draft.kt.trim(),
            quick = if (draft.kind == JobKind.RETURN) draft.quick else "",
            additionalWork = if (draft.kind == JobKind.RETURN) draft.additionalWork else "")
        val entity = JobEntity(draft = normalized, createdAt = System.currentTimeMillis(), loggerStoppedAt = stoppedAt, createdByAccountId = accountId())
        database.withTransaction {
            dao.insertJob(entity)
            initialNumber?.takeIf { it.isNotBlank() }?.let { number ->
                dao.insertContainer(ContainerEntity(jobId=entity.id,sequenceIndex=1,containerNumber=number))
            }
        }
        entity.id
    }

    suspend fun addContainer(jobId: String, number: String): String = withContext(Dispatchers.IO) {
        mutations.withLock { database.withTransaction {
            val batch = requireNotNull(dao.job(jobId))
            check(batch.job.exportedAt == null) { "Exportierte Aufträge sind abgeschlossen." }
            require(batch.containers.size < batch.job.draft.quantity) { "Alle Container sind bereits erfasst." }
            require(canAddContainer(batch.job.draft.kind, batch.containers.map { it.photos.map { p -> p.step }.toSet() })) { "Zuerst die Leerdokumentation des aktuellen Containers abschliessen." }
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
        mutations.withLock {
            database.withTransaction {
                val batch = requireNotNull(dao.job(jobId))
                check(batch.job.exportedAt == null) { "Exportierte Aufträge sind abgeschlossen." }
                val item = batch.containers.first { it.container.id == containerId }
                require(containerError(number,batch.containers.filter { it.container.id != containerId }.map { it.container.containerNumber }.toSet()) == null)
                if (item.container.containerNumber != number) {
                    dao.updateContainer(item.container.copy(containerNumber = number))
                    // Names are part of the upload version; stale in-flight acknowledgements must not survive a rename.
                    for (photo in item.photos) dao.putPhoto(photo.copy(id = UUID.randomUUID().toString()))
                    if (batch.complete && dao.uploadJob(jobId) != null) queuePhotos(requireNotNull(dao.job(jobId)))
                    dao.updateJob(batch.job.copy(updatedAt = System.currentTimeMillis()))
                }
            }
            if (dao.uploadJob(jobId) != null) dispatch(CompletionEffects(uploadJobId = jobId))
        }
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
                var effects = CompletionEffects()
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
                    if (updated.complete) {
                        val first = batch.job.completionTriggeredAt == null
                        if (first) dao.updateJob(batch.job.copy(updatedAt = at, completedAt = at, completionTriggeredAt = at))
                        var reminder: LoggerReminder? = null
                        if (batch.job.draft.kind == JobKind.DELIVERY) {
                            dao.insertReminder(LoggerReminder(jobId = jobId, referenceNumber = batch.job.draft.referenceNumber,
                                dueAt = reminderDueAt(batch.job.completionTriggeredAt ?: at)))
                            reminder = dao.jobReminder(jobId)
                        }
                        if (first || dao.uploadJob(jobId) != null) queuePhotos(requireNotNull(dao.job(jobId)))
                        effects = CompletionEffects(reminder, if (dao.uploadJob(jobId) != null) jobId else null)
                    }
                  }
                  committed = true
                  replaced?.let { File(it.localPath).delete() }
                  captured.delete()
                  dispatch(effects)
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
                if (dao.uploadJob(jobId) != null) dao.uploadState(jobId,"COMPLETE_PENDING_UPLOAD","Pflichtfotos fehlen · Upload pausiert",System.currentTimeMillis())
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

    suspend fun preparePhotoShare(jobId: String, teamsPackage: String? = null, photoId: String? = null): Intent = withContext(Dispatchers.IO) {
        mutations.withLock {
            val batch = requireNotNull(dao.job(jobId))
            pruneSharedExports(System.currentTimeMillis())
            val directory = File(context.cacheDir,"shared-photos/$jobId/${UUID.randomUUID()}")
            check(directory.mkdirs()) { "Fotos konnten nicht vorbereitet werden." }
            try {
                val photos = batch.ordered.flatMap { item -> item.photos.sortedBy { it.step }.filter { photoId == null || it.id == photoId }.map { photo ->
                    require(photo.step in 1..batch.job.draft.kind.photoCount)
                    val source = File(photo.localPath)
                    require(source.isFile && source.length() > 0)
                    File(directory,photoName(item.container.containerNumber,photo.step)).also { source.copyTo(it) }
                } }
                val intent = photoShareIntent(context,photos,teamsPackage)
                intent.putExtra(Intent.EXTRA_SUBJECT,batch.job.draft.referenceNumber)
                intent
            } catch (e: Exception) { directory.deleteRecursively();throw e }
        }
    }

    suspend fun prepareSelectedPhotoShare(photoIds: Set<String>): Intent = withContext(Dispatchers.IO) {
        mutations.withLock {
            val selected = selectedPhotos(photoIds)
            pruneSharedExports(System.currentTimeMillis())
            val directory = File(context.cacheDir,"shared-photos/selection-${UUID.randomUUID()}")
            check(directory.mkdirs())
            try {
                val files = selected.map { (jobId, number, photo) ->
                    val folder=File(directory,jobId).apply { check(mkdirs() || isDirectory) }
                    File(folder,photoName(number,photo.step)).also { File(photo.localPath).copyTo(it) }
                }
                photoShareIntent(context,files)
            } catch(e:Exception) { directory.deleteRecursively();throw e }
        }
    }

    private suspend fun selectedPhotos(ids:Set<String>):List<Triple<String,String,PhotoEntity>> {
        require(ids.isNotEmpty())
        val photos=dao.observeJobs().first().flatMap { batch -> batch.ordered.flatMap { item ->
            item.photos.filter { it.id in ids }.map { Triple(batch.job.id,item.container.containerNumber,it) }
        } }
        require(photos.map { it.third.id }.toSet()==ids)
        require(photos.all { File(it.third.localPath).isFile && File(it.third.localPath).length()>0 })
        return photos
    }

    suspend fun saveSelectedPhotos(photoIds:Set<String>,tree:Uri):Int = withContext(Dispatchers.IO) {
        mutations.withLock {
            val photos=selectedPhotos(photoIds)
            val parent=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,android.provider.DocumentsContract.getTreeDocumentId(tree))
            val created=mutableListOf<Uri>()
            try {
                for ((job,group) in photos.groupBy { it.first }) {
                    val folder=android.provider.DocumentsContract.createDocument(context.contentResolver,parent,android.provider.DocumentsContract.Document.MIME_TYPE_DIR,job)
                        ?: error("Speicherordner konnte nicht erstellt werden")
                    created+=folder
                    for((_,number,photo) in group) {
                        val target=android.provider.DocumentsContract.createDocument(context.contentResolver,folder,"image/jpeg",photoName(number,photo.step))
                            ?: error("Fotodatei konnte nicht erstellt werden")
                        created+=target
                        val stream=context.contentResolver.openOutputStream(target,"w") ?: error("Speicherziel nicht verfügbar")
                        stream.use { output -> File(photo.localPath).inputStream().use { it.copyTo(output) };output.flush() }
                    }
                }
                photos.size
            } catch(e:Exception) {
                created.asReversed().forEach { runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver,it) } }
                throw e
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
            dao.jobReminder(job.id)
        }
        File(context.filesDir, "pending/$jobId").deleteRecursively()
        reminder
    }

    private suspend fun queuePhotos(batch: JobWithContainers) {
        dao.insertUploadJob(UploadJobEntity(batch.job.id, ownerAccountId = batch.job.createdByAccountId))
        for (item in batch.ordered) for (photo in item.photos) {
            dao.insertUploadPhoto(UploadPhotoEntity(photo.id, batch.job.id,
                "${batch.job.id}/${item.container.containerNumber}/${photoName(item.container.containerNumber, photo.step)}"))
        }
        if (dao.uploadPhotos(batch.job.id).any { it.uploadedAt == null })
            dao.uploadState(batch.job.id, "COMPLETE_PENDING_UPLOAD", null, System.currentTimeMillis())
    }

    private fun dispatch(effects: CompletionEffects) {
        // The durable DB records remain authoritative if the process or scheduler stops here.
        effects.reminder?.let { runCatching { alarms.schedule(it) } }
        effects.uploadJobId?.let { runCatching { uploadScheduler.enqueue(it) } }
    }

    suspend fun enqueuePhotoUpload(jobId: String, owner: String) = withContext(Dispatchers.IO) {
        mutations.withLock {
            database.withTransaction {
                val batch = requireNotNull(dao.job(jobId))
                check(batch.complete) { "Zuerst alle Pflichtfotos aufnehmen." }
                queuePhotos(batch)
                val existing = requireNotNull(dao.uploadJob(jobId))
                check(existing.ownerAccountId == null || existing.ownerAccountId == owner) { "Dieser Auftrag gehört zu einem anderen Konto." }
                dao.bindUpload(jobId, owner)
            }
            dispatch(CompletionEffects(uploadJobId = jobId))
        }
    }

    suspend fun reconcilePendingEffects() = withContext(Dispatchers.IO) {
        mutations.withLock {
            database.withTransaction {
                for (batch in dao.observeJobs().first()) {
                    if (batch.complete && batch.job.draft.kind == JobKind.DELIVERY &&
                        batch.job.completionTriggeredAt != null && dao.jobReminder(batch.job.id) == null) {
                        dao.insertReminder(LoggerReminder(jobId = batch.job.id, referenceNumber = batch.job.draft.referenceNumber,
                            dueAt = reminderDueAt(System.currentTimeMillis())))
                    }
                }
            }
            dao.pendingReminders().forEach { dispatch(CompletionEffects(reminder = it)) }
            dao.uploadJobs().filter { it.state in setOf("COMPLETE_PENDING_UPLOAD","UPLOADING") }.forEach { dispatch(CompletionEffects(uploadJobId = it.jobId)) }
        }
    }

    private fun pruneSharedExports(now: Long) {
        val cutoff = now - 7 * 24 * 60 * 60 * 1000L
        listOf("shared-exports","shared-photos").forEach { base -> File(context.cacheDir, base).listFiles()?.forEach { jobDirectory ->
            jobDirectory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.deleteRecursively() }
            if (jobDirectory.listFiles()?.isEmpty() == true) jobDirectory.delete()
        } }
    }

    suspend fun deleteJob(jobId: String): LoggerReminder? = withContext(Dispatchers.IO) {
        mutations.withLock {
            val reminder = dao.jobReminder(jobId)
            uploadScheduler.cancel(jobId)
            withContext(NonCancellable) {
                dao.deleteJob(jobId)
                reminder?.let { alarms.cancel(it.id) }
                File(context.filesDir, "photos/$jobId").deleteRecursively()
                File(context.filesDir, "pending/$jobId").deleteRecursively()
                File(context.cacheDir, "shared-exports/$jobId").deleteRecursively()
                File(context.cacheDir, "shared-photos/$jobId").deleteRecursively()
                File(context.cacheDir,"shared-photos").listFiles()?.filter { it.name.startsWith("selection-") }?.forEach { selection ->
                    File(selection,jobId).deleteRecursively()
                    if(selection.listFiles()?.isEmpty()==true) selection.delete()
                }
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
