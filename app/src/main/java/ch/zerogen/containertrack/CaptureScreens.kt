package ch.zerogen.containertrack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(batch: JobWithContainers, vm: TrackViewModel, busy: Boolean, onAdded: (String) -> Unit) {
    var manual by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var scanned by rememberSaveable { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var manualOpen by rememberSaveable { mutableStateOf(false) }
    val existing = batch.containers.map { it.container.containerNumber }.toSet()
    fun accept(number: String) {
        val validation = containerError(number, existing)
        if (validation != null) { error = validation; scanned = number; manual = number; manualOpen = true }
        else { scanned = number; manual = number; error = null; vm.addContainer(batch.job.id, number, onAdded) }
    }
    CaptureWorkspace(header = {
        CaptureContextHeader("Container scannen", batch.job.draft.referenceNumber,
            "${batch.containers.size + 1} / ${batch.job.draft.quantity}")
    }, media = { modifier ->
        if (scanned == null) key(attempt) { CameraSurface(scan = true, enabled = !busy && !manualOpen, onNumber = { if (!manualOpen) accept(it) }, modifier = modifier) }
        else Box(modifier.background(Graphite), contentAlignment = Alignment.Center) {
            Text(error ?: "Erkannt · $scanned", Modifier.padding(12.dp))
        }
    }, actions = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (scanned != null) CaptureAction("Erneut scannen", !busy) { scanned = null; attempt++; error = null }
            else Text("4 Ziffern in den Rahmen halten", style = MaterialTheme.typography.labelSmall, color = Muted)
            CaptureAction("Nummer manuell eingeben", !busy) { manualOpen = true }
        }
    })
    if (manualOpen) AlertDialog(onDismissRequest = { if (!busy) manualOpen = false },
        title = { Text("Container-Nummer") },
        text = { OutlinedTextField(value = manual, onValueChange = { manual = it; error = null },
            label = { Text("Container-Nummer manuell") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy,
            isError = error != null, supportingText = { Text(error ?: "Exakt 4 Ziffern, z. B. 0708") }) },
        confirmButton = { TextButton(enabled = !busy, onClick = { accept(manual) }) { Text("Nummer übernehmen") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { manualOpen = false }) { Text("Abbrechen") } })

}

