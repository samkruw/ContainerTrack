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
