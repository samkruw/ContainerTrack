package ch.zerogen.containertrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PhotoReviewScreen(batch:JobWithContainers,busy:Boolean,onPhoto:(String,Int)->Unit,onContinue:()->Unit,onBack:()->Unit) {
    var current by rememberSaveable(batch.job.id){mutableStateOf(batch.ordered.firstOrNull()?.container?.id)}
    var choosing by remember{mutableStateOf(false)}
    val item=batch.ordered.find{it.container.id==current} ?: batch.ordered.firstOrNull()
    if(item==null){Information("Noch keine Container erfasst.");return}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(if(batch.job.draft.kind==JobKind.DELIVERY)Icons.Outlined.LocalShipping else Icons.Outlined.Undo,null,tint=Lime)
            Text(batch.job.draft.kind.label.uppercase(),color=Lime,style=MaterialTheme.typography.labelLarge)
            Text(orderIdentifier(batch),style=MaterialTheme.typography.labelLarge)
            Box {
                TextButton(onClick={choosing=true}){Text(item.container.containerNumber);if(batch.containers.size>1)Icon(Icons.Outlined.ExpandMore,null)}
                DropdownMenu(choosing,{choosing=false}){batch.ordered.forEach{c->DropdownMenuItem(text={Text("Container ${c.container.containerNumber}")},onClick={current=c.container.id;choosing=false})}}
            }
        }
        HorizontalDivider(color=Hairline)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(if(item.photos.size==batch.job.draft.kind.photoCount)Icons.Outlined.CheckCircle else Icons.Outlined.PhotoCamera,null,Modifier.size(28.dp),tint=Lime)
            Text("${item.photos.size} von ${batch.job.draft.kind.photoCount} Fotos ${if(item.photos.size==batch.job.draft.kind.photoCount)"vollständig" else "gespeichert"}",style=MaterialTheme.typography.bodyMedium)
        }
        LazyVerticalGrid(GridCells.Fixed(2),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            items(item.photos.sortedBy{it.step},key={it.id}){photo->
                Column(Modifier.clickable(enabled=!busy){onPhoto(item.container.id,photo.step)},verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Surface(shape=MaterialTheme.shapes.small){PhotoImage(photo.localPath,photoName(item.container.containerNumber,photo.step),Modifier.fillMaxWidth().height(176.dp))}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text(photoName(item.container.containerNumber,photo.step),style=MaterialTheme.typography.bodySmall,color=Muted)
                        Icon(Icons.Outlined.CheckCircle,"Foto gespeichert",Modifier.size(20.dp),tint=Lime)
                    }
                }
            }
        }
        Text(if(batch.job.exportedAt==null)"Foto antippen zum Ansehen oder Ersetzen" else "Foto antippen zum Ansehen",style=MaterialTheme.typography.labelSmall,color=Muted)
        PrimaryIconButton("Abschließen & in Teams teilen",Icons.Outlined.CheckCircle,!busy && batch.complete,onContinue)
        OutlineAction("Zurück zur Kamera",Icons.Outlined.ArrowBack,!busy,onBack)
    }
}
