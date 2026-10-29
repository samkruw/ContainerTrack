package ch.zerogen.containertrack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
    var photosOpen by rememberSaveable(jobId, containerId) { mutableStateOf(false) }
    var informationOpen by rememberSaveable(jobId, containerId) { mutableStateOf(false) }
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
    val phasePhotos = item.photos.filter { it.returnPhase == displayedPhase }.sortedBy { it.step }
    val reviewing = selected != null && (!capturing || readonly) && !hasPending
    val showingCamera = !hasPending && !reviewing && !readonly && (capturing || automaticContent) && captureAllowed
    val pendingTarget = item.photos.find { it.id == pendingReplaceId }
    val maySave = pendingFile != null && (pendingReplaceId == null || pendingTarget != null) &&
        (pendingPhase == ReturnPhotoPhase.INITIAL ||
            (batch.initialDocumentationComplete && (pendingTarget != null || cleanAvailable)))

    fun startCapture(nextPhase: ReturnPhotoPhase, nextCategory: ReturnPhotoCategory, photoId: String? = null) {
        phaseName = nextPhase.name; categoryName = nextCategory.name
        replaceId = photoId; reviewId = null; photosOpen = false; capturing = true
    }
    fun openOverview() { reviewId = null; replaceId = null; capturing = false }
    fun openSavedPhotos() { openOverview(); photosOpen = true }
    LaunchedEffect(hasPending) { if (hasPending) photosOpen = false }

    CaptureWorkspace(
        header = {
            CaptureContextHeader("Container ${item.container.containerNumber}",
                "${batch.job.draft.referenceNumber} · Rücknahme",
                "${progress.initialCount} Anfangsfotos · ${progress.cleanCount} / ${progress.initialCount} Sauberfotos",
                onEdit = if (readonly || busy) null else ({
                    renameValue = item.container.containerNumber; renameError = null; renaming = true
                }))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ReturnPhotoPhase.entries.forEach { tab ->
                    FilterChip(selected = displayedPhase == tab, enabled = !busy && !hasPending,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small,
                        onClick = { phaseName = tab.name; openOverview() },
                        label = { Text(if (tab == ReturnPhotoPhase.INITIAL) "Anfang · ${progress.initialCount}" else "Sauber · ${progress.cleanCount}/${progress.initialCount}",
                            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
                IconButton(onClick = { informationOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Info, "Return-Fotohinweise", tint = Muted)
                }
            }
        },
        media = { mediaModifier ->
            when {
                hasPending -> ReturnPhotoMedia(pendingFile?.path, "Vorschau ${returnPhotoTitle(pendingPhase, pendingCategory)}",
                    null, when {
                        pendingFile == null -> "Aufnahme nicht mehr verfügbar. Bitte erneut aufnehmen."
                        !maySave -> "Speichern derzeit nicht möglich. Anfangsfotos und Fotoanzahl prüfen."
                        else -> null
                    }, mediaModifier)
                reviewing -> ReturnPhotoMedia(selected!!.localPath,
                    named[selected.id]?.label ?: returnPhotoTitle(selected.returnPhase, selected.returnCategory),
                    named[selected.id]?.fileName, null, mediaModifier)
                showingCamera -> key(jobId, containerId, capturePhase, captureCategory, captureReplaceId) {
                    Column(mediaModifier) {
                        Text(if (captureReplaceId != null) "Neuaufnahme · ${returnPhotoTitle(capturePhase, captureCategory)}"
                            else if (automaticContent) "Pflichtfoto · Inhalt" else returnPhotoTitle(capturePhase, captureCategory),
                            modifier = Modifier.padding(bottom = 4.dp), color = Lime,
                            style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        CameraSurface(scan = false, enabled = !busy, onPhoto = {
                            vm.stageReturnPhoto(jobId, containerId, capturePhase, captureCategory, captureReplaceId, it)
                        }, modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
                else -> {
                    val last = phasePhotos.lastOrNull()
                    val message = when {
                        readonly -> "Abgeschlossen · Fotos zur Ansicht"
                        capturing -> "Aufnahme derzeit nicht möglich. Anfangsfotos und Fotoanzahl prüfen."
                        displayedPhase == ReturnPhotoPhase.CLEAN && !batch.initialDocumentationComplete ->
                            "Zuerst bei allen Containern das Inhaltsfoto aufnehmen."
                        progress.cleanCount > progress.initialCount -> "Überzählige Sauberfotos über Fotos einzeln löschen."
                        displayedPhase == ReturnPhotoPhase.CLEAN && progress.complete -> "Fotodokumentation vollständig"
                        last == null -> "Noch keine Fotos in dieser Phase"
                        else -> null
                    }
                    ReturnPhotoMedia(last?.localPath, last?.let { named[it.id]?.label } ?: "Return-Fotodokumentation",
                        last?.let { named[it.id]?.fileName }, message, mediaModifier)
                }
            }
        },
        actions = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    hasPending -> {
                        ReturnPrimaryAction("Foto verwenden", !busy && maySave) {
                            vm.savePendingReturnPhoto(jobId, containerId) {
                                phaseName = pendingPhase.name; openOverview()
                            }
                        }
                        ReturnActionRow(
                            "Erneut aufnehmen", !busy, { startCapture(pendingPhase, pendingCategory, pendingReplaceId); vm.discardPhoto(jobId, containerId) },
                            "Aufnahme verwerfen", !busy, { vm.discardPhoto(jobId, containerId); openOverview() })
                    }
                    reviewing -> {
                        if (!readonly) {
                            ReturnPrimaryAction("Dieses Foto erneut aufnehmen",
                                !busy && (selected!!.returnPhase != ReturnPhotoPhase.CLEAN || batch.initialDocumentationComplete)) {
                                startCapture(selected!!.returnPhase, selected.returnCategory, selected.id)
                            }
                            ReturnActionRow("Dieses Foto löschen", !busy, { deletingId = selected!!.id },
                                "Zur Fotoübersicht", !busy, ::openSavedPhotos, firstDestructive = true)
                        } else ReturnActionRow("Zur Fotoübersicht", !busy, ::openSavedPhotos, "Zum Auftrag", !busy, onDone)
                    }
                    else -> {
                        if (showingCamera && !automaticContent) {
                            ReturnPrimaryAction("Aufnahme abbrechen", !busy, ::openOverview)
                        } else if (!showingCamera && !readonly && capturing) {
                            ReturnPrimaryAction("Zur Fotoübersicht", !busy, ::openOverview)
                        } else if (!showingCamera && !readonly && displayedPhase == ReturnPhotoPhase.INITIAL) {
                            ReturnActionRow("Schaden ergänzen · optional", !busy && progress.hasContent,
                                { startCapture(ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.DAMAGE) },
                                "Sonstiges ergänzen · optional", !busy && progress.hasContent,
                                { startCapture(ReturnPhotoPhase.INITIAL, ReturnPhotoCategory.OTHER) })
                        } else if (!showingCamera && !readonly && displayedPhase == ReturnPhotoPhase.CLEAN) {
                            ReturnPrimaryAction("Sauberfoto aufnehmen", !busy && cleanAvailable) {
                                startCapture(ReturnPhotoPhase.CLEAN, ReturnPhotoCategory.CLEAN)
                            }
                        }
                        ReturnActionRow("Fotos (${phasePhotos.size})", !busy && phasePhotos.isNotEmpty(), { photosOpen = true },
                            "Zum Auftrag", !busy, onDone)
                    }
                }
            }
        })

    if (photosOpen) ModalBottomSheet(onDismissRequest = { photosOpen = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gespeicherte Fotos · ${phasePhotos.size}", style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(phasePhotos, key = { it.id }) { photo ->
                    ReturnOutlineAction(named[photo.id]?.label ?: returnPhotoTitle(photo.returnPhase, photo.returnCategory), !busy,
                        onClick = { reviewId = photo.id; phaseName = photo.returnPhase.name; capturing = false; replaceId = null; photosOpen = false },
                        caption = named[photo.id]?.fileName)
                }
            }
            TextButton(onClick = { photosOpen = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Schliessen") }
        }
    }
    if (informationOpen) AlertDialog(onDismissRequest = { informationOpen = false }, title = { Text("Return-Fotodokumentation") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Je Container ist ein Inhaltsfoto erforderlich. Schaden und Sonstiges sind zusätzliche Anfangsfotos. Jedes Anfangsfoto zählt zum Soll der Leer-/Sauber-Fotos.\n\nAbschlussfotos sind möglich, sobald bei allen Containern der Inhalt dokumentiert ist. Die Anzahl muss pro Container genau übereinstimmen.\n\nNeuaufnahmen ersetzen das ausgewählte Foto erst nach dem Bestätigen. Einzelnes Löschen entfernt keine anderen Fotos. Die 24h-Erinnerung bleibt bis zur Versandbestätigung offen.")
        } },
        confirmButton = { TextButton(onClick = { informationOpen = false }) { Text("Verstanden") } })
    item.photos.find { it.id == deletingId }?.takeIf { !readonly }?.let { photo ->
        ReturnDeletePhotoDialog(photo, busy, onDismiss = { deletingId = null }, onDelete = {
            deletingId = null; openOverview(); vm.deleteReturnPhoto(jobId, containerId, photo.id)
        })
    }
    if (renaming && !readonly) AlertDialog(onDismissRequest = { if (!busy) renaming = false },
        title = { Text("Container-Nummer bearbeiten") },
        text = { OutlinedTextField(value = renameValue, onValueChange = { renameValue = it; renameError = null },
            label = { Text("4 Ziffern") }, singleLine = true, enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = renameError != null,
            supportingText = { renameError?.let { Text(it) } }) },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            renameError = containerError(renameValue, batch.containers.filter { it.container.id != containerId }.map { it.container.containerNumber }.toSet())
            if (renameError == null) vm.rename(jobId, containerId, renameValue) { renaming = false }
        }) { Text("Speichern") } }, dismissButton = { TextButton(enabled = !busy, onClick = { renaming = false }) { Text("Abbrechen") } })
}

