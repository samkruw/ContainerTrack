package ch.zerogen.containertrack

import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Email targets already have a chooser or explicit component; ZIP exports use Android sharing. */
fun launchShareIntent(intent: Intent, startActivity: (Intent) -> Unit) {
    startActivity(if (intent.action == Intent.ACTION_CHOOSER || intent.component != null) intent
        else Intent.createChooser(intent, "Auftrag teilen"))
}

class EmailClientUnavailable : ActivityNotFoundException(
    "Keine E-Mail-App verfügbar, die JPEG-Anhänge unterstützt. Bitte eine E-Mail-App einrichten."
)

/** mailto identifies email applications; attachments use each application's real JPEG receiver. */
fun emailPhotoIntent(context: Context, photos: List<File>, subject: String, body: String): Intent =
    emailPhotoIntent(context, photos, subject, body) { query ->
        context.packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)
    }

internal fun emailPhotoIntent(
    context: Context,
    photos: List<File>,
    subject: String,
    body: String,
    queryActivities: (Intent) -> List<ResolveInfo>
): Intent {
    val share = photoShareIntent(context, photos).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val emailPackages = queryActivities(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))
        .mapNotNull { it.activityInfo }
        .filter { it.enabled && it.exported && it.packageName != "com.microsoft.teams" }
        .map { it.packageName }
        .distinct()
    val components = emailPackages.flatMap { packageName ->
        queryActivities(Intent(share).setPackage(packageName))
            .mapNotNull { it.activityInfo }
            .filter { it.enabled && it.exported && it.packageName == packageName }
            .map { ComponentName(it.packageName, it.name) }
    }.distinct()
    if (components.isEmpty()) throw EmailClientUnavailable()

    val targets = components.map { component -> Intent(share).setComponent(component) }
    if (targets.size == 1) return targets.single()
    return Intent.createChooser(targets.first(), "E-Mail-App wählen").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.drop(1).toTypedArray())
        clipData = share.clipData
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/** A temporary, read-only grant for this ZIP; internal photos and databases stay private. */
fun zipShareIntent(context: Context, zipFile: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", zipFile)
    return Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, zipFile.nameWithoutExtension)
        clipData = ClipData.newUri(context.contentResolver, zipFile.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/** Only numbered JPEG cache copies are exposed; the source photo tree stays private. */
@Suppress("UNUSED_PARAMETER") // Kept for older callers; the argument never selects a recipient application.
fun photoShareIntent(context: Context, photos: List<File>, teamsPackage: String? = null): Intent {
    require(photos.isNotEmpty())
    val root = File(context.cacheDir,"shared-photos").canonicalFile.toPath()
    val uris = photos.map { file ->
        require(file.isFile && file.canonicalFile.toPath().startsWith(root) && isSharePhotoName(file.name))
        FileProvider.getUriForFile(context,"${context.packageName}.exports",file)
    }
    return Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/jpeg"
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM,uris.first())
        else putParcelableArrayListExtra(Intent.EXTRA_STREAM,ArrayList(uris))
        clipData = ClipData.newUri(context.contentResolver,photos.first().name,uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
