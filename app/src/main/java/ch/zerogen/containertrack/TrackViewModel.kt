package ch.zerogen.containertrack

import android.app.Application
import android.app.Activity
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
    val microsoftIdentity = app.accounts.identity
    val microsoftStatus = app.accounts.status
    val microsoftConfigured get() = app.microsoftConfig.configured
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
    fun stagePhoto(jobId: String, containerId: String, step: Int, captured: File) = runAction {
        val staged = withContext(Dispatchers.IO) {
            val directory = File(app.filesDir, "pending/$jobId/$containerId").apply { mkdirs() }
            val destination = File(directory, "${UUID.randomUUID()}.jpg")
            check(captured.renameTo(destination)) { "Foto konnte nicht gespeichert werden." }
            destination
        }
        val key = pendingKey(jobId, containerId)
        saved.get<String>(key)?.let { File(it).delete() }
        saved[key + ":step"] = step
        saved[key] = staged.absolutePath
    }
    fun discardPhoto(jobId: String, containerId: String) {
        val key = pendingKey(jobId, containerId)
        saved.get<String>(key)?.let { File(it).delete() }
        saved[key] = null; saved[key + ":step"] = 0
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
                    is MicrosoftAuthenticationRequired -> "Microsoft-Anmeldung oder Firmenfreigabe prüfen."
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
    fun sharePhotos(jobId: String, teams: Boolean = false, photoId: String? = null) = runAction {
        shareRequests.send(app.repository.preparePhotoShare(jobId,if (teams) "com.microsoft.teams" else null,photoId))
    }
    fun shareSelectedPhotos(ids:Set<String>) = runAction { shareRequests.send(app.repository.prepareSelectedPhotoShare(ids)) }
    fun finishAndSharePhotos(jobId:String) = runAction {
        shareRequests.send(app.repository.preparePhotoShare(jobId,"com.microsoft.teams",requireComplete=true))
    }
    fun shareSelectedPhotosToTeams(ids:Set<String>) = runAction {
        shareRequests.send(app.repository.prepareSelectedPhotoShare(ids,"com.microsoft.teams"))
    }
    fun saveSelectedPhotos(ids:Set<String>,tree:Uri) = runAction {
        val count=app.repository.saveSelectedPhotos(ids,tree)
        notice.value="$count Originalfotos gespeichert."
    }
    fun uploadPhotos(jobId: String) = runAction {
        val owner = microsoftIdentity.value?.accountId ?: throw MicrosoftAuthenticationRequired()
        app.repository.enqueuePhotoUpload(jobId,owner)
        notice.value = "Fotos zur Übertragung vorgemerkt. Der Status zeigt die tatsächliche Bestätigung."
    }
    fun signIn(activity: Activity) = runAction { app.accounts.signIn(activity);app.repository.reconcilePendingEffects() }
    fun signOut() = runAction { app.accounts.signOut();app.uploadScheduler.stopForLogout() }
    fun switchAccount(activity: Activity) = runAction {
        app.accounts.signOut();app.uploadScheduler.stopForLogout();app.accounts.signIn(activity);app.repository.reconcilePendingEffects()
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
