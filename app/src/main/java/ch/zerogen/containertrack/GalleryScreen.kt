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
                  onSaveSelected:(Set<String>)->Unit={},onShareSelected:((Set<String>)->Unit)?=null,
                  onTeamsShareSelected:((Set<String>)->Unit)?=null) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    var query by rememberSaveable{mutableStateOf("")}
    var owner by rememberSaveable{mutableStateOf<String?>(null)}
    var ownerMenu by remember{mutableStateOf(false)}
    var opened by rememberSaveable{mutableStateOf<String?>(null)}
    var fullId by rememberSaveable{mutableStateOf<String?>(null)}
    var selected by rememberSaveable{mutableStateOf(listOf<String>())}
    val rows=remember(jobs,uploads){galleryPhotos(jobs,uploads)}
    val local by produceState(emptyList<GalleryPhoto>(),rows){value=withContext(Dispatchers.IO){rows.filter{File(it.localPath).isFile}}}
    val groups=local.filter{(it.matches(query,"",owner)||it.containerNumber.contains(query.trim())) && (owner==null || it.uploaderAccountId==owner)}.groupBy{"${it.jobId}|${it.containerNumber}"}
    val group=local.filter{"${it.jobId}|${it.containerNumber}"==opened}
    val chosen=selectedGalleryPhotos(if(opened==null)local else group,selected.toSet())
    fun toggle(id:String){selected=if(id in selected)selected-id else selected+id}
    BackHandler(opened!=null){opened=null}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(opened==null) {
            Text("AUF DIESEM GERÄT",Modifier.padding(top=18.dp),color=Lime,style=MaterialTheme.typography.labelMedium)
            ScreenTitle("Galerie")
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("KT oder Container suchen")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Done),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={focus.clearFocus()}))
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick={ownerMenu=true},shape=MaterialTheme.shapes.small,modifier=Modifier.heightIn(min=48.dp)) {Icon(Icons.Outlined.Group,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(owner==null)"Alle Uploader" else "Bestätigter Uploader");Icon(Icons.Outlined.ExpandMore,null)}
                    DropdownMenu(ownerMenu,{ownerMenu=false}) {
                        DropdownMenuItem(text={Text("Alle Uploader")},onClick={owner=null;ownerMenu=false})
                        local.mapNotNull{it.uploaderAccountId}.distinct().forEach{account->DropdownMenuItem(text={Text(account)},onClick={owner=account;ownerMenu=false})}
                    }
                }
                Text("${local.size} lokale Fotos",color=Muted,style=MaterialTheme.typography.bodySmall)
            }
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
                                StatusBadge(if(photos.all{it.uploaderAccountId!=null})"Gesendet" else "Lokal",confirmed=photos.all{it.uploaderAccountId!=null})
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
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {StatusBadge(batch?.job?.draft?.kind?.label?.uppercase() ?: "FOTOS");StatusBadge(if(group.all{it.uploaderAccountId!=null})"Gesendet" else "Lokal gespeichert",confirmed=group.all{it.uploaderAccountId!=null})}
                Text(batch?.let(::orderIdentifier) ?: first.reference,fontSize=56.sp,lineHeight=64.sp,fontWeight=FontWeight.ExtraBold)
                Text("Container ${first.containerNumber}",style=MaterialTheme.typography.titleMedium,color=Muted)
                HorizontalDivider(color=Hairline)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("${group.size} Originalfotos",color=Muted);Text(if(group.all{it.uploaderAccountId!=null})"In Teams bestätigt" else "Auf diesem Gerät",color=Muted,style=MaterialTheme.typography.bodySmall)}
            }
            LazyVerticalGrid(GridCells.Fixed(2),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(group,key={it.photoId}){photo->
                    Surface(shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,Hairline),color=Graphite) {
                        Column {
                            Box(Modifier.fillMaxWidth().clickable{fullId=photo.photoId}) {
                                PhotoImage(photo.localPath,"${photo.containerNumber} Foto ${photo.step}",Modifier.fillMaxWidth().height(158.dp))
                                IconButton(onClick={toggle(photo.photoId)},modifier=Modifier.align(Alignment.TopEnd)) {
                                    Icon(if(photo.photoId in selected)Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        if(photo.photoId in selected)"Foto ${photo.step} abwählen" else "Foto ${photo.step} auswählen",tint=if(photo.photoId in selected)Lime else MaterialTheme.colorScheme.onSurface,modifier=Modifier.size(26.dp))
                                }
                            }
                            Text(photoName(photo.containerNumber,photo.step),Modifier.padding(9.dp),style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if(local.isNotEmpty()) {
            Text("${chosen.size} von ${if(opened==null)local.size else group.size} Fotos ausgewählt",color=Muted,style=MaterialTheme.typography.bodySmall)
            PrimaryIconButton("Fotos herunterladen",Icons.Outlined.FileDownload,chosen.isNotEmpty()){onSaveSelected(chosen.map{it.photoId}.toSet())}
            OutlineAction("In Teams teilen",Icons.Outlined.Groups,chosen.isNotEmpty() && onTeamsShareSelected!=null) {
                onTeamsShareSelected?.invoke(chosen.map{it.photoId}.toSet())
            }
            OutlineAction("Über Android teilen",Icons.Outlined.Share,chosen.isNotEmpty() && (onShareSelected!=null || chosen.size==1)) {
                if(onShareSelected!=null)onShareSelected(chosen.map{it.photoId}.toSet()) else chosen.singleOrNull()?.let(onShare)
            }
            Text("Originalfotos · kein ZIP · keine Excel-Datei",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.labelSmall,color=Muted)
            Spacer(Modifier.height(4.dp))
        }
    }
    local.find{it.photoId==fullId}?.let{photo->Dialog(onDismissRequest={fullId=null}) {
        Surface(shape=MaterialTheme.shapes.medium){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(photoName(photo.containerNumber,photo.step),style=MaterialTheme.typography.titleMedium)
            PhotoImage(photo.localPath,"Foto in Vollansicht",Modifier.fillMaxWidth().height(340.dp),full=true)
            PrimaryIconButton("Foto teilen",Icons.Outlined.Share){onShare(photo)}
            TextButton(onClick={fullId=null}){Text("Schliessen")}
        }}
    }}
}
