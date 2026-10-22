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
    @Test fun onlyNumberedJpegsHaveReadOnlyProviderGrants() {
        val directory=File(context.cacheDir,"shared-photos/test-${UUID.randomUUID()}").apply{mkdirs()}
        try {
            val photos=listOf("0708_01.jpg","0708_02.jpg").map{File(directory,it).apply{writeBytes(byteArrayOf(1,2,3))}}
            val intent=photoShareIntent(context,photos,null)
            assertEquals(Intent.ACTION_SEND_MULTIPLE,intent.action);assertEquals("image/jpeg",intent.type)
            val uris=intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!
            assertEquals(2,uris.size);assertEquals(2,intent.clipData!!.itemCount)
            for(uri in uris) {
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
