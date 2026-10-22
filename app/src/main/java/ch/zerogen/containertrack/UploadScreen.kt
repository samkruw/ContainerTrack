package ch.zerogen.containertrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun UploadScreen(batch:JobWithContainers,queue:UploadJobEntity?,uploads:List<UploadPhotoEntity>,identity:MicrosoftIdentity?,
                 configured:Boolean,online:Boolean,busy:Boolean,onUpload:()->Unit,onShare:()->Unit,onOrders:()->Unit,onProfile:()->Unit) {
    val photos=batch.ordered.flatMap{c->c.photos.sortedBy{it.step}.map{c.container.containerNumber to it}}
    val acknowledgements=uploads.filter{it.jobId==batch.job.id}.associateBy{it.photoId}
    val sent=photos.count{acknowledgements[it.second.id]?.uploadedAt!=null}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Groups,null,tint=Lime);Text("Teams-Übertragung",color=Lime,style=MaterialTheme.typography.labelLarge)}
            HorizontalDivider(Modifier.padding(top=12.dp),color=Hairline)
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(if(batch.complete)Icons.Outlined.CheckCircle else Icons.Outlined.PhotoCamera,null,Modifier.size(48.dp),tint=if(batch.complete)Lime else Warning)
                Column {Text("Auftrag ${orderIdentifier(batch)}",color=Muted,style=MaterialTheme.typography.bodyMedium);Text(if(batch.containers.size==1)"Container ${batch.ordered.first().container.containerNumber}" else "${batch.containers.size} Container",fontSize=28.sp,fontWeight=FontWeight.Bold)}
            }
        }
        item {GlassSurface {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups,null,Modifier.size(38.dp),tint=androidx.compose.ui.graphics.Color(0xFF8B83ED))
                Column {Text("Zielkanal",color=Muted,style=MaterialTheme.typography.bodySmall);Text("Op530-Lal",style=MaterialTheme.typography.titleLarge)}
                Column {Text("Angemeldet als",color=Muted,style=MaterialTheme.typography.bodySmall);Text(identity?.displayName ?: "Nicht verbunden",style=MaterialTheme.typography.bodySmall)}
            }
            HorizontalDivider(color=Hairline)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Outlined.Sync,null,tint=Lime);Text("Automatisch nach Abschluss",style=MaterialTheme.typography.bodyMedium)}
            Text(if(queue?.ownerAccountId==null)"Für diesen Auftrag ist noch kein Firmenkonto zugeordnet." else syncLabel(queue,online),color=Muted,style=MaterialTheme.typography.bodySmall)
        }}}
        item {Text("${photos.size} nummerierte JPG-Fotos",color=Muted);HorizontalDivider(Modifier.padding(top=10.dp),color=Hairline)}
        items(photos,key={it.second.id}){(number,photo)->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PhotoImage(photo.localPath,photoName(number,photo.step),Modifier.size(54.dp))
                Column(Modifier.weight(1f)){Text(photoName(number,photo.step),fontWeight=FontWeight.SemiBold);Text("%.1f MB".format(File(photo.localPath).length()/1_000_000f),style=MaterialTheme.typography.bodySmall,color=Muted)}
                val confirmed=acknowledgements[photo.id]?.uploadedAt!=null
                Icon(if(confirmed)Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,if(confirmed)"Übertragung bestätigt" else "Noch nicht übertragen",tint=if(confirmed)Lime else Muted,modifier=Modifier.size(24.dp))
            }
            HorizontalDivider(Modifier.padding(top=10.dp),color=Hairline)
        }
        item {
            if(queue?.state=="SYNCED" && sent==photos.size && photos.isNotEmpty()) Text("Alle $sent Fotos erfolgreich gesendet",color=Lime,style=MaterialTheme.typography.titleMedium)
            else Text("$sent von ${photos.size} Fotos bestätigt",color=Muted)
            queue?.lastError?.let{Information(it,warning=true)}
            if(!configured || identity==null) OutlineAction("Mit Firmenkonto verbinden",Icons.Outlined.AccountCircle,!busy,onProfile)
            else PrimaryIconButton(if(queue?.state=="UPLOAD_ERROR")"Erneut versuchen" else "An Teams übertragen",Icons.Outlined.CloudUpload,!busy && batch.complete,onUpload)
        }
        item {OutlineAction("Über Android teilen",Icons.Outlined.Share,!busy && photos.isNotEmpty(),onShare)}
        item {PrimaryIconButton("Zur Auftragsübersicht",Icons.Outlined.FormatListBulleted,!busy,onOrders)}
    }
}
