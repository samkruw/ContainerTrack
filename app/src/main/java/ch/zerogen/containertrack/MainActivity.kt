package ch.zerogen.containertrack

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        if(Build.VERSION.SDK_INT>=29) window.isNavigationBarContrastEnforced=false
        setContent { ContainerTheme { TrackApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackApp(vm: TrackViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "jobs"
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val active by vm.activeJob.collectAsStateWithLifecycle()
    val uploads by vm.uploadJobs.collectAsStateWithLifecycle()
    val uploadedPhotos by vm.photoUploads.collectAsStateWithLifecycle()
    val identity by vm.microsoftIdentity.collectAsStateWithLifecycle()
    val microsoftStatus by vm.microsoftStatus.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val query by vm.search.collectAsStateWithLifecycle()
    val filterName by vm.filter.collectAsStateWithLifecycle()
    var actionJob by rememberSaveable { mutableStateOf<String?>(null) }
    var shareJob by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val app = context.applicationContext as ContainerTrackApp
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(vm, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.shares.collect { intent ->
                try { launchShareIntent(intent) { context.startActivity(it) } }
                catch (_: android.content.ActivityNotFoundException) {
                    vm.error.value = "Keine App zum Teilen gefunden. Bitte eine passende App installieren."
                } catch (_: SecurityException) {
                    vm.error.value = "Dateien konnten nicht geteilt werden. Bitte erneut versuchen."
                }
            }
        }
    }
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
    fun resumeJob(id:String) {
        val batch=jobs.find{it.job.id==id} ?: return
        vm.selectJob(id)
        if(batch.complete || batch.job.exportedAt!=null){openJob(id);return}
        val next=batch.ordered.firstOrNull{item->!(1..minOf(5,batch.job.draft.kind.photoCount)).all{step->item.photos.any{it.step==step}}}
        if(next!=null) nav.navigate("photo/$id/${next.container.id}?step=0")
        else if(batch.containers.size<batch.job.draft.quantity) nav.navigate("scan/$id")
        else batch.ordered.firstOrNull{it.photos.size<batch.job.draft.kind.photoCount}?.let{nav.navigate("photo/$id/${it.container.id}?step=0")}
    }
    val tabs = listOf("jobs" to "Aufträge", "scan" to "Kamera", "gallery" to "Galerie")
    var savingPhotos by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val savePhotos = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if(uri!=null && savingPhotos.isNotEmpty()) vm.saveSelectedPhotos(savingPhotos.toSet(),uri)
        savingPhotos=arrayListOf()
    }
    Scaffold(containerColor=Navy,topBar={if(route!="profile" || identity!=null)BrandHeader(identity){if(!busy)nav.navigate("profile"){launchSingleTop=true}}},
        floatingActionButton={if(route=="jobs")FloatingActionButton(onClick={if(!busy){vm.begin(JobKind.DELIVERY);nav.navigate("setup")}},containerColor=Lime,contentColor=Navy,shape=MaterialTheme.shapes.small){Icon(Icons.Default.Add,"Neuer Auftrag")}},
        bottomBar={if(route!="profile" || identity!=null)ReferenceBottomBar(route,!busy){path->nav.navigate(path){popUpTo(nav.graph.startDestinationId){saveState=true};launchSingleTop=true;restoreState=true}}}
    ) { insets ->
        Column(Modifier.padding(insets).imePadding().fillMaxSize()) {
            if(route !in tabs.map{it.first} && route!="setup" && route!="profile" && !route.startsWith("upload/")) Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={if(!busy)nav.popBackStack()},enabled=!busy){Icon(Icons.Default.ArrowBack,"Zurück")}
                Text(when {route=="profile"->"Profil";route=="access"->"Benutzer & Rechte";route.startsWith("upload/")->"Teams-Übertragung";route.startsWith("review/")->"Fotos prüfen";route.startsWith("job/")->"Auftragsdetails";else->"Zurück zum Auftrag"},style=MaterialTheme.typography.titleLarge)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            reminders.firstOrNull()?.let { ReminderStrip(it, reminders.size - 1, now) }
            NavHost(nav, startDestination = "jobs", modifier = Modifier.weight(1f)) {
                composable("home") { HomeScreen(jobs, onBegin = { kind -> vm.begin(kind); nav.navigate("setup") }, onJob = ::openJob) }
                composable("jobs") { OrderListScreen(jobs,uploads,uploadedPhotos,reminders,now,online,query,
                    runCatching { OrderFilter.valueOf(filterName) }.getOrDefault(OrderFilter.ACTIVE),vm::search,{vm.filter(it.name)},{actionJob=it},identity,onResume=::resumeJob,onRetry=vm::uploadPhotos) }
                composable("gallery") { GalleryScreen(jobs,uploadedPhotos,onShare={vm.sharePhotos(it.jobId,photoId=it.photoId)},
                    onSaveSelected={savingPhotos=ArrayList(it);savePhotos.launch(null)},onShareSelected=vm::shareSelectedPhotos,onTeamsShareSelected=vm::shareSelectedPhotosToTeams) }
                composable("profile") { ProfileScreen(identity,microsoftStatus,vm.microsoftConfigured,busy,vm::signIn,vm::signOut,vm::switchAccount,onContinue={nav.navigate("jobs"){popUpTo("jobs");launchSingleTop=true}},onAccess={nav.navigate("access")}) }
                composable("access") {UserAccessScreen(identity)}
                composable("scan") { ScanJobsScreen(jobs,active,onJob={resumeJob(it.job.id)}) }
                composable("exports") { ExportJobsScreen(jobs, onJob = { nav.navigate("export/${it.job.id}") }) }
                composable("more") { MoreScreen() }
                composable("setup") { SetupScreen(vm, busy) { id -> nav.navigate("job/$id") { popUpTo("setup") { inclusive = true } };vm.createdContainerId?.let{container->nav.navigate("photo/$id/$container?step=0")} } }
                composable("job/{jobId}") { back ->
                    val id = back.arguments?.getString("jobId") ?: return@composable
                    val batch = jobs.find { it.job.id == id }
                    if (batch != null) JobScreen(batch, vm, busy,
                        onScan = { nav.navigate("scan/$id") },
                        onContainer = { nav.navigate("photo/$id/$it?step=0") },
                        onExport = { nav.navigate("export/$id") }, onDeleted = { nav.popBackStack() }, onPhoto = { container, step -> nav.navigate("photo/$id/$container?step=$step") },onUpload={nav.navigate("upload/$id")},onReview={nav.navigate("review/$id")})
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
                composable("review/{jobId}") { back ->
                    val id=back.arguments?.getString("jobId") ?: return@composable
                    jobs.find{it.job.id==id}?.let{batch->PhotoReviewScreen(batch,busy,
                        onPhoto={container,step->nav.navigate("photo/$id/$container?step=$step")},onContinue={nav.navigate("upload/$id")},onBack={batch.ordered.firstOrNull()?.let{nav.navigate("photo/$id/${it.container.id}?step=0")} ?: resumeJob(id)})}
                }
                composable("upload/{jobId}") { back ->
                    val id=back.arguments?.getString("jobId") ?: return@composable
                    jobs.find{it.job.id==id}?.let{batch->UploadScreen(batch,uploads.find{it.jobId==id},uploadedPhotos,identity,vm.microsoftConfigured,online,busy,
                        onUpload={vm.uploadPhotos(id)},onShare={vm.sharePhotos(id)},onOrders={nav.navigate("jobs"){popUpTo("jobs");launchSingleTop=true}},onProfile={nav.navigate("profile")},onTeamsShare={vm.sharePhotos(id,teams=true)})}
                }
                composable("export/{jobId}") { back ->
                    val id = back.arguments?.getString("jobId") ?: return@composable
                    jobs.find { it.job.id == id }?.let { batch ->
                        ExportReviewScreen(batch, busy, onShare = { vm.share(id) }, onExport = { exportJob(batch) })
                    }
                }
            }
        }
    }
    jobs.find { it.job.id==actionJob }?.let { batch -> ModalBottomSheet(onDismissRequest={actionJob=null}) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            ScreenTitle(batch.job.draft.referenceNumber,"${batch.containers.sumOf { it.photos.size }}/${batch.job.draft.quantity*batch.job.draft.kind.photoCount} Fotos")
            PrimaryButton("Fotodokumentation fortsetzen",enabled=!busy && !batch.complete) {
                actionJob=null;vm.selectJob(batch.job.id)
                val first=batch.ordered.firstOrNull { item -> !(1..minOf(5,batch.job.draft.kind.photoCount)).all { step -> item.photos.any { it.step==step } } }
                if(first!=null) nav.navigate("photo/${batch.job.id}/${first.container.id}?step=0")
                else if(batch.containers.size < batch.job.draft.quantity) nav.navigate("scan/${batch.job.id}")
                else batch.ordered.firstOrNull { it.photos.size<batch.job.draft.kind.photoCount }?.let { nav.navigate("photo/${batch.job.id}/${it.container.id}?step=0") }
            }
            PrimaryButton("Auftragsdetails",enabled=!busy) { actionJob=null;openJob(batch.job.id) }
            OutlinedButton(onClick={actionJob=null;shareJob=batch.job.id},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Fotos teilen / Teams") }
            OutlinedButton(onClick={actionJob=null;nav.navigate("export/${batch.job.id}")},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Lokaler ZIP-Export") }
            Spacer(Modifier.height(24.dp))
        }
    } }
    jobs.find { it.job.id==shareJob }?.let { batch -> ModalBottomSheet(onDismissRequest={shareJob=null}) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            ScreenTitle("Fotos teilen","Ausschließlich nummerierte JPEG-Dateien")
            uploads.find { it.jobId==batch.job.id }?.let { Text(syncLabel(it,online));it.lastError?.let { message -> Information(message,warning=true) } }
            OutlineAction("Übertragungsdetails",androidx.compose.material.icons.Icons.Default.Info) {shareJob=null;nav.navigate("upload/${batch.job.id}")}
            PrimaryButton("An Teams-Kanal übertragen",enabled=!busy && batch.complete && identity!=null && vm.microsoftConfigured) { vm.uploadPhotos(batch.job.id);shareJob=null }
            if(identity==null) Text("Für den automatischen Upload zuerst im Profil anmelden.")
            val teamsAvailable = remember(batch.containers.sumOf { it.photos.size }) {
                Intent(if (batch.containers.sumOf { it.photos.size } > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND)
                    .setType("image/jpeg").setPackage("com.microsoft.teams").resolveActivity(context.packageManager) != null
            }
            if (teamsAvailable) PrimaryButton("In Teams-App teilen",enabled=!busy && batch.containers.any { it.photos.isNotEmpty() }) { vm.sharePhotos(batch.job.id,teams=true);shareJob=null }
            else Text("Teams-App nicht verfügbar. Fotos können über Android geteilt werden.")
            OutlinedButton(onClick={vm.sharePhotos(batch.job.id);shareJob=null},enabled=!busy && batch.containers.any { it.photos.isNotEmpty() },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Fotos über Android teilen") }
            Text("Der Android-Teilen-Dialog bestätigt keinen Upload. Der Teams-Status zählt nur vom Server bestätigte Dateien.",style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
        }
    } }
    error?.let { message -> AlertDialog(onDismissRequest = { vm.error.value = null }, title = { Text("Vorgang prüfen") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("Verstanden") } }) }
    notice?.let { message -> AlertDialog(onDismissRequest = { vm.notice.value = null }, title = { Text("Hinweis") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.notice.value = null }) { Text("Schliessen") } }) }
    reminders.firstOrNull { it.dueAt <= now }?.let { reminder ->
        AlertDialog(onDismissRequest = {}, title = { Text("Logger jetzt starten") },
            text = { Text("Auftrag ${reminder.referenceNumber}\nBitte Logger starten und anschliessend bestätigen.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { vm.confirmStarted(reminder.id) }) { Text("Logger gestartet – bestätigen") } })
    }
}

@Composable
fun ReminderStrip(reminder: LoggerReminder, others: Int, now: Long) {
    val remaining = ((reminder.dueAt - now + 999) / 1000).coerceAtLeast(0)
    GlassSurface {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Logger · ${reminder.referenceNumber} · ${if (remaining == 0L) "JETZT" else "%02d:%02d".format(remaining / 60, remaining % 60)}${if (others > 0) " · +$others weitere" else ""}",
                color = if (remaining == 0L) Warning else LightBlue, style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (1f - remaining / 1800f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (remaining == 0L) Warning else ElectricBlue)
        }
    }
}
