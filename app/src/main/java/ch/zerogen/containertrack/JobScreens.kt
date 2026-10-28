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
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            GlassSurface(onClick = { onBegin(kind) }) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(kind.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (kind == JobKind.DELIVERY) "Container bei Anlieferung erfassen und dokumentieren." else "Container bei Rückgabe erfassen und dokumentieren.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (kind == JobKind.DELIVERY) "6 Pflichtfotos je Container" else "Logger stoppen · Anfangs- und Leer-/Sauber-Fotos", style = MaterialTheme.typography.labelMedium, color = LightBlue)
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
    GlassSurface(onClick = onClick) {
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
    val candidates = jobs.filter { !it.complete && it.job.exportedAt == null && it.job.emailSentAt == null }.sortedByDescending { it.job.id == active }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Kamera", "Auftrag für die nächste Erfassung wählen") }
        if (candidates.isEmpty()) item { Information("Kein offener Auftrag zum Scannen. Unter Aufträge mit + einen neuen Auftrag anlegen.") }
        items(candidates, key = { it.job.id }) { JobRow(it) { onJob(it) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(vm:TrackViewModel,busy:Boolean,onCreated:(String)->Unit) {
    val kindName by vm.formKind.collectAsStateWithLifecycle()
    val reference by vm.formReference.collectAsStateWithLifecycle()
    val order by vm.formOrder.collectAsStateWithLifecycle()
    val quantity by vm.formQuantity.collectAsStateWithLifecycle()
    val quick by vm.formQuick.collectAsStateWithLifecycle()
    val work by vm.formWork.collectAsStateWithLifecycle()
    val number by vm.formContainer.collectAsStateWithLifecycle()
    val stopped by vm.formStoppedAt.collectAsStateWithLifecycle()
    val kind=JobKind.valueOf(kindName)
    val locked=kind==JobKind.RETURN && stopped==null
    var attempted by rememberSaveable{mutableStateOf(false)}
    var details by rememberSaveable(kindName){mutableStateOf(kind==JobKind.RETURN)}
    var scanning by remember{mutableStateOf(false)}
    val draft=JobDraft(kind=kind,referenceNumber=reference,orderValue=order,quantity=quantity.toIntOrNull() ?: 0,quick=quick,additionalWork=work)
    val errors=if(attempted)draft.errors(stopped) else emptyMap()
    val numberError=if(number.isNotBlank())containerError(number,emptySet()) else null
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Neuer Auftrag")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            JobKind.entries.forEachIndexed{index,value->SegmentedButton(kind==value,{if(!busy && kind!=value)vm.begin(value)},if(index==0)androidx.compose.foundation.shape.RoundedCornerShape(topStart=5.dp,bottomStart=5.dp) else androidx.compose.foundation.shape.RoundedCornerShape(topEnd=5.dp,bottomEnd=5.dp),colors=SegmentedButtonDefaults.colors(activeContainerColor=Lime,activeContentColor=Navy,inactiveContainerColor=Navy,inactiveContentColor=Muted),modifier=Modifier.heightIn(min=52.dp),icon={Icon(if(value==JobKind.DELIVERY)Icons.Outlined.LocalShipping else Icons.Outlined.Undo,null)}){Text(value.label,fontWeight=FontWeight.Bold)}}
        }
        if(kind==JobKind.RETURN) {
            if(locked){Information("Zuerst den Logger stoppen. Erst dann sind die Formularfelder freigegeben.",true);PrimaryButton("Logger gestoppt – bestätigen",!busy){vm.confirmStopped()}}
            else Information("Logger gestoppt · ${localTime(stopped!!)}")
        }
        FormField("Order Number",reference,!locked && !busy,errors["referenceNumber"]){vm.field("formReference",it)}
        FormField("Container-Nr.",number,!locked && !busy,if(attempted)numberError else null){if(it.length<=4 && it.all{c->c in '0'..'9'})vm.field("formContainer",it)}
        OutlineAction("Nummer scannen",Icons.Outlined.PhotoCamera,!locked && !busy){scanning=true}
        TextButton(onClick={details=!details},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Auftragsdaten · Pflichtangaben");Spacer(Modifier.weight(1f));Icon(if(details)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)}
        if(details) {
            FormField("Order / Container-Typ",order,!locked && !busy,errors["orderValue"]){vm.field("formOrder",it)}
            OutlinedTextField(quantity,{if(it.length<=2 && it.all{c->c in '0'..'9'})vm.field("formQuantity",it)},Modifier.fillMaxWidth(),enabled=!locked && !busy,label={Text("Quantity · 1 bis 50")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError=errors["quantity"]!=null,supportingText={errors["quantity"]?.let{Text(it)}})
            if(kind==JobKind.RETURN) {
                FormField("Quick",quick,!locked && !busy,errors["quick"],true){vm.field("formQuick",it)}
                FormField("additional Work",work,!locked && !busy,errors["additionalWork"],true){vm.field("formWork",it)}
                Text("Nothing und Empty sind gültige Angaben.",color=Muted,style=MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider(color=Hairline)
        Text("Fotodokumentation",style=MaterialTheme.typography.titleLarge)
        GlassSurface {Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            NumberDisc(if(kind==JobKind.DELIVERY)"5" else "1")
            Column {Text(if(kind==JobKind.DELIVERY)"Leerdokumentation" else "Inhaltsfoto",fontWeight=FontWeight.SemiBold);Text(if(kind==JobKind.DELIVERY)"Leer · Front · Türen · Links · Rechts" else "Pflicht: Inhalt · optional: Schaden und Sonstiges",color=Muted,style=MaterialTheme.typography.bodySmall)}
        }}
        if(kind==JobKind.DELIVERY) GlassSurface {Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            NumberDisc("1");Column {Text("Befüllt-Foto",fontWeight=FontWeight.SemiBold);Text("Kann später ergänzt werden",color=Muted,style=MaterialTheme.typography.bodySmall)}
        }}
        if(kind==JobKind.RETURN) GlassSurface { Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            NumberDisc("=");Column {Text("Leer-/Sauber-Fotos nach der Reinigung",fontWeight=FontWeight.SemiBold);Text("Pro Container gleich viele Leer-/Sauber-Fotos wie Anfangsfotos. Nach 24 Stunden erinnert die App bis „E-Mail gesendet“ bestätigt ist.",color=Muted,style=MaterialTheme.typography.bodySmall)}
        }}
        Spacer(Modifier.height(8.dp))
    }
        Spacer(Modifier.height(10.dp))
        PrimaryIconButton("Fotos starten",Icons.Outlined.PhotoCamera,!locked && !busy) {
            attempted=true
            if(draft.errors(stopped).isEmpty() && numberError==null)vm.create(onCreated) else details=true
        }
        Spacer(Modifier.height(12.dp))
    }
    if(scanning) androidx.compose.ui.window.Dialog(onDismissRequest={scanning=false}) {
        Surface(shape=MaterialTheme.shapes.medium){Column(Modifier.padding(12.dp)) {
            Text("Container-Nummer scannen",style=MaterialTheme.typography.titleMedium)
            CameraSurface(scan=true,enabled=!busy,onNumber={vm.field("formContainer",it);scanning=false})
            TextButton(onClick={scanning=false}){Text("Manuell eingeben")}
        }}
    }
}

@Composable
fun FormField(label: String, value: String, enabled: Boolean, error: String?, multiline: Boolean = false, onChange: (String) -> Unit) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), keyboardOptions=KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Done),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={focus.clearFocus()}), textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize=if(label=="Order Number" || label=="Return Number" || label=="Container-Nr.")52.sp else 18.sp,fontWeight=if(label=="Order Number" || label=="Return Number" || label=="Container-Nr.")FontWeight.ExtraBold else FontWeight.Normal,lineHeight=if(label=="Order Number" || label=="Return Number" || label=="Container-Nr.")60.sp else 24.sp), singleLine = !multiline, minLines = 1, maxLines = if (multiline) 4 else 1,
        isError = error != null, supportingText = if (error != null) ({ Text(error) }) else null)
}

