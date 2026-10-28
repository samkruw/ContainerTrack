package ch.zerogen.containertrack

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val app = application as ContainerTrackApp
    val uploadJobs = app.database.dao().observeUploadJobs().map{rows->rows.filter{it.state=="SYNCED"}}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val photoUploads = app.database.dao().observePhotoUploads().map{rows->rows.filter{it.uploadedAt!=null}}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val online = app.network.online
    val search = saved.getStateFlow("orderSearch", "")
    val filter = saved.getStateFlow("orderFilter", "ACTIVE")
    fun search(value: String) { saved["orderSearch"] = value }
    fun filter(value: String) { saved["orderFilter"] = value }
    val jobs = app.repository.jobs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reminders = app.repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val notice = MutableStateFlow<String?>(null)
    private val shareRequests = Channel<Intent>(Channel.BUFFERED)
    val shares = shareRequests.receiveAsFlow()
    val activeJob = saved.getStateFlow<String?>("activeJob", null)
    val now = MutableStateFlow(System.currentTimeMillis())
    var pendingExport: String?
        get() = saved["pendingExport"]
        set(value) { saved["pendingExport"] = value }
    val formKind = saved.getStateFlow("formKind", JobKind.DELIVERY.name)
    val formContainer = saved.getStateFlow("formContainer", "")
    var createdContainerId: String? = null
        private set
    val formReference = saved.getStateFlow("formReference", "")
    val formOrder = saved.getStateFlow("formOrder", "")
    val formQuantity = saved.getStateFlow("formQuantity", "1")
    val formQuick = saved.getStateFlow("formQuick", "")
    val formWork = saved.getStateFlow("formWork", "")
    val formStoppedAt = saved.getStateFlow<Long?>("formStoppedAt", null)

    init { viewModelScope.launch { while (true) { now.value = System.currentTimeMillis(); kotlinx.coroutines.delay(1_000) } } }
    fun selectJob(id: String) { saved["activeJob"] = id }
    private fun pendingKey(jobId: String, containerId: String) = "capture:$jobId:$containerId"
    fun pendingPhoto(jobId: String, containerId: String) = saved.getStateFlow<String?>(pendingKey(jobId, containerId), null)
    fun pendingStep(jobId: String, containerId: String) = saved.getStateFlow(pendingKey(jobId, containerId) + ":step", 0)
    fun pendingReturnPhase(jobId: String, containerId: String) = saved.getStateFlow<String?>(pendingKey(jobId, containerId) + ":returnPhase", null)
    fun pendingReturnCategory(jobId: String, containerId: String) = saved.getStateFlow<String?>(pendingKey(jobId, containerId) + ":returnCategory", null)
    fun pendingReturnReplaceId(jobId: String, containerId: String) = saved.getStateFlow<String?>(pendingKey(jobId, containerId) + ":replaceId", null)
    private suspend fun stageCapturedPhoto(jobId: String, containerId: String, captured: File): String {
        val previousPath = saved.get<String>(pendingKey(jobId, containerId))
        return withContext(Dispatchers.IO) {
            val directory = File(app.filesDir, "pending/$jobId/$containerId").apply { mkdirs() }
            val destination = File(directory, "${UUID.randomUUID()}.jpg")
            if (!captured.renameTo(destination)) {
                captured.copyTo(destination, overwrite = false)
                check(captured.delete()) { "Aufnahme konnte nicht abgeschlossen werden." }
            }
            previousPath?.let { File(it).delete() }
            destination.absolutePath
        }
    }
    fun stagePhoto(jobId: String, containerId: String, step: Int, captured: File) = runAction {
        val staged = stageCapturedPhoto(jobId, containerId, captured)
        val key = pendingKey(jobId, containerId)
        saved[key + ":step"] = step
        saved[key + ":returnPhase"] = null
        saved[key + ":returnCategory"] = null
        saved[key + ":replaceId"] = null
        saved[key] = staged
    }
    fun stageReturnPhoto(jobId: String, containerId: String, phase: ReturnPhotoPhase,
                         category: ReturnPhotoCategory, replaceId: String?, captured: File) = runAction {
        val staged = stageCapturedPhoto(jobId, containerId, captured)
        val key = pendingKey(jobId, containerId)
        saved[key + ":step"] = 0
        saved[key + ":returnPhase"] = phase.name
        saved[key + ":returnCategory"] = category.name
        saved[key + ":replaceId"] = replaceId
        saved[key] = staged
    }
    fun savePendingReturnPhoto(jobId: String, containerId: String, done: () -> Unit) = runAction {
        val key = pendingKey(jobId, containerId)
        val file = saved.get<String>(key)?.let(::File) ?: error("Keine Aufnahme vorhanden.")
        val phase = ReturnPhotoPhase.valueOf(saved.get<String>(key + ":returnPhase") ?: error("Aufnahmephase fehlt."))
        val category = ReturnPhotoCategory.valueOf(saved.get<String>(key + ":returnCategory") ?: error("Fotokategorie fehlt."))
        app.repository.saveReturnPhoto(jobId, containerId, phase, category, file, saved[key + ":replaceId"])
        discardPhoto(jobId, containerId)
        done()
    }
    fun deleteReturnPhoto(jobId: String, containerId: String, photoId: String) = runAction {
        app.repository.deleteReturnPhoto(jobId, containerId, photoId)
    }
    fun discardPhoto(jobId: String, containerId: String) {
        val key = pendingKey(jobId, containerId)
        saved.get<String>(key)?.let { File(it).delete() }
        saved[key] = null
        saved[key + ":step"] = 0
        saved[key + ":returnPhase"] = null
        saved[key + ":returnCategory"] = null
        saved[key + ":replaceId"] = null
    }
    fun begin(kind: JobKind) {
        saved["formKind"] = kind.name
        for (key in listOf("formReference", "formOrder", "formQuick", "formWork", "formContainer")) saved[key] = ""
        saved["formQuantity"] = "1"; saved["formStoppedAt"] = null
    }
    fun field(key: String, value: String) { saved[key] = value }
    fun confirmStopped() { if (formStoppedAt.value == null) saved["formStoppedAt"] = System.currentTimeMillis() }
    fun draft() = JobDraft(kind=JobKind.valueOf(formKind.value), referenceNumber=formReference.value,
        orderValue=formOrder.value, quantity=formQuantity.value.toIntOrNull() ?: 0, quick=formQuick.value, additionalWork=formWork.value)

    private fun runAction(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() }
            catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error.value = when (e) {
                    is EmailClientUnavailable -> "Keine E-Mail-App verfügbar. Bitte eine E-Mail-App installieren und ein Konto einrichten."
                    is IllegalArgumentException -> "Eingaben, Container-Nummer und Fotoreihenfolge bitte prüfen."
                    is IllegalStateException -> "Vorgang im aktuellen Auftragsstatus nicht möglich. Auftrag und verfügbaren Speicher prüfen."
                    is SecurityException -> "Zugriff nicht erlaubt. Berechtigungen oder Exportziel prüfen."
                    is IOException -> "Speichern fehlgeschlagen. Freien Speicher und Exportziel prüfen."
                    else -> "Vorgang fehlgeschlagen. Die gespeicherten Daten bleiben erhalten. Bitte erneut versuchen."
                }
            } finally { busy.value = false }
        }
    }

    fun create(onCreated: (String) -> Unit) = runAction {
        val id = app.repository.createJob(draft(), formStoppedAt.value,formContainer.value)
        createdContainerId=app.database.dao().job(id)?.ordered?.firstOrNull()?.container?.id
        selectJob(id); onCreated(id)
    }
    fun addContainer(jobId: String, number: String, onAdded: (String) -> Unit) = runAction {
        onAdded(app.repository.addContainer(jobId, number))
    }
    fun rename(jobId: String, containerId: String, number: String, done: () -> Unit) = runAction {
        app.repository.renameContainer(jobId, containerId, number); done()
    }
    fun savePhoto(jobId: String, containerId: String, step: Int, file: File, done: () -> Unit) = runAction {
        app.repository.savePhoto(jobId, containerId, step, file)
        discardPhoto(jobId, containerId); done()
    }
    fun deletePhotos(jobId: String, containerId: String, step: Int) = runAction { app.repository.deletePhotos(jobId, containerId, step) }
    fun export(uri: Uri) {
        val id = pendingExport ?: return
        pendingExport = null
        runAction {
            app.repository.export(id, uri)
            notice.value = "ZIP erfolgreich gespeichert."
        }
    }
    fun share(jobId: String) = runAction {
        shareRequests.send(app.repository.prepareShare(jobId))
    }
    fun prepareEmail(jobId: String) = runAction {
        shareRequests.send(app.repository.prepareOrderEmail(jobId))
    }
    fun finishAndSharePhotos(jobId: String) = prepareEmail(jobId)
    fun confirmEmailSent(jobId: String) = runAction {
        app.repository.confirmEmailSent(jobId)
        notice.value = "E-Mail als gesendet bestätigt."
    }
    fun saveSelectedPhotos(ids:Set<String>,tree:Uri) = runAction {
        val count=app.repository.saveSelectedPhotos(ids,tree)
        notice.value="$count Originalfotos gespeichert."
    }
    fun deleteJob(id: String, done: () -> Unit) = runAction {
        app.repository.deleteJob(id)
        if (activeJob.value == id) saved["activeJob"] = null
        done()
    }
    fun confirmStarted(id: Int) = runAction {
        app.repository.confirmReminder(id)
    }
}
