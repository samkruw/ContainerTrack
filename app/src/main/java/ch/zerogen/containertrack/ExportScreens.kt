package ch.zerogen.containertrack

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ExportJobsScreen(jobs: List<JobWithContainers>, onJob: (JobWithContainers) -> Unit) {
    val complete = jobs.filter { it.complete }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Export", "Nur vollständige Aufträge können exportiert werden.") }
        if (complete.isEmpty()) item { Information("Noch kein vollständiger Auftrag vorhanden. Zuerst alle Container und Pflichtfotos erfassen.") }
        items(complete, key = { it.job.id }) { JobRow(it) { onJob(it) } }
    }
}

@Composable
fun ExportReviewScreen(batch: JobWithContainers, busy: Boolean, onShare: () -> Unit, onExport: () -> Unit) {
    val job = batch.job
    val draft = job.draft
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenTitle("Export prüfen", "${draft.referenceNumber}.zip")
        Information("${draft.kind.label} · ${batch.completedCount} / ${draft.quantity} Container\n${batch.containers.sumOf { it.photos.size }} Fotos\nUTF-8 TXT und Metadaten enthalten")
        Text("Excel-Zuordnung", style = MaterialTheme.typography.titleMedium)
        val rows = listOf("In/Out" to if (draft.kind == JobKind.DELIVERY) "OUT" else "IN",
            "Auftragsnummer" to draft.referenceNumber,
            "Order" to draft.orderValue, "Quantity" to draft.quantity.toString()) +
            (if (draft.kind == JobKind.RETURN) listOf("additional Work" to draft.additionalWork) else emptyList()) +
            listOf("Container Number" to batch.ordered.joinToString(",") { it.container.containerNumber }) +
            (if (draft.kind == JobKind.RETURN) listOf("Quick" to draft.quick) else emptyList())
        GlassSurface {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { (label, value) ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(color = Graphite)
                }
            }
        }
        if (draft.kind == JobKind.DELIVERY) {
            Information(if (job.exportedAt == null) "Sobald die ZIP gespeichert oder zum Teilen erstellt ist, beginnt die 30-Minuten-Erinnerung zum Starten des Loggers. Sie läuft auch weiter, wenn Sie die App-Auswahl abbrechen. Benachrichtigungen in den Einstellungen erlauben."
                else "Erster Export · ${localTime(job.exportedAt)}\nErneuter Export verändert die Logger-Erinnerung nicht.")
        }
        if (batch.isManagedReturn) Information("Der lokale ZIP-Export beendet die 24-Stunden-Erinnerung nicht. Sie bleibt bis zur Bestätigung „E-Mail gesendet“ aktiv.")
        Text("ZIP direkt mit einer App teilen, zum Beispiel Ihrem E-Mail-Programm, oder auf dem Gerät speichern.", style = MaterialTheme.typography.bodyMedium)
        PrimaryButton(if (busy) "ZIP wird erstellt …" else "ZIP teilen", batch.complete && !busy, onShare)
        OutlinedButton(onClick = onExport, enabled = batch.complete && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ZIP speichern") }
        Text("Für Offline-Betrieb einen lokalen Speicherort wählen. Beim Teilen verarbeitet die ausgewählte App den Anhang.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MoreScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { PermissionsContent() }
}

@Composable
fun PermissionsContent() {
    val context = LocalContext.current
    val app = context.applicationContext as ContainerTrackApp
    val lifecycle = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val notifications = remember(refresh) { app.alarms.notificationsAllowed() }
    val exact = remember(refresh) { app.alarms.exactAllowed() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Logger- und Return-Erinnerungen", style = MaterialTheme.typography.titleLarge)
        Information(if (notifications) "Benachrichtigungen sind erlaubt. Lieferungen erinnern an den Logger-Start, Return-Aufträge 24 Stunden nach Anlage an den offenen Abschluss." else "Benachrichtigungen sind gesperrt. Fällige Logger- und Return-Erinnerungen werden beim Öffnen der App weiterhin angezeigt. Eine Systembenachrichtigung ist dann nicht möglich.", warning = !notifications)
        PrimaryButton(if (notifications) "Benachrichtigungseinstellungen" else "Benachrichtigungen erlauben") {
            if (Build.VERSION.SDK_INT >= 33 && !notifications) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
        TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Systemeinstellungen für Benachrichtigungen öffnen") }
        Information(if (exact) "Genaue Alarme sind verfügbar. Android kann Benachrichtigungen trotzdem durch Energiespar- oder Geräteeinstellungen verzögern." else "Ohne Zugriff auf genaue Alarme kann Android die Erinnerung im Standby verzögern. Der gespeicherte Fälligkeitszeitpunkt bleibt unverändert.", warning = !exact)
        if (Build.VERSION.SDK_INT >= 31 && !exact) OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }) { Text("Genaue Alarme erlauben") }
        HorizontalDivider()
        Text("Offline und Datenschutz", style = MaterialTheme.typography.titleLarge)
        Text("Aufträge, OCR und Fotos werden auf diesem Gerät verarbeitet. Die E-Mail-Vorbereitung übergibt alle Originalfotos eines vollständigen Auftrags mit der Auftragsnummer als Betreff an deine E-Mail-App. Container Track benötigt keine Anmeldung. Die App kann den tatsächlichen Versand nicht prüfen. Die Return-Erinnerung bleibt aktiv, bis du in der App „E-Mail gesendet“ bestätigst. Fotos liegen im internen App-Speicher. Android-Backups sind deaktiviert.")
        Text("ZIP-Dateien werden am selbst gewählten Speicherort abgelegt oder über Android an eine von Ihnen ausgewählte App übergeben. Beim Teilen kann diese App den Anhang versenden. Bei Deinstallation werden die internen Aufträge und Fotos gelöscht; separat gespeicherte Dateien bleiben erhalten.")
        Text("App-Berechtigungen", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Kamera und App-Berechtigungen öffnen") }
        Text("Container Track · Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
