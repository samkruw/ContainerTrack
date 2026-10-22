package ch.zerogen.containertrack
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class PhotoSelectionTest {
    @Test fun selectionContainsOnlyRequestedExistingLocalPhotos() {
        val file=File.createTempFile("selected", ".jpg").apply { writeBytes(byteArrayOf(1)) }
        try {
            val first=GalleryPhoto("p1","j","KT 2481","0708",1,file.path,null)
            val second=first.copy(photoId="p2",step=2)
            val missing=first.copy(photoId="missing",localPath=file.path+"-missing")
            assertEquals(listOf(second),selectedGalleryPhotos(listOf(first,second,missing),setOf("p2","missing","stale")))
            assertTrue(selectedGalleryPhotos(listOf(first),emptySet()).isEmpty())
        } finally { file.delete() }
    }
}
