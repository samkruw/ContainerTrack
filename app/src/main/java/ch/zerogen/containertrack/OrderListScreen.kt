package ch.zerogen.containertrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OrderListScreen(jobs:List<JobWithContainers>,uploads:List<UploadJobEntity>,photos:List<UploadPhotoEntity>,
    reminders:List<LoggerReminder>,now:Long,online:Boolean,query:String,filter:OrderFilter,
    onQuery:(String)->Unit,onFilter:(OrderFilter)->Unit,onJob:(String)->Unit,
    identity:MicrosoftIdentity?=null,onResume:(String)->Unit=onJob,onRetry:(String)->Unit={}) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val states=uploads.associateBy{it.jobId};val open=reminders.associateBy{it.jobId}
    val visible=jobs.filter{matchesOrder(it,query,filter,states[it.job.id],it.job.id in open)}
    val active=visible.firstOrNull{!it.complete && it.job.exportedAt==null}
    LazyColumn(contentPadding=PaddingValues(16.dp,8.dp,16.dp,90.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
            Text(identity?.displayName?.takeIf{it.isNotBlank()}?.let{"Angemeldet als $it"} ?: "Lokal auf diesem Gerät",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=Muted)
            IconButton(onClick={filtersExpanded=!filtersExpanded}){Icon(Icons.Outlined.Search,"Aufträge filtern",tint=if(filtersExpanded)Lime else Muted)}
            }
            StatusBadge(if(online)"Online" else "Offline",confirmed=online)
        };HorizontalDivider(color=Hairline) }
        item { Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { ScreenTitle("Meine Aufträge",Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE, d. MMMM",Locale.GERMAN))) }
        } }
        if(!online && photos.any{it.uploadedAt==null}) item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.CloudOff,null,tint=Warning,modifier=Modifier.size(18.dp));Text("Offline · ${photos.count{it.uploadedAt==null}} Fotos warten",color=Warning,style=MaterialTheme.typography.bodySmall) } }
        if(filtersExpanded || query.isNotBlank() || filter!=OrderFilter.ACTIVE) {
        item { OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),placeholder={Text("Auftrag oder Container suchen")},label={Text("Aufträge suchen")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,shape=MaterialTheme.shapes.small) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(OrderFilter.ACTIVE to "Aktiv",OrderFilter.NEEDS_FILL to "Befüllt-Foto fehlt",OrderFilter.UPLOAD_ERROR to "Upload-Fehler",OrderFilter.ALL to "Alle").forEach{(value,label)->
                FilterChip(filter==value,{onFilter(value)},label={Text(label)},modifier=Modifier.heightIn(min=48.dp),shape=MaterialTheme.shapes.small)
            }
        } }
        }
        if(active!=null) item {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("AKTIVER AUFTRAG",color=Lime,style=MaterialTheme.typography.labelLarge)
                HorizontalDivider(color=Hairline)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(if(active.job.draft.kind==JobKind.DELIVERY)Icons.Outlined.LocalShipping else Icons.Outlined.Undo,null,tint=Lime)
                    Text(active.job.draft.kind.label.uppercase(Locale.GERMAN),color=Lime,fontWeight=FontWeight.SemiBold)
                }
                Text(orderIdentifier(active),fontSize=if(orderIdentifier(active).length>8)32.sp else 56.sp,lineHeight=if(orderIdentifier(active).length>8)40.sp else 64.sp,fontWeight=FontWeight.ExtraBold,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f)) { Text("Container-Nr.",color=Muted,style=MaterialTheme.typography.bodySmall);Text(active.ordered.firstOrNull{it.photos.size<active.job.draft.kind.photoCount}?.container?.containerNumber ?: "Noch offen",fontSize=40.sp,lineHeight=46.sp,fontWeight=FontWeight.Bold) }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Fortschritt",color=Muted,style=MaterialTheme.typography.bodySmall)
                        PhotoSegments(active.containers.sumOf{it.photos.size},active.job.draft.quantity*active.job.draft.kind.photoCount)
                        Text("${active.containers.sumOf{it.photos.size}}/${active.job.draft.quantity*active.job.draft.kind.photoCount} Fotos",color=Muted)
                    }
                }
                if(active.phase==WorkflowPhase.READY_FOR_FILL) StatusBadge("Befüllt-Foto fehlt")
                open[active.job.id]?.let{ReminderCountdown(it,now)}
                PrimaryIconButton("Auftrag fortsetzen",Icons.Outlined.PlayArrow) { onResume(active.job.id) }
            }
        }
        if(visible.isEmpty()) item { Information(if(jobs.isEmpty())"Noch keine Aufträge. Mit + eine Lieferung oder einen Return anlegen." else "Keine passenden Aufträge. Suche oder Filter ändern.") }
        if(visible.any{it!=active}) item { Text("Letzte Aufträge",Modifier.padding(top=10.dp),style=MaterialTheme.typography.titleLarge);HorizontalDivider(Modifier.padding(top=10.dp),color=Hairline) }
        items(visible.filter{it!=active},key={it.job.id}) { batch ->
            val sync=states[batch.job.id]
            Column(Modifier.fillMaxWidth().clickable{onJob(batch.job.id)},verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Icon(if(batch.job.draft.kind==JobKind.DELIVERY)Icons.Outlined.LocalShipping else Icons.Outlined.Undo,null,tint=Muted,modifier=Modifier.size(26.dp))
                    Column(Modifier.weight(1f)) {
                        Text(batch.job.draft.kind.label.uppercase(Locale.GERMAN),style=MaterialTheme.typography.labelSmall,color=Muted)
                        Text(orderIdentifier(batch),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                        Text("${batch.job.draft.quantity} Container · ${batch.containers.sumOf{it.photos.size}}/${batch.job.draft.quantity*batch.job.draft.kind.photoCount} Fotos",style=MaterialTheme.typography.bodySmall,color=Muted)
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        StatusBadge(if(sync?.state=="SYNCED")"Gesendet" else if(sync?.state=="UPLOAD_ERROR")"Fehler" else if(sync!=null)"Wartend" else if(batch.complete)"Vollständig" else "Entwurf",confirmed=sync?.state=="SYNCED",error=sync?.state=="UPLOAD_ERROR")
                        if(batch.phase==WorkflowPhase.READY_FOR_FILL) Text("Befüllt-Foto fehlt",color=Warning,style=MaterialTheme.typography.labelSmall)
                    }
                    Icon(Icons.Outlined.ChevronRight,null,tint=Muted)
                }
                if(sync?.state=="UPLOADING") Text("Teams · ${syncLabel(sync,online)}",style=MaterialTheme.typography.bodySmall,color=if(sync.state=="UPLOAD_ERROR")MaterialTheme.colorScheme.error else Muted)
                if(sync?.state=="UPLOAD_ERROR") {
                    Text(sync.lastError ?: "Übertragung nicht bestätigt",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={onRetry(batch.job.id)}){Icon(Icons.Outlined.Refresh,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Erneut versuchen")}
                }
                open[batch.job.id]?.let{ReminderCountdown(it,now)}
                HorizontalDivider(color=Hairline)
            }
        }
    }
}
fun orderIdentifier(batch:JobWithContainers)=batch.job.draft.referenceNumber
@Composable
fun PhotoSegments(done:Int,total:Int) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        repeat(total.coerceIn(1,12)){n->Surface(Modifier.weight(1f).height(6.dp),color=if(n.toFloat()/total.coerceAtMost(12)<done.toFloat()/total.coerceAtLeast(1))Lime else Hairline,shape=MaterialTheme.shapes.small){} }
    }
}
@Composable
fun ReminderCountdown(reminder:LoggerReminder,now:Long) {
    val seconds=((reminder.dueAt-now+999)/1000).coerceAtLeast(0)
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.Schedule,null,Modifier.size(18.dp),tint=Warning);Text("Logger · ${if(seconds==0L)"JETZT" else "%02d:%02d".format(seconds/60,seconds%60)}",color=Warning,style=MaterialTheme.typography.labelLarge) }
}
