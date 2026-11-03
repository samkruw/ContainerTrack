package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class GalleryFiltersTest {
    @Test fun galleryIgnoresLegacyKtAndUsesOrderReference() {
        val job=JobEntity(id="j",draft=JobDraft(JobKind.DELIVERY,"ORDER-456","KT-123","20 ft"),createdAt=1L)
        val container=ContainerEntity(id="c",jobId="j",sequenceIndex=1,containerNumber="0708")
        val row=PhotoEntity(id="p",containerId="c",step=1,localPath="/local/p.jpg",capturedAt=1L)
        val photo=galleryPhotos(listOf(JobWithContainers(job,listOf(ContainerWithPhotos(container,listOf(row))))),emptyList()).single()
        assertTrue(photo.matches("order-456","0708",null))
        assertFalse(photo.matches("kt-123","",null))
        assertEquals("ORDER-456",photo.reference)
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