@Composable
fun JobScreen(batch: JobWithContainers, vm: TrackViewModel, busy: Boolean, onScan: () -> Unit,
              onContainer: (String) -> Unit, onExport: () -> Unit, onDeleted: () -> Unit,
              onPhoto: (String,Int) -> Unit = { id, _ -> onContainer(id) }, onUpload: (() -> Unit)? = null, onReview: (() -> Unit)? = null) {
    var deleting by remember { mutableStateOf(false) }
    val job = batch.job
    var expanded by rememberSaveable(job.id) { mutableStateOf<String?>(batch.ordered.firstOrNull()?.container?.id) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){StatusBadge(job.draft.kind.label.uppercase());StatusBadge(batch.status,confirmed=batch.complete)}
            ScreenTitle(orderIdentifier(batch))
        }
        item { Information("${job.draft.orderValue}\n${batch.completedCount} / ${job.draft.quantity} Container vollständig\n${if (job.draft.kind == JobKind.DELIVERY) "Lieferung" else "Logger gestoppt · ${job.loggerStoppedAt?.let(::localTime) ?: "–"}"}") }
        items(batch.ordered, key = { it.container.id }) { item ->
            ContainerDetails(item,job.draft.kind,job.exportedAt != null || job.emailSentAt != null,expanded == item.container.id,
                onExpand = { expanded = if (expanded == item.container.id) null else item.container.id },
                onPhoto = { onPhoto(item.container.id,it) },busy = busy, managedReturn = batch.isManagedReturn, names = namedPhotos(batch).filter { it.photo.containerId == item.container.id })
        }
        val unfinished = batch.ordered.firstOrNull { !containerDocumentationComplete(batch,it) }
        val canAdd = batch.containers.size < job.draft.quantity &&
            if(batch.isManagedReturn) batch.containers.all { returnProgress(it.photos).hasContent } else
            canAddContainer(job.draft.kind, batch.containers.map { it.photos.map { photo -> photo.step }.toSet() })
        if (job.exportedAt == null && job.emailSentAt == null && canAdd) item {
            PrimaryButton("Container ${batch.containers.size + 1} / ${job.draft.quantity} erfassen", !busy, onScan)
        }
        if (job.exportedAt == null && job.emailSentAt == null && unfinished != null) item {
            PrimaryButton("Container ${unfinished.container.containerNumber} fortsetzen", !busy) { onContainer(unfinished.container.id) }
        }
        if (onReview!=null && batch.containers.any{it.photos.isNotEmpty()}) item { PrimaryButton("Fotos prüfen",!busy,onReview) }
        if (batch.complete && onUpload!=null) item { PrimaryButton("Auftrag per E-Mail", !busy, onUpload) }
        if (batch.complete) item { PrimaryButton(if (job.exportedAt == null) "Export prüfen" else "Erneut exportieren", !busy, onExport) }
        if (job.draft.kind == JobKind.RETURN) item {
            Text("Quick · ${job.draft.quick}\nadditional Work · ${job.draft.additionalWork}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { OutlinedButton(enabled = !busy, onClick = { deleting = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Auftrag löschen", color = MaterialTheme.colorScheme.error) } }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Auftrag löschen?") },
        text = { Text("${job.draft.referenceNumber}: Alle lokalen Daten, Fotos und Erinnerungen werden unwiderruflich gelöscht. Bereits exportierte Dateien bleiben am gewählten Speicherort.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { deleting = false; vm.deleteJob(job.id, onDeleted) }) { Text("Endgültig löschen") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Abbrechen") } })
}

fun containerDocumentationComplete(batch: JobWithContainers, item: ContainerWithPhotos): Boolean =
    if (batch.isManagedReturn) returnProgress(item.photos).complete
    else item.photos.map { it.step }.sorted() == (1..batch.job.draft.kind.photoCount).toList()

fun documentationTarget(batch: JobWithContainers): Int = if (batch.isManagedReturn) {
    batch.containers.sumOf { returnProgress(it.photos).initialCount.coerceAtLeast(1) * 2 } +
        (batch.job.draft.quantity - batch.containers.size).coerceAtLeast(0) * 2
} else batch.job.draft.quantity * batch.job.draft.kind.photoCount

fun documentationCountText(batch: JobWithContainers): String =
    "${batch.containers.sumOf { it.photos.size }}/${documentationTarget(batch)} Fotos"

fun documentationStatus(batch: JobWithContainers): String = when {
    batch.job.emailSentAt != null -> "E-Mail bestätigt"
    batch.complete -> "E-Mail offen"
    batch.isManagedReturn && batch.initialDocumentationComplete -> "Leer-/Sauber-Fotos fehlen"
    batch.phase == WorkflowPhase.READY_FOR_FILL -> "Befüllt-Foto fehlt"
    else -> "In Bearbeitung"
}
