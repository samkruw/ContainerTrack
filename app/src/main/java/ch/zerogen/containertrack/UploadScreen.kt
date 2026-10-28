package ch.zerogen.containertrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun UploadScreen(batch: JobWithContainers, busy: Boolean, onPrepareEmail: () -> Unit,
                 onOrders: () -> Unit, onConfirmSent: () -> Unit) {
    val photos = namedPhotos(batch)
    val job = batch.job
    val attachmentBytes = photos.sumOf { File(it.photo.localPath).length() }
    val attachmentSize = "%.1f MB".format(attachmentBytes / 1_000_000f)
    val prepared = job.emailPreparedRevision == job.documentationRevision && job.emailPreparedAt != null
    var confirming by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Email, null, tint = Lime)
                Text("Auftrag per E-Mail", color = Lime, style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider(Modifier.padding(top = 12.dp), color = Hairline)
        }
        item {
            ScreenTitle(job.draft.referenceNumber, "${batch.containers.size} Container · ${photos.size} Originalfotos")
            Text("Betreff: ${job.draft.referenceNumber}", color = Muted)
            Text("Die E-Mail enthält alle Fotos aller Container dieses Auftrags.", color = Muted)
        }
        if (!batch.complete) item { Information("Die Fotodokumentation ist noch unvollständig. Zuerst alle Pflichtfotos und bei Return die passende Anzahl Leer-/Sauber-Fotos aufnehmen.", warning = true) }
        if (job.emailSentAt != null) item { Information("E-Mail gesendet · in der App bestätigt am ${localTime(job.emailSentAt)}") }
        item {
            PrimaryIconButton(if (prepared) "E-Mail erneut vorbereiten" else "E-Mail vorbereiten", Icons.Outlined.Email,
                !busy && batch.complete, onPrepareEmail)
        }
        item {
            Information("Die App kann den tatsächlichen Versand nicht prüfen. Sende die vorbereitete E-Mail in deiner E-Mail-App und bestätige anschliessend hier mit „E-Mail gesendet“. Abbrechen oder das Öffnen der E-Mail-App bestätigt keinen Versand.")
            if (batch.isManagedReturn && job.emailSentAt == null) Text("Die 24-Stunden-Erinnerung bleibt bis zu deiner Bestätigung aktiv.", color = Warning)
        }
        if (job.emailSentAt == null) item {
            PrimaryIconButton("E-Mail gesendet", Icons.Outlined.CheckCircle, !busy && batch.complete && prepared) { confirming = true }
            if (!prepared) Text("Zuerst die E-Mail für den aktuellen Stand vorbereiten. Nach Fotoänderungen ist eine neue Vorbereitung nötig.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item { Text("${photos.size} Anhänge · $attachmentSize", style = MaterialTheme.typography.titleMedium); HorizontalDivider(Modifier.padding(top = 10.dp), color = Hairline) }
        if (attachmentBytes > 20L * 1024 * 1024) item {
            Information("Die Anhänge umfassen $attachmentSize. Deine E-Mail-App oder dein Anbieter kann eine Grössenbegrenzung haben. Alle Fotos sind enthalten; die App lässt keine Anhänge weg.", warning = true)
        }
        items(photos, key = { it.photo.id }) { named ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoImage(named.photo.localPath, named.label, Modifier.size(54.dp))
                Column(Modifier.weight(1f)) {
                    Text(named.fileName, fontWeight = FontWeight.SemiBold)
                    Text("${named.label} · %.1f MB".format(File(named.photo.localPath).length() / 1_000_000f), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Icon(Icons.Outlined.CheckCircle, "Foto lokal gespeichert", tint = Lime, modifier = Modifier.size(24.dp))
            }
            HorizontalDivider(Modifier.padding(top = 10.dp), color = Hairline)
        }
        item { OutlineAction("Zur Auftragsübersicht", Icons.Outlined.FormatListBulleted, !busy, onOrders) }
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false }, title = { Text("E-Mail gesendet?") },
        text = { Text("Bestätige nur, wenn du die E-Mail mit allen ${photos.size} Fotos tatsächlich gesendet hast. Die App kann den Versand nicht überprüfen.") },
        confirmButton = { TextButton(enabled = !busy && batch.complete && prepared, onClick = { confirming = false; onConfirmSent() }) { Text("E-Mail gesendet") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("Zurück") } })
}