@Composable
private fun ReturnPhotoMedia(path: String?, description: String, fileName: String?, message: String?, modifier: Modifier) {
    Box(modifier.background(Graphite, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
        if (path != null) LocalPhoto(path, description, Modifier.fillMaxSize(), bounded = true)
        else Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(48.dp), tint = Muted)
        if (message != null) Text(message, modifier = Modifier.align(Alignment.Center).background(Navy.copy(alpha = .90f)).padding(12.dp),
            style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        if (path != null) Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Navy.copy(alpha = .90f)).padding(8.dp)) {
            Text(description, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            fileName?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun ReturnPrimaryAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = text },
        shape = MaterialTheme.shapes.small, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Navy)) {
        Text(returnActionLabel(text), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReturnActionRow(first: String, firstEnabled: Boolean, onFirst: () -> Unit,
                            second: String, secondEnabled: Boolean, onSecond: () -> Unit,
                            firstDestructive: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f)) { ReturnOutlineAction(first, firstEnabled, onFirst, destructive = firstDestructive) }
        Box(Modifier.weight(1f)) { ReturnOutlineAction(second, secondEnabled, onSecond) }
    }
}

@Composable
private fun ReturnOutlineAction(text: String, enabled: Boolean, onClick: () -> Unit,
                                caption: String? = null, destructive: Boolean = false) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = text },
        shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Hairline),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.fillMaxWidth()) {
            Text(returnActionLabel(text), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            caption?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** Keep dock labels legible on narrow screens; full actions remain available to accessibility. */
private fun returnActionLabel(text: String): String = when (text) {
    "Dieses Foto erneut aufnehmen", "Erneut aufnehmen" -> "Neu aufnehmen"
    "Aufnahme verwerfen" -> "Verwerfen"
    "Aufnahme abbrechen" -> "Abbrechen"
    "Dieses Foto löschen" -> "Löschen"
    "Zur Fotoübersicht" -> "Fotos"
    "Schaden ergänzen · optional" -> "Schaden"
    "Sonstiges ergänzen · optional" -> "Sonstiges"
    else -> text
}

@Composable
private fun ReturnDeletePhotoDialog(photo: PhotoEntity, busy: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Dieses Foto löschen?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(if (photo.returnPhase == ReturnPhotoPhase.INITIAL)
                "Nur dieses Anfangsfoto wird gelöscht. Das Sauberfoto-Soll wird neu berechnet. Bereits gespeicherte Sauberfotos bleiben erhalten und können bei Bedarf einzeln gelöscht werden."
            else "Nur dieses Sauberfoto wird gelöscht. Die anderen Fotos bleiben gespeichert.")
        } }, confirmButton = {
            TextButton(enabled = !busy, onClick = onDelete) { Text("Foto löschen", color = MaterialTheme.colorScheme.error) }
        }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Abbrechen") } })
}

private fun returnPhotoTitle(phase: ReturnPhotoPhase, category: ReturnPhotoCategory): String =
    if (phase == ReturnPhotoPhase.CLEAN) "Leer / sauber · gereinigter Container" else "Anfang · " + when (category) {
        ReturnPhotoCategory.CONTENT -> "Inhalt"
        ReturnPhotoCategory.DAMAGE -> "Schaden"
        ReturnPhotoCategory.OTHER -> "Sonstiges"
        ReturnPhotoCategory.CLEAN -> "gereinigter Container"
    }
