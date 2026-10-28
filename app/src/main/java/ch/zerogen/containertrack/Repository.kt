package ch.zerogen.containertrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
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
    private val automaticUploads: Boolean = true,
    private val accountId: () -> String? = { null }) {
    private val dao = database.dao()
    private val mutations = Mutex()
    private val alarms = ReminderAlarms(context)
    val jobs = dao.observeJobs()
    val reminders = dao.observeReminders()
    private suspend fun checkedJob(id:String):JobWithContainers {
        val batch=requireNotNull(dao.job(id))
        return batch
    }

    suspend fun createJob(draft: JobDraft, stoppedAt: Long?, initialNumber: String? = null): String = withContext(Dispatchers.IO) {
        require(initialNumber.isNullOrBlank() || containerError(initialNumber,emptySet())==null)
        require(draft.errors(stoppedAt).isEmpty()) { "Bitte alle Pflichtfelder prüfen." }
        val normalized = draft.copy(referenceNumber = draft.referenceNumber.trim(),
            kt = "",
            quick = if (draft.kind == JobKind.RETURN) draft.quick else "",
            additionalWork = if (draft.kind == JobKind.RETURN) draft.additionalWork else "")
        val entity = JobEntity(draft = normalized, createdAt = System.currentTimeMillis(), loggerStoppedAt = stoppedAt, createdByAccountId = accountId(),
            returnWorkflowVersion = if(normalized.kind==JobKind.RETURN)1 else 0)
        database.withTransaction {
            dao.insertJob(entity)
            if(entity.returnWorkflowVersion==1)dao.insertReminder(LoggerReminder(jobId=entity.id,referenceNumber=normalized.referenceNumber,
                dueAt=returnReminderDueAt(entity.createdAt),kind=ReminderKind.RETURN_CLOSE))
            initialNumber?.takeIf { it.isNotBlank() }?.let { number ->
                dao.insertContainer(ContainerEntity(jobId=entity.id,sequenceIndex=1,containerNumber=number))
            }
        }
        dao.jobReminder(entity.id)?.let{dispatch(CompletionEffects(reminder=it))}
        entity.id
    }

    suspend fun addContainer(jobId: String, number: String): String = withContext(Dispatchers.IO) {
        mutations.withLock { database.withTransaction {
            val batch = checkedJob(jobId)
            checkEditable(batch)
            require(batch.containers.size < batch.job.draft.quantity) { "Alle Container sind bereits erfasst." }
            require(if(batch.isManagedReturn)batch.containers.all{returnProgress(it.photos).hasContent} else canAddContainer(batch.job.draft.kind, batch.containers.map { it.photos.map { p -> p.step }.toSet() })) { "Zuerst die Leerdokumentation des aktuellen Containers abschliessen." }
            require(containerError(number, batch.containers.map { it.container.containerNumber }.toSet()) == null) {
                containerError(number, batch.containers.map { it.container.containerNumber }.toSet()) ?: "Ungültige Nummer."
            }
            val container = ContainerEntity(jobId = jobId, sequenceIndex = batch.containers.size + 1, containerNumber = number)
            dao.insertContainer(container)
            dao.updateJob(edited(batch.job,System.currentTimeMillis()))
            container.id
        } }
    }

    suspend fun renameContainer(jobId: String, containerId: String, number: String) = withContext(Dispatchers.IO) {
        mutations.withLock {
            database.withTransaction {
                val batch = checkedJob(jobId)
                checkEditable(batch)
                val item = batch.containers.first { it.container.id == containerId }
                require(containerError(number,batch.containers.filter { it.container.id != containerId }.map { it.container.containerNumber }.toSet()) == null)
                if (item.container.containerNumber != number) {
                    dao.updateContainer(item.container.copy(containerNumber = number))
                    // Names are part of the upload version; stale in-flight acknowledgements must not survive a rename.
                    for (photo in item.photos) dao.putPhoto(photo.copy(id = UUID.randomUUID().toString()))
                    if (batch.complete && dao.uploadJob(jobId) != null) queuePhotos(checkedJob(jobId))
                    dao.updateJob(edited(batch.job,System.currentTimeMillis()))
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
                    val batch = checkedJob(jobId)
                    checkEditable(batch)
                    val item = batch.containers.first { it.container.id == containerId }
                    require(!batch.isManagedReturn) { "Bitte den Return-Fotoablauf verwenden." }
                    require(step in 1..batch.job.draft.kind.photoCount)
                    require((1 until step).all { previous -> item.photos.any { it.step == previous } }) { "Vorherige Fotos fehlen." }
                    replaced = item.photos.find { it.step == step }
                    val at = System.currentTimeMillis()
                    dao.putPhoto(PhotoEntity(containerId = containerId, step = step, localPath = destination.absolutePath, capturedAt = at))
                    val complete = (item.photos.map { it.step }.toSet() + step).size == batch.job.draft.kind.photoCount
                    dao.updateContainer(item.container.copy(completedAt = if (complete) at else null))
                    val updated = checkedJob(jobId)
                    dao.updateJob(edited(batch.job,at).copy(completedAt = if (updated.complete) at else null))
                    if (updated.complete) {
                        val first = batch.job.completionTriggeredAt == null
                        if (first) dao.updateJob(edited(batch.job,at).copy(completedAt = at, completionTriggeredAt = at))
                        var reminder: LoggerReminder? = null
                        if (batch.job.draft.kind == JobKind.DELIVERY) {
                            dao.insertReminder(LoggerReminder(jobId = jobId, referenceNumber = batch.job.draft.referenceNumber,
                                dueAt = reminderDueAt(batch.job.completionTriggeredAt ?: at)))
                            reminder = dao.jobReminder(jobId)
                        }
                        if (first || dao.uploadJob(jobId) != null) queuePhotos(checkedJob(jobId))
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
                val batch = checkedJob(jobId)
                checkEditable(batch)
                require(!batch.isManagedReturn) { "Return-Fotos einzeln löschen." }
                val item = batch.containers.first { it.container.id == containerId }
                val paths = item.photos.filter { it.step >= fromStep }.map { it.localPath }
                dao.deletePhotosFrom(containerId, fromStep)
                dao.updateContainer(item.container.copy(completedAt = null))
                dao.updateJob(edited(batch.job,System.currentTimeMillis()).copy(completedAt = null))
                if (dao.uploadJob(jobId) != null) dao.uploadState(jobId,"COMPLETE_PENDING_UPLOAD","Pflichtfotos fehlen · Upload pausiert",System.currentTimeMillis())
                paths
            }
            deleted.forEach { File(it).delete() }
        }
    }

    suspend fun export(jobId: String, uri: Uri): LoggerReminder? = withContext(Dispatchers.IO) {
        mutations.withLock {
            val batch = checkedJob(jobId)
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
            val batch = checkedJob(jobId)
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

    suspend fun preparePhotoShare(jobId: String, teamsPackage: String? = null, photoId: String? = null, requireComplete: Boolean = false): Intent = withContext(Dispatchers.IO) {
        mutations.withLock {
            val batch = checkedJob(jobId)
            require(!requireComplete || batch.complete) { "Zuerst alle Pflichtfotos aufnehmen." }
            pruneSharedExports(System.currentTimeMillis())
            val directory = File(context.cacheDir,"shared-photos/$jobId/${UUID.randomUUID()}")
            check(directory.mkdirs()) { "Fotos konnten nicht vorbereitet werden." }
            try {
                val photos = namedPhotos(batch).filter{photoId==null || it.photo.id==photoId}.map{named->
                    val source=File(named.photo.localPath)
                    require(source.isFile && source.length()>0)
                    File(directory,named.fileName).also{source.copyTo(it)}
                }
                val intent = photoShareIntent(context,photos,teamsPackage)
                intent.putExtra(Intent.EXTRA_SUBJECT,batch.job.draft.referenceNumber)
                val numbers=batch.ordered.filter{item->item.photos.any{photoId==null || it.id==photoId}}.map{it.container.containerNumber}
                intent.putExtra(Intent.EXTRA_TEXT,photoShareText(batch.job.draft,numbers,photos.size))
                intent
            } catch (e: Exception) { directory.deleteRecursively();throw e }
        }
    }

    suspend fun prepareSelectedPhotoShare(photoIds: Set<String>, teamsPackage: String? = null): Intent = withContext(Dispatchers.IO) {
        mutations.withLock {
            val selected = selectedPhotos(photoIds)
            pruneSharedExports(System.currentTimeMillis())
            val directory = File(context.cacheDir,"shared-photos/selection-${UUID.randomUUID()}")
            check(directory.mkdirs())
            try {
                val files = selected.map { (jobId, number, photo) ->
                    val folder=File(directory,jobId).apply { check(mkdirs() || isDirectory) }
                    File(folder,namedPhotos(checkedJob(jobId)).first{it.photo.id==photo.id}.fileName).also { File(photo.localPath).copyTo(it) }
                }
                val text=selected.groupBy{it.first}.map{(jobId,rows)->
                    photoShareText(checkedJob(jobId).job.draft,rows.map{it.second},rows.size)
                }.joinToString("\n\n")
                photoShareIntent(context,files,teamsPackage).apply {
                    putExtra(Intent.EXTRA_TEXT,text)
                    putExtra(Intent.EXTRA_SUBJECT,"Container Track · Fotodokumentation")
                }
            } catch(e:Exception) { directory.deleteRecursively();throw e }
        }
    }

    private suspend fun selectedPhotos(ids:Set<String>):List<Triple<String,String,PhotoEntity>> {
        require(ids.isNotEmpty())
        val photos=dao.observeJobs().first().flatMap { batch -> batch.ordered.flatMap { item ->
            item.photos.filter { it.id in ids }.map { Triple(batch.job.id,item.container.containerNumber,it) }
        } }
        photos.map{it.first}.distinct().forEach{checkedJob(it)}
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
                        val target=android.provider.DocumentsContract.createDocument(context.contentResolver,folder,"image/jpeg",namedPhotos(checkedJob(job)).first{it.photo.id==photo.id}.fileName)
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
            item.photos.sortedBy { it.step }.map { ExportPhoto(it.step, File(it.localPath),it.returnPhase,it.returnCategory) }) }
        file.outputStream().use { output ->
            writeExportZip(output, batch.job.draft, containers, metadata(batch, preparedAt),
                Instant.ofEpochMilli(preparedAt).atZone(ZoneId.systemDefault()).toLocalDate(),managedReturn=batch.isManagedReturn)
        }
    }

    private suspend fun finishExport(jobId: String): LoggerReminder? = withContext(NonCancellable) {
        val now = System.currentTimeMillis()
        val reminder = database.withTransaction {
            val job = checkedJob(jobId).job
            dao.updateJob(job.copy(exportedAt = firstExportAt(job.exportedAt, now), updatedAt = now))
            dao.jobReminder(job.id)
        }
        File(context.filesDir, "pending/$jobId").deleteRecursively()
        reminder
    }

    private suspend fun queuePhotos(batch: JobWithContainers) {
        if(!automaticUploads || batch.isManagedReturn)return // Active workflow uses explicit e-mail preparation.
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
        check(automaticUploads) { "Bitte die Fotos per E-Mail vorbereiten." }
        mutations.withLock {
            database.withTransaction {
                val batch = checkedJob(jobId)
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
                    if(batch.isManagedReturn && batch.job.emailSentAt==null && dao.jobReminder(batch.job.id)==null) {
                        dao.insertReminder(LoggerReminder(jobId=batch.job.id,referenceNumber=batch.job.draft.referenceNumber,
                            dueAt=returnReminderDueAt(batch.job.createdAt),kind=ReminderKind.RETURN_CLOSE))
                    }
                    if (batch.complete && batch.job.draft.kind == JobKind.DELIVERY &&
                        batch.job.completionTriggeredAt != null && dao.jobReminder(batch.job.id) == null) {
                        dao.insertReminder(LoggerReminder(jobId = batch.job.id, referenceNumber = batch.job.draft.referenceNumber,
                            dueAt = reminderDueAt(batch.job.completionTriggeredAt)))
                    }
                }
            }
            dao.confirmedReminders().forEach { reminder -> runCatching { alarms.cancel(reminder.id) } }
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
            checkedJob(jobId)
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
        val reminder=dao.reminder(id) ?: return@withContext
        checkedJob(reminder.jobId)
        check(reminder.kind==ReminderKind.LOGGER) { "Return erst nach E-Mail-Versand bestätigen." }
        dao.confirmReminder(id, System.currentTimeMillis())
        alarms.cancel(id)
    }


    private fun checkEditable(batch:JobWithContainers) {
        check(batch.job.exportedAt==null && batch.job.emailSentAt==null) { "Abgeschlossene Aufträge können nicht verändert werden." }
    }
    private fun edited(job:JobEntity,at:Long)=job.copy(updatedAt=at,documentationRevision=job.documentationRevision+1,
        emailPreparedRevision=null,emailPreparedAt=null)

    suspend fun saveReturnPhoto(jobId:String,containerId:String,phase:ReturnPhotoPhase,category:ReturnPhotoCategory,
        captured:File,replaceId:String?=null)=withContext(Dispatchers.IO) {
        mutations.withLock {
            require(captured.isFile && captured.length()>0)
            val directory=File(context.filesDir,"photos/$jobId/$containerId").apply{mkdirs()}
            val target=File(directory,"${UUID.randomUUID()}.jpg")
            val staged=File(directory,"${target.name}.tmp")
            var committed=false
            var replaced:PhotoEntity?=null
            try {
                captured.inputStream().use{input->staged.outputStream().use{output->input.copyTo(output);output.fd.sync()}}
                check(staged.renameTo(target))
                withContext(NonCancellable) {
                    database.withTransaction {
                        val batch=checkedJob(jobId);check(batch.isManagedReturn);checkEditable(batch)
                        val item=batch.containers.first{it.container.id==containerId}
                        val old=replaceId?.let{id->requireNotNull(item.photos.find{it.id==id})}
                        if(old!=null)require(old.returnPhase==phase && old.returnCategory==category)
                        val progress=returnProgress(item.photos)
                        if(phase==ReturnPhotoPhase.INITIAL) {
                            require(category in setOf(ReturnPhotoCategory.CONTENT,ReturnPhotoCategory.DAMAGE,ReturnPhotoCategory.OTHER))
                            if(category==ReturnPhotoCategory.CONTENT)require(old!=null || !progress.hasContent)
                            else require(progress.hasContent) { "Zuerst das Inhalts-Pflichtfoto aufnehmen." }
                        } else {
                            require(category==ReturnPhotoCategory.CLEAN && batch.initialDocumentationComplete) { "Zuerst alle Container anfangs dokumentieren." }
                            require(old!=null || progress.cleanCount<progress.initialCount) { "Die erforderlichen Abschlussfotos sind bereits vorhanden." }
                        }
                        val at=System.currentTimeMillis()
                        dao.putPhoto(PhotoEntity(containerId=containerId,step=old?.step ?: ((item.photos.maxOfOrNull{it.step} ?: 0)+1),
                            localPath=target.path,capturedAt=at,returnPhase=phase,returnCategory=category))
                        replaced=old
                        updateReturnState(jobId,containerId,batch.job,at)
                    }
                    committed=true
                    replaced?.let{File(it.localPath).delete()};captured.delete()
                }
            } finally {staged.delete();if(!committed)target.delete()}
        }
    }
    private suspend fun updateReturnState(jobId:String,containerId:String,previous:JobEntity,at:Long) {
        val batch=checkedJob(jobId)
        val item=batch.containers.first{it.container.id==containerId}
        dao.updateContainer(item.container.copy(completedAt=if(returnProgress(item.photos).complete)item.container.completedAt ?: at else null))
        dao.updateJob(edited(previous,at).copy(completedAt=if(batch.complete)previous.completedAt ?: at else null,
            completionTriggeredAt=if(batch.complete)previous.completionTriggeredAt ?: at else previous.completionTriggeredAt))
    }
    suspend fun deleteReturnPhoto(jobId:String,containerId:String,photoId:String)=withContext(Dispatchers.IO) {
        mutations.withLock {
            val path=database.withTransaction {
                val batch=checkedJob(jobId);check(batch.isManagedReturn);checkEditable(batch)
                val photo=batch.containers.first{it.container.id==containerId}.photos.first{it.id==photoId}
                dao.deletePhoto(photo.id)
                updateReturnState(jobId,containerId,batch.job,System.currentTimeMillis())
                photo.localPath
            }
            File(path).delete()
        }
    }
    suspend fun prepareOrderEmail(jobId:String):Intent=withContext(Dispatchers.IO) {
        mutations.withLock {
            val batch=checkedJob(jobId)
            require(batch.complete) { "Zuerst alle Pflicht- und Abschlussfotos aufnehmen." }
            pruneSharedExports(System.currentTimeMillis())
            val directory=File(context.cacheDir,"shared-photos/$jobId/${UUID.randomUUID()}")
            check(directory.mkdirs())
            try {
                val attachments=namedPhotos(batch)
                val files=attachments.map{named->
                    val source=File(named.photo.localPath);require(source.isFile && source.length()>0)
                    File(directory,named.fileName).also{source.copyTo(it)}
                }
                val body=photoShareText(batch.job.draft,attachments.map{it.containerNumber},files.size)+
                    "\n\n"+attachments.joinToString("\n"){it.fileName+" · "+it.label}
                val intent=emailPhotoIntent(context,files,batch.job.draft.referenceNumber,body)
                dao.updateJob(batch.job.copy(emailPreparedRevision=batch.job.documentationRevision,emailPreparedAt=System.currentTimeMillis()))
                intent
            } catch(e:Exception){directory.deleteRecursively();throw e}
        }
    }
    suspend fun confirmEmailSent(jobId:String)=withContext(Dispatchers.IO+NonCancellable) {
        mutations.withLock {
            val reminder=database.withTransaction {
                val batch=checkedJob(jobId)
                if(batch.job.emailSentAt!=null)return@withTransaction null
                check(batch.complete && batch.job.emailPreparedAt!=null && batch.job.emailPreparedRevision==batch.job.documentationRevision) {
                    "Bitte zuerst die aktuelle Fotodokumentation per E-Mail vorbereiten."
                }
                val at=System.currentTimeMillis()
                dao.updateJob(batch.job.copy(emailSentAt=at,updatedAt=at))
                dao.jobReminder(jobId)?.takeIf{it.kind==ReminderKind.RETURN_CLOSE}?.also{dao.confirmReminder(it.id,at)}
            }
            reminder?.let{alarms.cancel(it.id)}
        }
    }

    private fun metadata(batch: JobWithContainers, preparedAt: Long): String {
        val job = batch.job
        val containers = JSONArray()
        for (item in batch.ordered) {
            val photos = JSONArray()
            for (named in namedPhotos(batch).filter{it.photo.containerId==item.container.id}) {
                val photo=named.photo
                photos.put(JSONObject()
                    .put("step", photo.step)
                    .put("fileName", named.fileName)
                    .put("phase",photo.returnPhase.name).put("category",photo.returnCategory.name)
                    .put("capturedAt", photo.capturedAt))
            }
            containers.put(JSONObject()
                .put("containerNumber", item.container.containerNumber)
                .put("sequenceIndex", item.container.sequenceIndex)
                .put("completedAt", item.container.completedAt ?: JSONObject.NULL)
                .put("photos", photos))
        }
        return JSONObject().apply {
        put("schemaVersion", 3); put("id", job.id); put("type", job.draft.kind.name)
        put("referenceNumber", job.draft.referenceNumber)
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
