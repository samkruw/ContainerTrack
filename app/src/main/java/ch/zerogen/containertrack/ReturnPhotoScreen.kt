package ch.zerogen.containertrack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun ReturnPhotoScreen(batch: JobWithContainers, containerId: String, vm: TrackViewModel,
                      busy: Boolean, onDone: () -> Unit, selectedStep: Int = 0) {
    val item = batch.containers.find { it.container.id == containerId } ?: return
    val jobId = batch.job.id
    val readonly = batch.job.exportedAt != null || batch.job.emailSentAt != null
    val progress = returnProgress(item.photos)
    val entryPhoto = item.photos.find { it.step == selectedStep }
    var phaseName by rememberSaveable(jobId, containerId, selectedStep) {
        mutableStateOf((entryPhoto?.returnPhase ?: ReturnPhotoPhase.INITIAL).name)
    }
    var categoryName by rememberSaveable(jobId, containerId, selectedStep) { mutableStateOf(ReturnPhotoCategory.CONTENT.name) }
    var reviewId by rememberSaveable(jobId, containerId, selectedStep) { mutableStateOf(entryPhoto?.id) }
    var replaceId by rememberSaveable(jobId, containerId) { mutableStateOf<String?>(null) }
    var capturing by rememberSaveable(jobId, containerId) { mutableStateOf(false) }
    var deletingId by rememberSaveable(jobId, containerId) { mutableStateOf<String?>(null) }
    var renaming by rememberSaveable(jobId, containerId) { mutableStateOf(false) }
    var renameValue by rememberSaveable(jobId, containerId) { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    val pendingPath by vm.pendingPhoto(jobId, containerId).collectAsStateWithLifecycle()
    val pendingPhaseName by vm.pendingReturnPhase(jobId, containerId).collectAsStateWithLifecycle()
    val pendingCategoryName by vm.pendingReturnCategory(jobId, containerId).collectAsStateWithLifecycle()
    val pendingReplaceId by vm.pendingReturnReplaceId(jobId, containerId).collectAsStateWithLifecycle()
    val phase = ReturnPhotoPhase.entries.firstOrNull { it.name == phaseName } ?: ReturnPhotoPhase.INITIAL
    val category = ReturnPhotoCategory.entries.firstOrNull { it.name == categoryName } ?: ReturnPhotoCategory.CONTENT
    val pendingPhase = ReturnPhotoPhase.entries.firstOrNull { it.name == pendingPhaseName } ?: ReturnPhotoPhase.INITIAL
    val pendingCategory = ReturnPhotoCategory.entries.firstOrNull { it.name == pendingCategoryName } ?: ReturnPhotoCategory.CONTENT
    val pendingFile = pendingPath?.let(::File)?.takeIf { it.isFile }
    val hasPending = !readonly && pendingPath != null
    val displayedPhase = if (hasPending) pendingPhase else phase
    val selected = item.photos.find { it.id == reviewId }
    val replacement = item.photos.find { it.id == replaceId }
    val automaticContent = !progress.hasContent && !capturing && phase == ReturnPhotoPhase.INITIAL
    val capturePhase = if (automaticContent) ReturnPhotoPhase.INITIAL else replacement?.returnPhase ?: phase
    val captureCategory = if (automaticContent) ReturnPhotoCategory.CONTENT else replacement?.returnCategory ?: category
    val captureReplaceId = if (automaticContent) null else replaceId
    val cleanAvailable = batch.initialDocumentationComplete && progress.cleanCount < progress.initialCount
    val captureAllowed = !readonly && (captureReplaceId == null || replacement != null) &&
        (capturePhase == ReturnPhotoPhase.INITIAL ||
            (batch.initialDocumentationComplete && (replacement != null || cleanAvailable)))
    val named = namedPhotos(batch).filter { it.photo.containerId == containerId }.associateBy { it.photo.id }

    fun startCapture(nextPhase: ReturnPhotoPhase, nextCategory: ReturnPhotoCategory, photoId: String? = null) {
        phaseName = nextPhase.name
        categoryName = nextCategory.name
        replaceId = photoId
        reviewId = null
        capturing = true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Container ${item.container.containerNumber}", "${batch.job.draft.referenceNumber} · Rücknahme")
        if (!readonly) TextButton(enabled = !busy, onClick = {
            renameValue = item.container.containerNumber; renameError = null; renaming = true
        }) { Text("Container-Nummer bearbeiten") }
        ReturnProgressSummary(progress.initialCount, progress.cleanCount, progress.hasContent, progress.complete)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReturnPhotoPhase.entries.forEach { tab ->
                FilterChip(selected = displayedPhase == tab, enabled = !busy && !hasPending,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    onClick = { phaseName = tab.name; reviewId = null; replaceId = null; capturing = false },
                    label = { Text(if (tab == ReturnPhotoPhase.INITIAL) "Anfang · ${progress.initialCount}" else "Sauber · ${progress.cleanCount}/${progress.initialCount}") })
            }
        }
        if (readonly) Information("Auftrag abgeschlossen. Die gespeicherten Fotos sind zur Ansicht verfügbar.")
        else if (displayedPhase == ReturnPhotoPhase.CLEAN && !batch.initialDocumentationComplete) {
            Information("Sauberfotos werden freigegeben, sobald bei allen Containern das Anfangsfoto vom Inhalt gespeichert ist.", warning = true)
        }
        when {
            hasPending -> {
                val pendingTarget = item.photos.find { it.id == pendingReplaceId }
                val maySave = pendingFile != null && (pendingReplaceId == null || pendingTarget != null) &&
                    (pendingPhase == ReturnPhotoPhase.INITIAL ||
                        (batch.initialDocumentationComplete && (pendingTarget != null || cleanAvailable)))
                ReturnPendingPhoto(pendingFile, pendingPhase, pendingCategory, pendingReplaceId != null, busy, maySave,
                    onSave = {
                        vm.savePendingReturnPhoto(jobId, containerId) {
                            phaseName = pendingPhase.name; capturing = false; replaceId = null; reviewId = null
                        }
                    },
                    onRetake = {
                        startCapture(pendingPhase, pendingCategory, pendingReplaceId)
                        vm.discardPhoto(jobId, containerId)
                    },
                    onDiscard = { vm.discardPhoto(jobId, containerId); capturing = false; replaceId = null })
            }
            selected != null && (!capturing || readonly) -> {
                ReturnPhotoReview(selected, named[selected.id]?.label ?: returnPhotoTitle(selected.returnPhase, selected.returnCategory),
                    named[selected.id]?.fileName, busy, readonly,
                    canRetake = selected.returnPhase != ReturnPhotoPhase.CLEAN || batch.initialDocumentationComplete,
                    onRetake = { startCapture(selected.returnPhase, selected.returnCategory, selected.id) },
                    onDelete = { deletingId = selected.id }, onBack = { reviewId = null })
            }
            !readonly && (capturing || automaticContent) && captureAllowed -> {
                Text(if (captureReplaceId != null) "Neuaufnahme" else if (automaticContent) "Pflichtfoto · Inhalt" else "Foto ergänzen",
                    color = Lime, style = MaterialTheme.typography.labelLarge)
                Text(returnPhotoTitle(capturePhase, captureCategory), style = MaterialTheme.typography.titleLarge)
                if (captureReplaceId != null) Information("Das gespeicherte Foto wird erst ersetzt, wenn die Neuaufnahme bestätigt ist.")
                key(jobId, containerId, capturePhase, captureCategory, captureReplaceId) {
                    CameraSurface(scan = false, enabled = !busy, onPhoto = {
                        vm.stageReturnPhoto(jobId, containerId, capturePhase, captureCategory, captureReplaceId, it)
                    })
                }
                if (!automaticContent) TextButton(enabled = !busy, onClick = {
                    capturing = false; replaceId = null
                }) { Text("Aufnahme abbrechen") }
            }
            !readonly && capturing -> {
                Information("Diese Aufnahme ist derzeit nicht möglich. Bitte die Anfangsdokumentation und das Sauberfoto-Soll prüfen.", warning = true)
                TextButton(enabled = !busy, onClick = { capturing = false; replaceId = null }) { Text("Zur Fotoübersicht") }
            }
            !readonly -> ReturnCaptureActions(phase, progress.hasContent, cleanAvailable, busy,
                onCapture = { nextPhase, nextCategory -> startCapture(nextPhase, nextCategory) })
        }
        if (!hasPending && (!capturing || readonly) && progress.hasContent) {
            PrimaryButton(when {
                readonly || progress.complete -> "Zum Auftrag"
                !batch.initialDocumentationComplete -> "Zum Auftrag · nächster Container"
                else -> "Zum Auftrag · Sauberfotos später"
            }, !busy, onDone)
        } else {
            TextButton(enabled = !busy, onClick = onDone) { Text("Zum Auftrag") }
        }
        if (!hasPending && (!capturing || readonly)) {
            ReturnSavedPhotos(item.photos.filter { it.returnPhase == phase }, named, busy,
                onSelect = { photo -> reviewId = photo.id; phaseName = photo.returnPhase.name })
        }
    }
    item.photos.find { it.id == deletingId }?.takeIf { !readonly }?.let { photo ->
        ReturnDeletePhotoDialog(photo, busy, onDismiss = { deletingId = null }, onDelete = {
            deletingId = null; reviewId = null; replaceId = null; capturing = false
            vm.deleteReturnPhoto(jobId, containerId, photo.id)
        })
    }
    if (renaming && !readonly) AlertDialog(onDismissRequest = { if (!busy) renaming = false },
        title = { Text("Container-Nummer bearbeiten") },
        text = {
            OutlinedTextField(value = renameValue, onValueChange = { renameValue = it; renameError = null },
                label = { Text("4 Ziffern") }, singleLine = true, enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = renameError != null,
                supportingText = { renameError?.let { Text(it) } })
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                renameError = containerError(renameValue, batch.containers.filter { it.container.id != containerId }.map { it.container.containerNumber }.toSet())
                if (renameError == null) vm.rename(jobId, containerId, renameValue) { renaming = false }
            }) { Text("Speichern") }
        }, dismissButton = { TextButton(enabled = !busy, onClick = { renaming = false }) { Text("Abbrechen") } })
}

