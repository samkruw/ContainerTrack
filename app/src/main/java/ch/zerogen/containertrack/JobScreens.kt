package ch.zerogen.containertrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun localTime(at: Long) = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm"))

@Composable
fun HomeScreen(jobs: List<JobWithContainers>, onBegin: (JobKind) -> Unit, onJob: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Erfassung", "Lokal auf diesem Gerät") }
        items(JobKind.entries) { kind ->
            Surface(onClick = { onBegin(kind) }, color = Graphite, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(kind.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (kind == JobKind.DELIVERY) "Container bei Anlieferung erfassen und dokumentieren." else "Container bei Rückgabe erfassen und dokumentieren.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (kind == JobKind.DELIVERY) "6 Pflichtfotos je Container" else "Logger stoppen · 1 Pflichtfoto je Container", style = MaterialTheme.typography.labelMedium, color = LightBlue)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LightBlue)
                }
            }
        }
        item { Text("Letzte lokale Vorgänge", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium) }
        if (jobs.isEmpty()) item { Text("Noch keine Aufträge gespeichert.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(jobs.take(5), key = { it.job.id }) { JobRow(it) { onJob(it.job.id) } }
    }
}

@Composable
fun JobRow(batch: JobWithContainers, onClick: () -> Unit) {
    Surface(color = Graphite, shape = MaterialTheme.shapes.medium, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(batch.job.draft.referenceNumber, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(batch.status, color = if (batch.complete) Success else LightBlue, style = MaterialTheme.typography.labelMedium)
            }
            Text("${batch.job.draft.kind.label} · ${batch.completedCount} / ${batch.job.draft.quantity} Container", style = MaterialTheme.typography.bodyMedium)
            Text(localTime(batch.job.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun JobsScreen(jobs: List<JobWithContainers>, onJob: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Aufträge", "Fortsetzen, prüfen und bewusst löschen") }
        if (jobs.isEmpty()) item { Information("Noch keine lokalen Aufträge. Unter Start eine Lieferung oder einen Return anlegen.") }
        items(jobs, key = { it.job.id }) { JobRow(it) { onJob(it.job.id) } }
    }
}

@Composable
fun ScanJobsScreen(jobs: List<JobWithContainers>, active: String?, onJob: (JobWithContainers) -> Unit) {
    val candidates = jobs.filter { !it.complete && it.job.exportedAt == null }.sortedByDescending { it.job.id == active }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Scannen", "Auftrag für die nächste Erfassung wählen") }
        if (candidates.isEmpty()) item { Information("Kein offener Auftrag zum Scannen. Unter Start einen neuen Auftrag anlegen.") }
        items(candidates, key = { it.job.id }) { JobRow(it) { onJob(it) } }
    }
}

@Composable
fun SetupScreen(vm: TrackViewModel, busy: Boolean, onCreated: (String) -> Unit) {
    val kindName by vm.formKind.collectAsStateWithLifecycle()
    val reference by vm.formReference.collectAsStateWithLifecycle()
    val kt by vm.formKt.collectAsStateWithLifecycle()
    val order by vm.formOrder.collectAsStateWithLifecycle()
    val quantity by vm.formQuantity.collectAsStateWithLifecycle()
    val quick by vm.formQuick.collectAsStateWithLifecycle()
    val work by vm.formWork.collectAsStateWithLifecycle()
    val stopped by vm.formStoppedAt.collectAsStateWithLifecycle()
    val kind = JobKind.valueOf(kindName)
    val locked = kind == JobKind.RETURN && stopped == null
    var attempted by rememberSaveable { mutableStateOf(false) }
    val draft = JobDraft(kind, reference, kt, order, quantity.toIntOrNull() ?: 0, quick, work)
    val errors = if (attempted) draft.errors(stopped) else emptyMap()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("${kind.label} anlegen", "Alle Felder sind Pflichtfelder.")
        if (kind == JobKind.RETURN) {
            if (locked) {
                Information("Zuerst den Logger stoppen. Das Formular wird erst nach Ihrer Bestätigung freigegeben.", warning = true)
                PrimaryButton("Logger gestoppt – bestätigen", enabled = !busy) { vm.confirmStopped() }
            } else Information("Logger gestoppt · ${localTime(stopped!!)}")
        }
        FormField(if (kind == JobKind.DELIVERY) "Order Number" else "Return Number", reference, !locked && !busy, errors["referenceNumber"]) { vm.field("formReference", it) }
        if (kind == JobKind.DELIVERY) FormField("ZRH Number", kt, !busy, errors["kt"]) { vm.field("formKt", it) }
        FormField("Order / Container-Typ", order, !locked && !busy, errors["orderValue"]) { vm.field("formOrder", it) }
        OutlinedTextField(value = quantity, onValueChange = { if (it.length <= 2 && it.all { c -> c in '0'..'9' }) vm.field("formQuantity", it) },
            label = { Text("Quantity · 1 bis 50") }, enabled = !locked && !busy, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = errors["quantity"] != null,
            supportingText = { errors["quantity"]?.let { Text(it) } })
        FormField("Quick", quick, !locked && !busy, errors["quick"], multiline = true) { vm.field("formQuick", it) }
        FormField("additional Work", work, !locked && !busy, errors["additionalWork"], multiline = true) { vm.field("formWork", it) }
        Text("Nothing und Empty sind gültige Angaben.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton("Auftrag speichern", enabled = !locked && !busy) {
            attempted = true
            if (draft.errors(stopped).isEmpty()) vm.create(onCreated)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun FormField(label: String, value: String, enabled: Boolean, error: String?, multiline: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), singleLine = !multiline, minLines = 1, maxLines = if (multiline) 4 else 1,
        isError = error != null, supportingText = if (error != null) ({ Text(error) }) else null)
}

@Composable
fun JobScreen(batch: JobWithContainers, vm: TrackViewModel, busy: Boolean, onScan: () -> Unit,
              onContainer: (String) -> Unit, onExport: () -> Unit, onDeleted: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    val job = batch.job
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle(job.draft.referenceNumber, "${job.draft.kind.label} · ${batch.status}") }
        item { Information("${job.draft.orderValue}\n${batch.completedCount} / ${job.draft.quantity} Container vollständig\n${if (job.draft.kind == JobKind.DELIVERY) "ZRH · ${job.draft.kt}" else "Logger gestoppt · ${job.loggerStoppedAt?.let(::localTime) ?: "–"}"}") }
        items(batch.ordered, key = { it.container.id }) { item ->
            Surface(onClick = { if (!busy) onContainer(item.container.id) }, color = Graphite, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${item.container.sequenceIndex.toString().padStart(2, '0')} · ${item.container.containerNumber}", style = MaterialTheme.typography.titleMedium)
                        Text("${item.photos.size} / ${job.draft.kind.photoCount} Fotos · ${if (item.container.completedAt != null) "Vollständig" else "Fortsetzen"}", color = if (item.container.completedAt != null) Success else LightBlue)
                    }
                    Icon(Icons.Default.ArrowForward, "Container öffnen")
                }
            }
        }
        val unfinished = batch.ordered.firstOrNull { it.container.completedAt == null }
        if (job.exportedAt == null && unfinished != null) item {
            PrimaryButton("Container ${unfinished.container.containerNumber} fortsetzen", !busy) { onContainer(unfinished.container.id) }
        } else if (job.exportedAt == null && batch.containers.size < job.draft.quantity) item {
            PrimaryButton("Container ${batch.containers.size + 1} / ${job.draft.quantity} erfassen", !busy, onScan)
        }
        if (batch.complete) item { PrimaryButton(if (job.exportedAt == null) "Export prüfen" else "Erneut exportieren", !busy, onExport) }
        item {
            Text("Quick · ${job.draft.quick}\nadditional Work · ${job.draft.additionalWork}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { OutlinedButton(enabled = !busy, onClick = { deleting = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Auftrag löschen", color = MaterialTheme.colorScheme.error) } }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Auftrag löschen?") },
        text = { Text("${job.draft.referenceNumber}: Alle lokalen Daten, Fotos und Logger-Erinnerungen werden unwiderruflich gelöscht. Bereits exportierte Dateien bleiben am gewählten Speicherort.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { deleting = false; vm.deleteJob(job.id, onDeleted) }) { Text("Endgültig löschen") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Abbrechen") } })
}
