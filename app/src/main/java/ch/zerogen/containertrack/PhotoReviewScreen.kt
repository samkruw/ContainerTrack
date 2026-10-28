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
import androidx.compose.ui.unit.dp

@Composable
fun PhotoReviewScreen(batch:JobWithContainers,busy:Boolean,onPhoto:(String,Int)->Unit,onContinue:()->Unit,onBack:()->Unit) {
    var current by rememberSaveable(batch.job.id){mutableStateOf(batch.ordered.firstOrNull()?.container?.id)}
    var choosing by remember{mutableStateOf(false)}
    val item=batch.ordered.find{it.container.id==current} ?: batch.ordered.firstOrNull()
    if(item==null){Information("Noch keine Container erfasst.");return}
    val photos=namedPhotos(batch).filter{it.photo.containerId==item.container.id}
    val progress=returnProgress(item.photos)
    val complete=containerDocumentationComplete(batch,item)
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(if(batch.job.draft.kind==JobKind.DELIVERY)Icons.Outlined.LocalShipping else Icons.Outlined.Undo,null,tint=Lime)
            Text(batch.job.draft.kind.label.uppercase(),color=Lime,style=MaterialTheme.typography.labelLarge)
            Text(orderIdentifier(batch),Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
            Box {
                TextButton(onClick={choosing=true}){Text(item.container.containerNumber);if(batch.containers.size>1)Icon(Icons.Outlined.ExpandMore,null)}
                DropdownMenu(choosing,{choosing=false}){batch.ordered.forEach{c->DropdownMenuItem(text={Text("Container ${c.container.containerNumber}")},onClick={current=c.container.id;choosing=false})}}
            }
        }
        HorizontalDivider(color=Hairline)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(if(complete)Icons.Outlined.CheckCircle else Icons.Outlined.PhotoCamera,null,Modifier.size(28.dp),tint=Lime)
            Text(if(batch.isManagedReturn)"${progress.initialCount} Anfangsfotos · ${progress.cleanCount}/${progress.initialCount.coerceAtLeast(1)} Leer-/Sauber-Fotos" else "${item.photos.size} von ${batch.job.draft.kind.photoCount} Fotos ${if(complete)"vollständig" else "gespeichert"}",style=MaterialTheme.typography.bodyMedium)
        }
        LazyVerticalGrid(GridCells.Fixed(2),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            val sections = if(batch.isManagedReturn) listOf("Anfangsfotos" to photos.filter{it.photo.returnPhase==ReturnPhotoPhase.INITIAL}, "Leer-/Sauber-Fotos nach der Reinigung" to photos.filter{it.photo.returnPhase==ReturnPhotoPhase.CLEAN}) else listOf("Fotos" to photos)
            sections.forEach { (title, sectionPhotos) ->
                if(batch.isManagedReturn) item(span={GridItemSpan(maxLineSpan)}){Text(title,style=MaterialTheme.typography.titleMedium)}
                if(sectionPhotos.isEmpty() && batch.isManagedReturn) item(span={GridItemSpan(maxLineSpan)}){Text("Noch keine Fotos",color=Muted)}
                items(sectionPhotos,key={it.photo.id}){named->
                    Column(Modifier.clickable(enabled=!busy){onPhoto(item.container.id,named.photo.step)},verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Surface(shape=MaterialTheme.shapes.small){PhotoImage(named.photo.localPath,named.label,Modifier.fillMaxWidth().height(176.dp))}
                        Text(named.label,style=MaterialTheme.typography.labelMedium)
                        Text(named.fileName,style=MaterialTheme.typography.bodySmall,color=Muted)
                    }
                }
            }
        }
        Text(if(batch.job.exportedAt==null && batch.job.emailSentAt==null)"Foto antippen zum Ansehen oder Ersetzen" else "Foto antippen zum Ansehen",style=MaterialTheme.typography.labelSmall,color=Muted)
        PrimaryIconButton("Auftrag per E-Mail",Icons.Outlined.Email,!busy && batch.complete,onContinue)
        if(!batch.complete) Text("Noch unvollständig · ${documentationCountText(batch)}",color=Warning,style=MaterialTheme.typography.bodySmall)
        OutlineAction("Zurück zur Kamera",Icons.Outlined.ArrowBack,!busy,onBack)
    }
}