@Composable
private fun ReturnProgressSummary(initialCount: Int, cleanCount: Int, hasContent: Boolean, complete: Boolean) {
    GlassSurface {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (complete) "Fotodokumentation vollständig" else "Anfang dokumentieren, danach sauber fotografieren.",
                style = MaterialTheme.typography.bodyMedium, color = if (complete) Lime else Muted)
            StatusBadge(if (hasContent) "Inhalt dokumentiert" else "Pflichtfoto Inhalt fehlt", confirmed = hasContent)
            Text("$initialCount Anfangsfotos · $cleanCount / $initialCount Sauberfotos", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { if (initialCount == 0) 0f else (cleanCount.toFloat() / initialCount).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("Jedes Anfangsfoto zählt zum Soll der Sauberfotos, auch Schaden und Sonstiges.", style = MaterialTheme.typography.bodySmall, color = Muted)
            if (cleanCount > initialCount) {
                Information("${cleanCount - initialCount} Sauberfotos über dem Soll. Bitte überzählige Fotos einzeln auswählen und löschen. Es wird nichts automatisch gelöscht.", warning = true)
            }
        }
    }
}

@Composable
private fun ReturnPendingPhoto(file: File?, phase: ReturnPhotoPhase, category: ReturnPhotoCategory,
                               replacing: Boolean, busy: Boolean, maySave: Boolean,
                               onSave: () -> Unit, onRetake: () -> Unit, onDiscard: () -> Unit) {
    Text("Vorschau · ${returnPhotoTitle(phase, category)}", style = MaterialTheme.typography.titleMedium)
    if (file != null) LocalPhoto(file.path, "Vorschau ${returnPhotoTitle(phase, category)}")
    else Information("Die vorgemerkte Fotodatei ist nicht mehr verfügbar. Bitte erneut aufnehmen.", warning = true)
    if (replacing) Information("Diese Aufnahme ersetzt nur das ausgewählte Foto in derselben Phase und Kategorie.")
    if (!maySave && file != null) Information("Speichern derzeit nicht möglich. Bitte die Anfangsdokumentation und das aktuelle Sauberfoto-Soll prüfen.", warning = true)
    PrimaryButton("Foto verwenden", !busy && maySave, onSave)
    OutlinedButton(enabled = !busy, onClick = onRetake, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Erneut aufnehmen") }
    TextButton(enabled = !busy, onClick = onDiscard) { Text("Aufnahme verwerfen") }
}

@Composable
private fun ReturnPhotoReview(photo: PhotoEntity, label: String, fileName: String?, busy: Boolean,
                              readonly: Boolean, canRetake: Boolean, onRetake: () -> Unit,
                              onDelete: () -> Unit, onBack: () -> Unit) {
    Text(label, style = MaterialTheme.typography.titleLarge)
    LocalPhoto(photo.localPath, label)
    fileName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted) }
    if (!readonly) {
        PrimaryButton("Dieses Foto erneut aufnehmen", !busy && canRetake, onRetake)
        OutlinedButton(enabled = !busy, onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Text("Dieses Foto löschen", color = MaterialTheme.colorScheme.error)
        }
    }
    TextButton(enabled = !busy, onClick = onBack) { Text("Zur Fotoübersicht") }
}

