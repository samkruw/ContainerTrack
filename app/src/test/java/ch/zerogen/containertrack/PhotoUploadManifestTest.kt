package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PhotoUploadManifestTest {
    @Test fun onlyNumberedCurrentJpegsAreCandidates() {
        val file=File.createTempFile("photo", ".jpg").apply {writeBytes(byteArrayOf(1))}
        try {
            val job=JobEntity(id="job-one",draft=JobDraft(JobKind.DELIVERY,"LONG:REF?","ZRH","20 ft",1),createdAt=1L)
            val container=ContainerEntity(id="container",jobId=job.id,sequenceIndex=1,containerNumber="0708")
            val photos=(1..6).map {PhotoEntity(id="p$it",containerId=container.id,step=it,localPath=file.path,capturedAt=1L)}
            val batch=JobWithContainers(job,listOf(ContainerWithPhotos(container,photos)))
            val candidates=uploadManifest(batch)
            assertEquals((1..6).map {"0708_0$it.jpg"},candidates.map{it.fileName})
            assertEquals(candidates.map{it.remotePath},uploadManifest(batch).map{it.remotePath})
            assertTrue(candidates.all{it.remotePath.startsWith("job-one/0708/")})
            assertFalse(candidates.any{it.fileName.endsWith("zip") || it.fileName.contains("META")})
            assertNotEquals(candidates.first().remotePath,uploadManifest(batch.copy(job=job.copy(id="job-two"))).first().remotePath)
            assertThrows(IllegalArgumentException::class.java) { uploadManifest(batch.copy(containers=listOf(ContainerWithPhotos(container,photos.take(5))))) }
            val returned=batch.copy(job=job.copy(draft=job.draft.copy(kind=JobKind.RETURN)),containers=listOf(ContainerWithPhotos(container,photos.take(1))))
            assertEquals(listOf("0708_01.jpg"),uploadManifest(returned).map{it.fileName})
        } finally {file.delete()}
    }
}
