package ch.zerogen.containertrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun GalleryScreen(jobs:List<JobWithContainers>,uploads:List<UploadPhotoEntity>,onShare:(GalleryPhoto)->Unit,
                  onSaveSelected:(Set<String>)->Unit={}, busy:Boolean=false) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    var query by rememberSaveable{mutableStateOf("")}
    var opened by rememberSaveable{mutableStateOf<String?>(null)}
    var fullId by rememberSaveable{mutableStateOf<String?>(null)}
    var selected by rememberSaveable{mutableStateOf(listOf<String>())}
    val rows=remember(jobs,uploads){galleryPhotos(jobs,uploads)}
    val local by produceState(emptyList<GalleryPhoto>(),rows){value=withContext(Dispatchers.IO){rows.filter{File(it.localPath).isFile}}}
    val groups=local.filter{it.matches(query,"",null)||it.containerNumber.contains(query.trim())}.groupBy{"${it.jobId}|${it.containerNumber}"}
    val group=local.filter{"${it.jobId}|${it.containerNumber}"==opened}
    val chosen=selectedGalleryPhotos(local,selected.toSet())
    val selectedJob = chosen.map { it.jobId }.distinct().singleOrNull()?.let { id -> jobs.find { it.job.id == id } }
    fun toggle(id:String){selected=if(id in selected)selected-id else selected+id}
    BackHandler(opened!=null){opened=null}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(opened==null) {
            Text("AUF DIESEM GERÄT",Modifier.padding(top=18.dp),color=Lime,style=MaterialTheme.typography.labelMedium)
            ScreenTitle("Galerie")
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Auftrag oder Container suchen")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Done),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={focus.clearFocus()}))
            Text("${local.size} lokale Fotos",color=Muted,style=MaterialTheme.typography.bodySmall)
            HorizontalDivider(color=Hairline)
            Text("Zuletzt aufgenommen",style=MaterialTheme.typography.bodyLarge)
            if(groups.isEmpty()) Information("Keine passenden lokalen Fotos vorhanden.")
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(groups.entries.toList(),key={it.key}){(key,photos)->
                    val first=photos.first();val batch=jobs.find{it.job.id==first.jobId}
                    Column {
                        Row(Modifier.fillMaxWidth().clickable{opened=key}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Checkbox(photos.all{it.photoId in selected},{checked->selected=if(checked)(selected+photos.map{it.photoId}).distinct() else selected-photos.map{it.photoId}.toSet()},Modifier.size(28.dp))
                            Surface(shape=MaterialTheme.shapes.small){PhotoImage(first.localPath,"Container ${first.containerNumber}",Modifier.width(112.dp).height(108.dp))}
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text("${batch?.let(::orderIdentifier) ?: first.reference} · ${first.containerNumber}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                                Text("${photos.size} Originalfotos",color=Muted,style=MaterialTheme.typography.bodySmall)
                                StatusBadge(if(batch?.job?.emailSentAt!=null)"E-Mail bestätigt" else "Lokal",confirmed=batch?.job?.emailSentAt!=null)
                            }
                            Icon(Icons.Outlined.ChevronRight,null,tint=Muted)
                        }
                        HorizontalDivider(Modifier.padding(top=10.dp),color=Hairline)
                    }
                }
            }
        } else {
            val first=group.firstOrNull();val batch=jobs.find{it.job.id==first?.jobId}
            Row(Modifier.padding(top=6.dp),verticalAlignment=Alignment.CenterVertically) {IconButton(onClick={opened=null}){Icon(Icons.Outlined.ArrowBack,"Zur Galerie")};Text("Auftragsdetails",style=MaterialTheme.typography.titleLarge)}
            if(first!=null) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {StatusBadge(batch?.job?.draft?.kind?.label?.uppercase() ?: "FOTOS");StatusBadge(if(batch?.job?.emailSentAt!=null)"E-Mail bestätigt" else "Lokal gespeichert",confirmed=batch?.job?.emailSentAt!=null)}
                Text(batch?.let(::orderIdentifier) ?: first.reference,fontSize=if((batch?.let(::orderIdentifier) ?: first.reference).length>8)32.sp else 56.sp,lineHeight=if((batch?.let(::orderIdentifier) ?: first.reference).length>8)40.sp else 64.sp,fontWeight=FontWeight.ExtraBold,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("Container ${first.containerNumber}",style=MaterialTheme.typography.titleMedium,color=Muted)
                HorizontalDivider(color=Hairline)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("${group.size} Originalfotos",color=Muted);Text("Auf diesem Gerät",color=Muted,style=MaterialTheme.typography.bodySmall)}
            }
            LazyVerticalGrid(GridCells.Fixed(2),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(group,key={it.photoId}){photo->
                    Surface(shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,Hairline),color=Graphite) {
                        Column {
                            Box(Modifier.fillMaxWidth().clickable{fullId=photo.photoId}) {
                                PhotoImage(photo.localPath,photo.label,Modifier.fillMaxWidth().height(158.dp))
                                IconButton(onClick={toggle(photo.photoId)},modifier=Modifier.align(Alignment.TopEnd)) {
                                    Icon(if(photo.photoId in selected)Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        if(photo.photoId in selected)"Foto ${photo.step} abwählen" else "Foto ${photo.step} auswählen",tint=if(photo.photoId in selected)Lime else MaterialTheme.colorScheme.onSurface,modifier=Modifier.size(26.dp))
                                }
                            }
                            Text(photo.fileName,Modifier.padding(9.dp),style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if(local.isNotEmpty()) {
            Text("${chosen.size} von ${local.size} Fotos ausgewählt",color=Muted,style=MaterialTheme.typography.bodySmall)
            PrimaryIconButton("Fotos herunterladen",Icons.Outlined.FileDownload,!busy && chosen.isNotEmpty()){onSaveSelected(chosen.map{it.photoId}.toSet())}
            val info=chosen.groupBy{it.jobId}.mapNotNull{(jobId,photos)->
                jobs.find{it.job.id==jobId}?.let{photoShareText(it.job.draft,photos.map{p->p.containerNumber},photos.size)}
            }.joinToString("\n\n")
            PhotoShareInfoAction(info,chosen.isNotEmpty())
            OutlineAction("Auftrag per E-Mail",Icons.Outlined.Email,!busy && selectedJob?.complete==true) {
                chosen.firstOrNull()?.let(onShare)
            }
            Text(when {
                chosen.isEmpty() -> "Fotos eines Auftrags auswählen, um die E-Mail vorzubereiten."
                selectedJob == null -> "Für eine E-Mail bitte Fotos aus genau einem Auftrag auswählen."
                !selectedJob.complete -> "Auftrag noch unvollständig. Zuerst alle Pflichtfotos aufnehmen."
                else -> "Die E-Mail enthält alle ${namedPhotos(selectedJob).size} Fotos aller Container dieses Auftrags."
            },style=MaterialTheme.typography.bodySmall,color=Muted)
            Spacer(Modifier.height(4.dp))
        }
    }
    local.find{it.photoId==fullId}?.let{photo->Dialog(onDismissRequest={fullId=null}) {
        Surface(shape=MaterialTheme.shapes.medium){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(photo.fileName,style=MaterialTheme.typography.titleMedium)
            PhotoImage(photo.localPath,"Foto in Vollansicht",Modifier.fillMaxWidth().height(340.dp),full=true)
            val batch = jobs.find { it.job.id == photo.jobId }
            PrimaryIconButton("Auftrag per E-Mail",Icons.Outlined.Email,!busy && batch?.complete==true){onShare(photo)}
            Text(if(batch?.complete==true)"Alle ${namedPhotos(batch).size} Fotos dieses Auftrags werden angehängt." else "Auftrag noch unvollständig. Zuerst alle Pflichtfotos aufnehmen.",color=Muted,style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={fullId=null}){Text("Schliessen")}
        }}
    }}
}
