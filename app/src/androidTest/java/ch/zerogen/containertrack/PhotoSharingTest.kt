package ch.zerogen.containertrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PhotoSharingTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Suppress("DEPRECATION")
    @Test fun genericJpegUsesOneChooserAndPreservesOriginalGrants() {
        val uri=Uri.parse("content://${context.packageName}.exports/shared-photos/0708_01.jpg")
        val intent=Intent(Intent.ACTION_SEND).setType("image/jpeg")
            .putExtra(Intent.EXTRA_TEXT,"Auftrag: ORDER-2481\nContainer: 0708").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData=android.content.ClipData.newRawUri("0708_01.jpg",uri)
        val launches=mutableListOf<Intent>()
        launchShareIntent(intent) { launches += it }
        assertEquals(1,launches.size)
        assertEquals(Intent.ACTION_CHOOSER,launches.single().action)
        val target=launches.single().getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertNull(target.`package`)
        assertNull(target.component)
        assertEquals("Auftrag: ORDER-2481\nContainer: 0708",target.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(uri,target.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(uri,target.clipData!!.getItemAt(0).uri)
        assertTrue(target.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
        assertEquals(0,target.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    @Suppress("DEPRECATION")
    @Test fun onlyNumberedJpegsHaveReadOnlyProviderGrants() {
        val directory=File(context.cacheDir,"shared-photos/test-${UUID.randomUUID()}").apply{mkdirs()}
        try {
            val photos=listOf("0708_01.jpg","0708_02.jpg").map{File(directory,it).apply{writeBytes(byteArrayOf(1,2,3))}}
            val intent=photoShareIntent(context,photos,null)
            assertEquals(Intent.ACTION_SEND_MULTIPLE,intent.action);assertEquals("image/jpeg",intent.type)
            val uris=intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!
            assertEquals(2,uris.size);assertEquals(2,intent.clipData!!.itemCount)
            for((index,uri) in uris.withIndex()) {
                assertEquals(photos[index].name,uri.lastPathSegment)
                context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)!!.use { cursor->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(photos[index].name,cursor.getString(0))
                }
                assertEquals("content",uri.scheme);assertEquals("image/jpeg",context.contentResolver.getType(uri))
                context.contentResolver.openInputStream(uri)!!.use {assertArrayEquals(byteArrayOf(1,2,3),it.readBytes())}
            }
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
            assertEquals(0,intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            assertEquals(Intent.ACTION_SEND,photoShareIntent(context,photos.take(1),null).action)
            assertThrows(IllegalArgumentException::class.java){photoShareIntent(context,listOf(File(directory,"META.json").apply{writeText("{}")}),null)}
            val outside=File.createTempFile("private", ".jpg",context.filesDir)
            try {assertThrows(IllegalArgumentException::class.java){photoShareIntent(context,listOf(outside),null)}}finally{outside.delete()}
        } finally{directory.deleteRecursively()}
    }
}
