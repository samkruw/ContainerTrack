package ch.zerogen.containertrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ContainerTheme { TrackApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackApp(vm: TrackViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val active by vm.activeJob.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as ContainerTrackApp
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val saveZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri) else vm.pendingExport = null
    }
    val exportJob: (JobWithContainers) -> Unit = { batch ->
        if (!busy) {
            vm.pendingExport = batch.job.id
            saveZip.launch("${batch.job.draft.referenceNumber}.zip")
        }
    }
    LaunchedEffect(reminders.size) {
        if (reminders.isNotEmpty() && !notificationAsked && Build.VERSION.SDK_INT >= 33 && !app.alarms.notificationsAllowed()) {
            notificationAsked = true
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun openJob(id: String) { vm.selectJob(id); nav.navigate("job/$id") { launchSingleTop = true } }
    val tabs = listOf("home" to "Start", "jobs" to "Aufträge", "scan" to "Scannen", "exports" to "Export", "more" to "Mehr")
    Scaffold(topBar = {
        TopAppBar(title = { Text("Container Track", style = MaterialTheme.typography.titleLarge) }, navigationIcon = {
            if (route !in tabs.map { it.first }) IconButton(onClick = { if (!busy) nav.popBackStack() }, enabled = !busy) { Icon(Icons.Default.ArrowBack, "Zurück") }
        })
    }, bottomBar = {
        NavigationBar(containerColor = Navy) {
            tabs.forEachIndexed { index, (path, label) ->
                NavigationBarItem(selected = route == path, enabled = !busy,
                    onClick = { nav.navigate(path) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(when (index) { 0 -> Icons.Default.Home; 1 -> Icons.Default.List; 2 -> Icons.Default.Search; 3 -> Icons.Default.Share; else -> Icons.Default.MoreVert }, label) },
                    label = { Text(label, maxLines = 1) })
            }
        }
    }) { insets ->
        Column(Modifier.padding(insets).imePadding().fillMaxSize()) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            reminders.firstOrNull()?.let { ReminderStrip(it, reminders.size - 1, now) }
            NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                composable("home") { HomeScreen(jobs, onBegin = { kind -> vm.begin(kind); nav.navigate("setup") }, onJob = ::openJob) }
                composable("jobs") { JobsScreen(jobs, onJob = ::openJob) }
                composable("scan") { ScanJobsScreen(jobs, active, onJob = { batch ->
                    vm.selectJob(batch.job.id)
                    val unfinished = batch.ordered.firstOrNull { it.container.completedAt == null }
                    if (unfinished != null) nav.navigate("photo/${batch.job.id}/${unfinished.container.id}?step=0")
                    else nav.navigate("scan/${batch.job.id}")
                }) }
                composable("exports") { ExportJobsScreen(jobs, onJob = { nav.navigate("export/${it.job.id}") }) }
                composable("more") { MoreScreen() }
                composable("setup") { SetupScreen(vm, busy) { id -> nav.navigate("job/$id") { popUpTo("setup") { inclusive = true } } } }
                composable("job/{jobId}") { back ->
                    val id = back.arguments?.getString("jobId") ?: return@composable
                    val batch = jobs.find { it.job.id == id }
                    if (batch != null) JobScreen(batch, vm, busy,
                        onScan = { nav.navigate("scan/$id") },
                        onContainer = { nav.navigate("photo/$id/$it?step=0") },
                        onExport = { nav.navigate("export/$id") }, onDeleted = { nav.popBackStack() })
                }
                composable("scan/{jobId}") { back ->
                    val id = back.arguments?.getString("jobId") ?: return@composable
                    jobs.find { it.job.id == id }?.let { batch ->
                        ScanScreen(batch, vm, busy) { containerId -> nav.navigate("photo/$id/$containerId?step=0") { popUpTo("scan/$id") { inclusive = true } } }
                    }
                }
                composable("photo/{jobId}/{containerId}?step={step}") { back ->
                    val id = back.arguments?.getString("jobId") ?: return@composable
                    val containerId = back.arguments?.getString("containerId") ?: return@composable
                    val selectedStep = back.arguments?.getString("step")?.toIntOrNull() ?: 0
                    jobs.find { it.job.id == id }?.let { batch ->
                        PhotoScreen(batch, containerId, selectedStep, vm, busy, onDone = {
                            nav.navigate("job/$id") { popUpTo("job/$id") { inclusive = true }; launchSingleTop = true }
                        })
                    }
                }
                composable("export/{jobId}") { back ->
                    val id = back.arguments?.getString("jobId") ?: return@composable
                    jobs.find { it.job.id == id }?.let { ExportReviewScreen(it, busy) { exportJob(it) } }
                }
            }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { vm.error.value = null }, title = { Text("Vorgang prüfen") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("Verstanden") } }) }
    notice?.let { message -> AlertDialog(onDismissRequest = { vm.notice.value = null }, title = { Text("Export gespeichert") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.notice.value = null }) { Text("Schliessen") } }) }
    reminders.firstOrNull { it.dueAt <= now }?.let { reminder ->
        AlertDialog(onDismissRequest = {}, title = { Text("Logger jetzt starten") },
            text = { Text("Auftrag ${reminder.referenceNumber}\nBitte Logger starten und anschliessend bestätigen.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { vm.confirmStarted(reminder.id) }) { Text("Logger gestartet – bestätigen") } })
    }
}

@Composable
fun ReminderStrip(reminder: LoggerReminder, others: Int, now: Long) {
    val remaining = ((reminder.dueAt - now + 999) / 1000).coerceAtLeast(0)
    Surface(color = Graphite) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Logger · ${reminder.referenceNumber} · ${if (remaining == 0L) "JETZT" else "%02d:%02d".format(remaining / 60, remaining % 60)}${if (others > 0) " · +$others weitere" else ""}",
                color = if (remaining == 0L) Warning else LightBlue, style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (1f - remaining / 1800f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (remaining == 0L) Warning else ElectricBlue)
        }
    }
}
