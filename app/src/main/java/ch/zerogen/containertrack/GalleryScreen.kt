package ch.zerogen.containertrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun GalleryScreen(jobs: List<JobWithContainers>, uploads: List<UploadPhotoEntity>, onShare: (GalleryPhoto) -> Unit) {
    var reference by rememberSaveable { mutableStateOf("") }
    var container by rememberSaveable { mutableStateOf("") }
    var uploader by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val rows = remember(jobs,uploads) { galleryPhotos(jobs,uploads) }
    val local by produceState(emptyList<GalleryPhoto>(),rows) { value=withContext(Dispatchers.IO) { rows.filter { File(it.localPath).isFile } } }
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Galerie","Lokal gespeicherte Fotos")
        OutlinedTextField(reference,{reference=it},Modifier.fillMaxWidth(),label={Text("KT / Order / Return")},singleLine=true)
        OutlinedTextField(container,{container=it},Modifier.fillMaxWidth(),label={Text("Containernummer")},singleLine=true)
        val uploaders=local.mapNotNull { it.uploaderAccountId }.distinct()
        if(uploaders.isNotEmpty()) {
            TextButton(onClick={uploader=if(uploader==null) uploaders.first() else null}) { Text(if(uploader==null) "Uploader: Alle" else "Uploader: $uploader (bestätigt)") }
            if (uploader != null && uploaders.size > 1) uploaders.filter { it != uploader }.forEach { account ->
                TextButton(onClick={uploader=account}) { Text("Uploader: $account") }
            }
        }
        val visible=local.filter { it.matches(reference,container,uploader) }
        if(visible.isEmpty()) Information("Keine passenden lokalen Fotos vorhanden.")
        LazyVerticalGrid(GridCells.Fixed(2),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(visible,key={it.photoId}) { photo ->
                GlassSurface(onClick={selectedId=photo.photoId}) {
                    Column(Modifier.padding(8.dp)) {
                        PhotoImage(photo.localPath,"${photo.containerNumber} Foto ${photo.step}",Modifier.fillMaxWidth().height(130.dp))
                        Text("${photo.containerNumber} · ${photo.step.toString().padStart(2,'0')}",style=MaterialTheme.typography.labelLarge)
                        Text(photo.reference,style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    local.find { it.photoId == selectedId }?.let { photo -> Dialog(onDismissRequest={selectedId=null}) {
        Surface(shape=MaterialTheme.shapes.large) { Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("${photo.containerNumber} · Foto ${photo.step}",style=MaterialTheme.typography.titleLarge)
            PhotoImage(photo.localPath,"Foto in Vollansicht",Modifier.fillMaxWidth().height(360.dp),full=true)
            PrimaryButton("Foto teilen") { onShare(photo) }
            TextButton(onClick={selectedId=null}) { Text("Schliessen") }
        } }
    } }
}