@Composable
private fun ReturnCaptureActions(phase: ReturnPhotoPhase, hasContent: Boolean, cleanAvailable: Boolean,
                                busy: Boolean, onCapture: (ReturnPhotoPhase, ReturnPhotoCategory) -> Unit) {
    if (phase == ReturnPhotoPhase.INITIAL) {
        Text("Anfangsfotos", style = MaterialTheme.typography.titleLarge)
        if (!hasContent) PrimaryButton("Inhalt aufnehmen · Pflichtfoto", !busy) {
            onCapture(ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.CONTENT)
        }
        Information("Schäden und weitere Details können zusätzlich dokumentiert werden. Zusatzfotos erhöhen das Sauberfoto-Soll dieses Containers.")
        OutlinedButton(enabled = !busy && hasContent, onClick = { onCapture(ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.DAMAGE) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Schaden ergänzen · optional") }
        OutlinedButton(enabled = !busy && hasContent, onClick = { onCapture(ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.OTHER) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Sonstiges ergänzen · optional") }
    } else {
        Text("Sauberfotos", style = MaterialTheme.typography.titleLarge)
        Information("Den gereinigten Container dokumentieren. Die Anzahl muss den Anfangsfotos dieses Containers entsprechen.")
        PrimaryButton("Sauberfoto aufnehmen", !busy && cleanAvailable) {
            onCapture(ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
        }
    }
}

@Composable
private fun ReturnSavedPhotos(photos: List<PhotoEntity>, names: Map<String, NamedPhoto>, busy: Boolean,
                              onSelect: (PhotoEntity) -> Unit) {
    HorizontalDivider()
    Text("Gespeicherte Fotos · ${photos.size}", style = MaterialTheme.typography.titleMedium)
    if (photos.isEmpty()) Information("In dieser Phase sind noch keine Fotos gespeichert.")
    photos.sortedBy { it.step }.forEach { photo ->
        OutlinedButton(enabled = !busy, onClick = { onSelect(photo) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(names[photo.id]?.label ?: returnPhotoTitle(photo.returnPhase, photo.returnCategory))
                names[photo.id]?.fileName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted) }
            }
        }
    }
}

@Composable
private fun ReturnDeletePhotoDialog(photo: PhotoEntity, busy: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Dieses Foto löschen?") },
        text = {
            Text(if (photo.returnPhase == ReturnPhotoPhase.INITIAL)
                "Nur dieses Anfangsfoto wird gelöscht. Das Sauberfoto-Soll wird neu berechnet. Bereits gespeicherte Sauberfotos bleiben erhalten und können bei Bedarf einzeln gelöscht werden."
            else "Nur dieses Sauberfoto wird gelöscht. Die anderen Fotos bleiben gespeichert.")
        }, confirmButton = {
            TextButton(enabled = !busy, onClick = onDelete) { Text("Foto löschen", color = MaterialTheme.colorScheme.error) }
        }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Abbrechen") } })
}

private fun returnPhotoTitle(phase: ReturnPhotoPhase, category: ReturnPhotoCategory): String =
    if (phase == ReturnPhotoPhase.CLEAN) "Sauber · gereinigter Container" else "Anfang · " + when (category) {
        ReturnPhotoCategory.CONTENT -> "Inhalt"
        ReturnPhotoCategory.DAMAGE -> "Schaden"
        ReturnPhotoCategory.OTHER -> "Sonstiges"
        ReturnPhotoCategory.CLEAN -> "gereinigter Container"
    }
