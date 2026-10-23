package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class GalleryFiltersTest {
    @Test fun gallerySearchIncludesOrderWhenKtDiffers() {
        val photo=GalleryPhoto("p","j","KT-123","0708",1,"/local/p.jpg",null,"ORDER-456")
        assertTrue(photo.matches("order-456","0708",null))
        assertTrue(photo.matches("kt-123","",null))
        assertFalse(photo.matches("other","",null))
    }
    @Test fun galleryOnlyMapsRealPhotoRowsAndConfirmedUploaders() {
        val job=JobEntity(id="j",draft=JobDraft(JobKind.RETURN,"RET",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),createdAt=1L)
        val c=ContainerEntity(id="c",jobId="j",sequenceIndex=1,containerNumber="0708")
        val p=PhotoEntity(id="p",containerId="c",step=1,localPath="/real/internal/photo.jpg",capturedAt=1L)
        val batch=JobWithContainers(job,listOf(ContainerWithPhotos(c,listOf(p))))
        assertTrue(galleryPhotos(emptyList(),emptyList()).isEmpty())
        assertNull(galleryPhotos(listOf(batch),listOf(UploadPhotoEntity("p","j","path",uploadedByAccountId="not-confirmed"))).single().uploaderAccountId)
        val photo=galleryPhotos(listOf(batch),listOf(UploadPhotoEntity("p","j","path",uploadedAt=2L,uploadedByAccountId="account"))).single()
        assertEquals("0708",photo.containerNumber);assertEquals(p.localPath,photo.localPath)
        assertEquals("account",photo.uploaderAccountId)
        assertTrue(photo.matches("ret","0708",null))
        assertFalse(photo.matches("ret","708", "another-account"))
    }
}
