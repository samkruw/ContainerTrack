package ch.zerogen.containertrack

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OrderListScreen(jobs: List<JobWithContainers>, uploads: List<UploadJobEntity>, photos: List<UploadPhotoEntity>,
                    reminders: List<LoggerReminder>, now: Long, online: Boolean, query: String, filter: OrderFilter,
                    onQuery: (String) -> Unit, onFilter: (OrderFilter) -> Unit, onJob: (String) -> Unit) {
    val states = uploads.associateBy { it.jobId }
    val open = reminders.associateBy { it.jobId }
    val visible = jobs.filter { matchesOrder(it,query,filter,states[it.job.id],it.job.id in open) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp,end = 16.dp,top = 12.dp,bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("Aufträge","Container dokumentieren und fortsetzen") }
        if (!online) item {
            val waiting = photos.count { it.uploadedAt == null }
            Information("Offline · $waiting Fotos ausstehend. Aufnahme und Logger funktionieren weiter.",warning = true)
        }
        item { OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),label = { Text("Aufträge suchen") },
            placeholder = { Text("KT / Order / Return") },singleLine = true) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OrderFilter.ACTIVE to "Aktiv",OrderFilter.NEEDS_FILL to "Befüllt-Foto fehlt",OrderFilter.UPLOAD_ERROR to "Upload-Fehler",OrderFilter.ALL to "Alle").forEach { (value,label) ->
                    FilterChip(filter == value,{ onFilter(value) },label = { Text(label) },modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        if (visible.isEmpty()) item { Information(if (jobs.isEmpty()) "Noch keine Aufträge. Mit + eine Lieferung oder einen Return anlegen." else "Keine passenden Aufträge. Suche oder Filter ändern.") }
        items(visible,key = { it.job.id }) { batch ->
            val sync = states[batch.job.id]
            val count = batch.containers.sumOf { it.photos.size }
            val total = batch.job.draft.quantity * batch.job.draft.kind.photoCount
            GlassSurface(onClick = { onJob(batch.job.id) }) {
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(batch.job.draft.kt.ifBlank { batch.job.draft.referenceNumber },style = MaterialTheme.typography.titleMedium,fontWeight = FontWeight.Bold,fontFamily = FontFamily.Monospace)
                    if (batch.job.draft.kt != batch.job.draft.referenceNumber) Text(batch.job.draft.referenceNumber,style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${batch.job.draft.kind.label} · ${batch.job.draft.quantity} Container · $count/$total Fotos",style = MaterialTheme.typography.bodyMedium)
                    Text(when (batch.phase) { WorkflowPhase.READY_FOR_FILL -> "Bereit zum Befüllen";WorkflowPhase.COMPLETE -> "Vollständig";else -> "In Bearbeitung" },
                        color = if (batch.complete) Success else Warning,style = MaterialTheme.typography.labelLarge)
                    if (sync != null) Text("Teams · ${syncLabel(sync,online)}",style = MaterialTheme.typography.bodySmall,
                        color = if (sync.state == "UPLOAD_ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sync?.state == "UPLOADING") Text("${photos.count { it.jobId == batch.job.id && it.uploadedAt != null }}/$total Dateien bestätigt",style = MaterialTheme.typography.labelSmall)
                    open[batch.job.id]?.let { reminder ->
                        val seconds = ((reminder.dueAt-now+999)/1000).coerceAtLeast(0)
                        Text("Logger · ${if (seconds == 0L) "JETZT" else "%02d:%02d".format(seconds/60,seconds%60)}",fontFamily = FontFamily.Monospace,color = Warning,style = MaterialTheme.typography.labelLarge)
                    }
                    LinearProgressIndicator(progress = { if (total > 0) (count.toFloat()/total).coerceIn(0f,1f) else 0f },modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
