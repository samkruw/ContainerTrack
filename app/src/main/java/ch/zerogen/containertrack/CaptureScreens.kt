package ch.zerogen.containertrack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ScanScreen(batch: JobWithContainers, vm: TrackViewModel, busy: Boolean, onAdded: (String) -> Unit) {
    var manual by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var scanned by rememberSaveable { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val existing = batch.containers.map { it.container.containerNumber }.toSet()
    fun accept(number: String) {
        val validation = containerError(number, existing)
        if (validation != null) { error = validation; scanned = number; manual = number }
        else { scanned = number; manual = number; error = null; vm.addContainer(batch.job.id, number, onAdded) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Container scannen", "${batch.job.draft.referenceNumber} · Container ${batch.containers.size + 1} / ${batch.job.draft.quantity}")
        Information("Die vierstellige Nummer vollständig in den Rahmen halten. Der erste gültige Treffer wird sofort übernommen.")
        if (scanned == null) key(attempt) { CameraSurface(scan = true, enabled = !busy, onNumber = ::accept) }
        else {
            Information("Erkannt · $scanned", warning = error != null)
            OutlinedButton(enabled = !busy, onClick = { scanned = null; attempt++; error = null }) { Text("Erneut scannen") }
        }
        HorizontalDivider()
        OutlinedTextField(value = manual, onValueChange = { manual = it; error = null },
            label = { Text("Container-Nummer manuell") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy,
            isError = error != null, supportingText = { Text(error ?: "Exakt 4 Ziffern, z. B. 0708") })
        PrimaryButton("Nummer übernehmen", !busy) { accept(manual) }
    }
}

@Composable
fun LocalPhoto(path: String, description: String, modifier: Modifier = Modifier) {
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
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = description, modifier = modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp), contentScale = ContentScale.Fit)
    else Information("Foto wird geladen. Falls die Datei fehlt, bitte erneut aufnehmen.", warning = true)
}

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
    val step = if (pendingPhoto != null) pendingStep.takeIf { it in 1..kind.photoCount }
        else if (retaking && reviewStep in completed) reviewStep else nextStep
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Container ${item.container.containerNumber}", "${batch.job.draft.referenceNumber} · ${item.photos.size} / ${kind.photoCount} Fotos")
        if (!readonly) TextButton(enabled = !busy, onClick = { renameValue = item.container.containerNumber; renameError = null; rename = true }) { Text("Container-Nummer bearbeiten") }
        GlassSurface {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (item.photos.size == kind.photoCount) "Fotodokumentation vollständig" else "Pflichtfotos in der angegebenen Reihenfolge aufnehmen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.photos.size == kind.photoCount) Success else MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { item.photos.size.toFloat() / kind.photoCount }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (!readonly && kind == JobKind.DELIVERY && completed.containsAll((1..5).toSet()) && 6 !in completed) {
            Information("Leerdokumentation vollständig · Befüllt-Foto kann später ergänzt werden.")
            PrimaryButton("Befüllt später · zum Auftrag", !busy, onDone)
        }
        val selected = item.photos.find { it.step == reviewStep }
        when {
            pendingPhoto != null && step != null -> {
                Text("Foto $step · ${photoLabel(kind, step)}", style = MaterialTheme.typography.titleMedium)
                LocalPhoto(pendingPhoto!!.path, "Vorschau ${photoLabel(kind, step)}")
                PrimaryButton("Foto verwenden", !busy) {
                    val file = pendingPhoto ?: return@PrimaryButton
                    vm.savePhoto(batch.job.id, containerId, step, file) { retaking = false; reviewStep = 0 }
                }
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = { vm.discardPhoto(batch.job.id, containerId) }) { Text("Erneut aufnehmen") }
            }
            selected != null && !retaking -> {
                Text("Foto ${selected.step} · ${photoLabel(kind, selected.step)}", style = MaterialTheme.typography.titleMedium)
                LocalPhoto(selected.localPath, photoLabel(kind, selected.step))
                if (!readonly) {
                    PrimaryButton("Dieses Foto erneut aufnehmen", !busy) { retaking = true }
                    OutlinedButton(enabled = !busy, onClick = { deleting = selected.step }) { Text("Ab diesem Schritt löschen", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = { reviewStep = 0 }) { Text("Zur Übersicht") }
            }
            !readonly && step != null -> {
                Text("Foto $step / ${kind.photoCount}", color = LightBlue, style = MaterialTheme.typography.labelLarge)
                Text(photoLabel(kind, step), style = MaterialTheme.typography.titleLarge)
                key(containerId, step, retaking) {
                    CameraSurface(scan = false, enabled = !busy, onPhoto = { vm.stagePhoto(batch.job.id, containerId, step, it) })
                }
                if (retaking) TextButton(enabled = !busy, onClick = { retaking = false; reviewStep = 0 }) { Text("Neuaufnahme abbrechen") }
            }
            else -> {
                Information(if (readonly) "Auftrag exportiert. Fotos sind zur Ansicht verfügbar." else "Alle Pflichtfotos sind gespeichert.")
                PrimaryButton("Zum Auftrag", !busy, onDone)
            }
        }
        if (item.photos.isNotEmpty() && pendingPhoto == null && !retaking) {
            Text("Gespeicherte Fotos", style = MaterialTheme.typography.titleMedium)
            item.photos.sortedBy { it.step }.forEach { photo ->
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = { reviewStep = photo.step }) {
                    Text("${photo.step.toString().padStart(2, '0')} · ${photoLabel(kind, photo.step)}")
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