@Composable
fun LocalPhoto(path: String, description: String, modifier: Modifier = Modifier, bounded: Boolean = false) {
    val bitmap by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1400) inSampleSize *= 2
            }
            BitmapFactory.decodeFile(path, options)
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = description, modifier = if (bounded) modifier else modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp), contentScale = ContentScale.Fit)
    else Box(if (bounded) modifier else Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Foto wird geladen. Falls die Datei fehlt, bitte erneut aufnehmen.", Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(batch: JobWithContainers, containerId: String, selectedStep: Int, vm: TrackViewModel,
                busy: Boolean, onDone: () -> Unit) {
    if (batch.isManagedReturn) {
        ReturnPhotoScreen(batch, containerId, vm, busy, onDone, selectedStep)
        return
    }
    val item = batch.containers.find { it.container.id == containerId } ?: return
    val kind = batch.job.draft.kind
    val completed = item.photos.map { it.step }.toSet()
    val nextStep = nextPhotoStep(kind, completed)
    val readonly = batch.job.exportedAt != null || batch.job.emailSentAt != null
    var reviewStep by rememberSaveable(containerId) { mutableIntStateOf(selectedStep) }
    var retaking by rememberSaveable(containerId) { mutableStateOf(false) }
    val pendingPath by vm.pendingPhoto(batch.job.id, containerId).collectAsStateWithLifecycle()
    val pendingStep by vm.pendingStep(batch.job.id, containerId).collectAsStateWithLifecycle()
    val pendingPhoto = if (readonly) null else pendingPath?.let(::File)?.takeIf { it.isFile }
    var rename by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }
    var showPhotos by rememberSaveable(containerId) { mutableStateOf(false) }
    val step = if (pendingPhoto != null) pendingStep.takeIf { it in 1..kind.photoCount }
        else if (retaking && reviewStep in completed) reviewStep else nextStep
    val selected = item.photos.find { it.step == reviewStep }
    val showReview = selected != null && !retaking && pendingPhoto == null
    val deferredFill = !readonly && kind == JobKind.DELIVERY && completed.containsAll((1..5).toSet()) && 6 !in completed
    val label = when {
        pendingPhoto != null && step != null -> "Foto $step · ${photoLabel(kind, step)}"
        showReview -> "Foto ${selected!!.step} · ${photoLabel(kind, selected.step)}"
        !readonly && step != null -> "Foto $step / ${kind.photoCount} · ${photoLabel(kind, step)}"
        else -> "Fotodokumentation vollständig"
    }
    CaptureWorkspace(header = {
        CaptureContextHeader("Container ${item.container.containerNumber}", batch.job.draft.referenceNumber,
            "${item.photos.size} / ${kind.photoCount} Fotos",
            if (readonly || busy) null else { { renameValue = item.container.containerNumber; renameError = null; rename = true } })
        Text(label, style = MaterialTheme.typography.labelMedium, color = Lime, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }, media = { modifier ->
        when {
            pendingPhoto != null -> LocalPhoto(pendingPhoto.path, "Vorschau $label", modifier, bounded = true)
            showReview -> LocalPhoto(selected!!.localPath, photoLabel(kind, selected.step), modifier, bounded = true)
            !readonly && step != null -> key(containerId, step, retaking) {
                CameraSurface(scan = false, enabled = !busy,
                    onPhoto = { vm.stagePhoto(batch.job.id, containerId, step, it) }, modifier = modifier)
            }
            else -> Box(modifier.background(Graphite), contentAlignment = Alignment.Center) {
                Text(if (readonly) "Fotos zur Ansicht verfügbar." else "Alle Pflichtfotos sind gespeichert.", Modifier.padding(16.dp))
            }
        }
    }, actions = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                pendingPhoto != null && step != null -> {
                    CaptureAction("Foto verwenden", !busy, primary = true) {
                        vm.savePhoto(batch.job.id, containerId, step, pendingPhoto) { retaking = false; reviewStep = 0 }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CaptureAction("Erneut aufnehmen", !busy, modifier = Modifier.weight(1f)) { vm.discardPhoto(batch.job.id, containerId) }
                        CaptureAction("Verwerfen", !busy, modifier = Modifier.weight(1f)) {
                            vm.discardPhoto(batch.job.id, containerId); retaking = false; reviewStep = 0
                        }
                    }
                }
                showReview -> {
                    if (!readonly) CaptureAction("Dieses Foto erneut aufnehmen", !busy, primary = true) { retaking = true }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!readonly) CaptureAction("Löschen …", !busy, modifier = Modifier.weight(1f)) { deleting = selected!!.step }
                        CaptureAction("Zur Übersicht", !busy, modifier = Modifier.weight(1f)) { reviewStep = 0 }
                    }
                }
                else -> {
                    if (retaking) CaptureAction("Neuaufnahme abbrechen", !busy) { retaking = false; reviewStep = 0 }
                    else if (deferredFill) CaptureAction("Befüllt später · zum Auftrag", !busy, onClick = onDone)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CaptureAction("Fotos (${item.photos.size})", !busy && item.photos.isNotEmpty(), modifier = Modifier.weight(1f)) { showPhotos = true }
                        CaptureAction("Zum Auftrag", !busy, modifier = Modifier.weight(1f), onClick = onDone)
                    }
                }
            }
        }
    })
    if (showPhotos) ModalBottomSheet(onDismissRequest = { showPhotos = false }) {
        Text("Gespeicherte Fotos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(item.photos.sortedBy { it.step }, key = { it.id }) { photo ->
                CaptureAction("${photo.step.toString().padStart(2, '0')} · ${photoLabel(kind, photo.step)}", !busy) {
                    reviewStep = photo.step; retaking = false; showPhotos = false
                }
            }
        }
    }
    if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Container-Nummer bearbeiten") },
        text = { OutlinedTextField(value = renameValue, onValueChange = { renameValue = it; renameError = null }, label = { Text("4 Ziffern") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = renameError != null,
            supportingText = { renameError?.let { Text(it) } }) },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            renameError = containerError(renameValue, batch.containers.filter { it.container.id != containerId }.map { it.container.containerNumber }.toSet())
            if (renameError == null) vm.rename(batch.job.id, containerId, renameValue) { rename = false }
        }) { Text("Speichern") } }, dismissButton = { TextButton(onClick = { rename = false }) { Text("Abbrechen") } })
    deleting?.let { from -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Fotos löschen?") },
        text = { Text("Foto $from und alle folgenden Fotos werden gelöscht. Die Pflichtfolge beginnt danach wieder bei Schritt $from.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { deleting = null; reviewStep = 0; vm.deletePhotos(batch.job.id, containerId, from) }) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Abbrechen") } }) }
}
