package ch.zerogen.containertrack

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoImage(path: String, description: String, modifier: Modifier = Modifier, full: Boolean = false) {
    val bitmap by produceState<Bitmap?>(null,path,full) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                BitmapFactory.decodeFile(path,bounds)
                val options=BitmapFactory.Options().apply {
                    inSampleSize=1
                    while(maxOf(bounds.outWidth,bounds.outHeight)/inSampleSize > if(full)1600 else 256) inSampleSize*=2
                }
                BitmapFactory.decodeFile(path,options)
            }.getOrNull()
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(),description,modifier,contentScale = if (full) ContentScale.Fit else ContentScale.Crop)
    else Box(modifier) { Text("Vorschau nicht verfügbar",style = MaterialTheme.typography.labelSmall,color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun ContainerDetails(item: ContainerWithPhotos, kind: JobKind, readonly: Boolean, expanded: Boolean,
                     onExpand: () -> Unit, onPhoto: (Int) -> Unit, busy: Boolean) {
    val steps=item.photos.map { it.step }.toSet()
    GlassSurface {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth().clickable(onClick = onExpand).heightIn(min = 56.dp)) {
                Text("${item.container.containerNumber} · ${item.photos.size}/${kind.photoCount} Fotos",style = MaterialTheme.typography.titleMedium)
                Text(when {
                    steps.containsAll((1..kind.photoCount).toSet()) -> "Vollständig"
                    kind == JobKind.DELIVERY && steps.containsAll((1..5).toSet()) && 6 !in steps -> "Befüllt-Foto fehlt"
                    else -> "In Bearbeitung"
                } + if (expanded) " · Schliessen ▴" else " · Öffnen ▾",color = if (item.photos.size == kind.photoCount) Success else Warning,style = MaterialTheme.typography.labelMedium)
            }
            if (expanded) {
                for (row in (1..kind.photoCount).chunked(2)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { step ->
                            val photo=item.photos.find { it.step == step }
                            val allowed = photo != null || (!readonly && nextPhotoStep(kind,steps) == step)
                            OutlinedButton(onClick = { onPhoto(step) },enabled = allowed && !busy,
                                modifier = Modifier.weight(1f).heightIn(min = 108.dp),contentPadding = PaddingValues(8.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (photo != null) PhotoImage(photo.localPath,photoLabel(kind,step),Modifier.fillMaxWidth().height(68.dp))
                                    Text("${step.toString().padStart(2,'0')} · ${if (kind == JobKind.RETURN) "Inhalt" else listOf("Leer","Front","Rückseite / Türen","Links","Rechts","Befüllt")[step-1]}",style = MaterialTheme.typography.labelMedium)
                                    Text(if (photo == null) "Aufnehmen" else "Ansehen / Ersetzen",style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (row.size == 1 && kind.photoCount > 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
