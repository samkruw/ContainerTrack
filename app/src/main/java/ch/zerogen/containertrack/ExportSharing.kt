package ch.zerogen.containertrack

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

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
fun photoShareIntent(context: Context, photos: List<File>, teamsPackage: String? = null): Intent {
    require(photos.isNotEmpty())
    require(teamsPackage == null || teamsPackage == "com.microsoft.teams")
    val root = File(context.cacheDir,"shared-photos").canonicalFile.toPath()
    val uris = photos.map { file ->
        require(file.isFile && file.canonicalFile.toPath().startsWith(root) && Regex("[0-9]{4}_0[1-6]\\.jpg").matches(file.name))
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
        if (teamsPackage != null) {
            setPackage(teamsPackage)
            if (resolveActivity(context.packageManager) == null) setPackage(null)
        }
    }
}
