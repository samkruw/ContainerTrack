package ch.zerogen.containertrack

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EmailPhotoSharingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun receiver(packageName: String, name: String = "$packageName.ComposeActivity") = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            this.name = name
            exported = true
            enabled = true
        }
    }

    private fun withPhotos(names: List<String>, block: (List<File>) -> Unit) {
        val directory = File(context.cacheDir, "shared-photos/email-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            block(names.mapIndexed { index, name -> File(directory, name).apply { writeBytes(byteArrayOf(index.toByte(), 42)) } })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Suppress("DEPRECATION")
    private fun streams(intent: Intent): List<Uri> = if (intent.action == Intent.ACTION_SEND) {
        listOf(requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)))
    } else requireNotNull(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))

    private fun assertReadOnlyGrants(intent: Intent, expectedUris: List<Uri>) {
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertEquals(expectedUris.size, intent.clipData!!.itemCount)
        expectedUris.forEachIndexed { index, uri -> assertEquals(uri, intent.clipData!!.getItemAt(index).uri) }
    }

    @Test fun singleAttachmentTargetsOnlyAnEmailAppThatAcceptsJpeg() = withPhotos(listOf("0708_vorher_01_inhalt.jpg")) { photos ->
        val intent = emailPhotoIntent(context, photos, "ORDER-2481", "Auftrag: ORDER-2481\nContainer: 0708") { query ->
            when (query.action) {
                Intent.ACTION_SENDTO -> {
                    assertEquals("mailto", query.data!!.scheme)
                    listOf(receiver("mail.example"), receiver("com.microsoft.teams"), receiver("mail.noimages"))
                }
                Intent.ACTION_SEND -> {
                    assertEquals("image/jpeg", query.type)
                    when (query.`package`) {
                        "mail.example" -> listOf(receiver("mail.example"), receiver("chat.example"))
                        "mail.noimages" -> emptyList()
                        else -> error("A non-email package was queried: ${query.`package`}")
                    }
                }
                else -> error("Unexpected email discovery action: ${query.action}")
            }
        }
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals(ComponentName("mail.example", "mail.example.ComposeActivity"), intent.component)
        assertEquals("image/jpeg", intent.type)
        assertEquals("ORDER-2481", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Auftrag: ORDER-2481\nContainer: 0708", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertFalse(intent.hasExtra(Intent.EXTRA_EMAIL))
        assertFalse(intent.hasExtra(Intent.EXTRA_CC))
        assertFalse(intent.hasExtra(Intent.EXTRA_BCC))
        assertReadOnlyGrants(intent, streams(intent))
        val launches = mutableListOf<Intent>()
        launchShareIntent(intent) { launches += it }
        assertEquals(listOf(intent), launches)
    }

    @Suppress("DEPRECATION")
    @Test fun emailChooserPreservesAllNamedJpegsAndGrantsInEveryExplicitTarget() = withPhotos(
        listOf("0708_vorher_02_schaden.jpg", "0708_vorher_02_zusatz.jpg", "0708_nachher_01_leer_sauber.jpg", "0708_01.jpg")
    ) { photos ->
        val chooser = emailPhotoIntent(context, photos, "ORDER-2481", "Vier ausgewählte Fotos") { query ->
            when (query.action) {
                Intent.ACTION_SENDTO -> listOf(receiver("mail.first"), receiver("mail.second"), receiver("com.microsoft.teams"))
                Intent.ACTION_SEND_MULTIPLE -> {
                    assertEquals("image/jpeg", query.type)
                    when (query.`package`) {
                        "mail.first" -> listOf(receiver("mail.first"), receiver("mail.first"))
                        "mail.second" -> listOf(receiver("mail.second"))
                        else -> error("A non-email package was queried: ${query.`package`}")
                    }
                }
                else -> error("Unexpected discovery query: ${query.action}")
            }
        }
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val targets = listOf(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!) +
            chooser.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS)!!.map { it as Intent }
        assertEquals(setOf("mail.first", "mail.second"), targets.map { it.component!!.packageName }.toSet())
        assertEquals(2, targets.size)
        val expectedUris = streams(targets.first())
        assertReadOnlyGrants(chooser, expectedUris)
        targets.forEach { target ->
            assertEquals(Intent.ACTION_SEND_MULTIPLE, target.action)
            assertEquals("image/jpeg", target.type)
            assertEquals("ORDER-2481", target.getStringExtra(Intent.EXTRA_SUBJECT))
            assertEquals("Vier ausgewählte Fotos", target.getStringExtra(Intent.EXTRA_TEXT))
            assertFalse(target.hasExtra(Intent.EXTRA_EMAIL))
            assertEquals(expectedUris, streams(target))
            assertReadOnlyGrants(target, expectedUris)
        }
        expectedUris.forEachIndexed { index, uri ->
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.exports", uri.authority)
            assertEquals("image/jpeg", context.contentResolver.getType(uri))
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(photos[index].name, cursor.getString(0))
            }
            context.contentResolver.openInputStream(uri)!!.use { assertArrayEquals(byteArrayOf(index.toByte(), 42), it.readBytes()) }
        }
        val launches = mutableListOf<Intent>()
        launchShareIntent(chooser) { launches += it }
        assertEquals(listOf(chooser), launches)
    }

    @Test fun absentMailClientsAndMailClientsWithoutJpegSupportFailWithoutGenericFallback() = withPhotos(listOf("0708_01.jpg")) { photos ->
        for (emailReceivers in listOf(emptyList(), listOf(receiver("mail.noimages")), listOf(receiver("com.microsoft.teams")))) {
            val failure = assertThrows(EmailClientUnavailable::class.java) {
                emailPhotoIntent(context, photos, "ORDER-2481", "Foto") { query ->
                    if (query.action == Intent.ACTION_SENDTO) emailReceivers else emptyList()
                }
            }
            assertTrue(failure.message!!.contains("E-Mail"))
        }
    }

    @Test fun photoValidationRejectsPrivateSourcesAndNonPhotoCacheFiles() = withPhotos(listOf("0708_01.jpg")) { photos ->
        val privatePhoto = File.createTempFile("private-email", ".jpg", context.filesDir)
        val metadata = File(photos.single().parentFile, "META.json").apply { writeText("{}") }
        try {
            for (files in listOf(emptyList(), listOf(privatePhoto), listOf(metadata))) {
                assertThrows(IllegalArgumentException::class.java) {
                    emailPhotoIntent(context, files, "ORDER-2481", "Foto") { error("Invalid files reached email discovery") }
                }
            }
        } finally {
            privatePhoto.delete()
        }
    }

    @Test fun legacyTeamsArgumentDoesNotSelectTeams() = withPhotos(listOf("0708_01.jpg")) { photos ->
        val intent = photoShareIntent(context, photos, "com.microsoft.teams")
        assertNull(intent.`package`)
        assertNull(intent.component)
    }

    @Suppress("DEPRECATION")
    @Test fun genericZipStillReceivesExactlyOneChooser() {
        val original = Intent(Intent.ACTION_SEND).setType("application/zip")
        val launches = mutableListOf<Intent>()
        launchShareIntent(original) { launches += it }
        assertEquals(1, launches.size)
        assertEquals(Intent.ACTION_CHOOSER, launches.single().action)
        assertEquals(original, launches.single().getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
    }
}
