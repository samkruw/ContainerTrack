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
fun UploadScreen(batch:JobWithContainers,busy:Boolean,onShare:()->Unit,onOrders:()->Unit,onTeamsShare:()->Unit) {
    val photos=batch.ordered.flatMap{c->c.photos.sortedBy{it.step}.map{c.container.containerNumber to it}}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Groups,null,tint=Lime);Text("Teams-Übertragung",color=Lime,style=MaterialTheme.typography.labelLarge)}
            HorizontalDivider(Modifier.padding(top=12.dp),color=Hairline)
        }
        item {
            Text("Auftrag ${orderIdentifier(batch)}",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Text(if(batch.containers.size==1)"Container ${batch.ordered.first().container.containerNumber}" else "${batch.containers.size} Container",fontSize=36.sp,lineHeight=42.sp,fontWeight=FontWeight.ExtraBold)
        }
        item {
            val info=photoShareText(batch.job.draft,photos.map{it.first},photos.size)
            Text(info,color=Muted,style=MaterialTheme.typography.bodySmall)
            PhotoShareInfoAction(info,!busy && photos.isNotEmpty())
        }
        item {
            PrimaryIconButton("In Teams teilen",Icons.Outlined.Groups,!busy && photos.isNotEmpty(),onTeamsShare)
            Text("Teams bestimmt die verfügbaren Empfänger; über Teilen sind je nach Version nur Chats verfügbar. Ohne Teams-App öffnet sich Android-Teilen.",color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
        item {Information("Teams kann den Begleittext bei Fotos weglassen. Kopiere dann die Auftragsinfos und füge sie in Teams ein. Für einen Kanal lade die Fotos zuerst in der Galerie herunter und hänge sie direkt in Teams an; füge die kopierten Infos hinzu. Das Öffnen von Teams bestätigt keinen Versand.")}
        item {Text("${photos.size} nummerierte Original-JPGs",color=Muted);HorizontalDivider(Modifier.padding(top=10.dp),color=Hairline)}
        items(photos,key={it.second.id}){(number,photo)->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PhotoImage(photo.localPath,photoName(number,photo.step),Modifier.size(54.dp))
                Column(Modifier.weight(1f)){Text(photoName(number,photo.step),fontWeight=FontWeight.SemiBold);Text("%.1f MB".format(File(photo.localPath).length()/1_000_000f),style=MaterialTheme.typography.bodySmall,color=Muted)}
                Icon(Icons.Outlined.CheckCircle,"Foto lokal gespeichert",tint=Lime,modifier=Modifier.size(24.dp))
            }
            HorizontalDivider(Modifier.padding(top=10.dp),color=Hairline)
        }
        item {OutlineAction("Über Android teilen",Icons.Outlined.Share,!busy && photos.isNotEmpty(),onShare)}
        item {PrimaryIconButton("Zur Auftragsübersicht",Icons.Outlined.FormatListBulleted,!busy,onOrders)}
    }
}
