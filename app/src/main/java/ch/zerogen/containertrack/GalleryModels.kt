package ch.zerogen.containertrack

data class GalleryPhoto(val photoId: String, val jobId: String, val reference: String, val containerNumber: String,
    val step: Int, val localPath: String, val uploaderAccountId: String?, val orderReference: String = reference) {
    fun matches(referenceQuery: String, containerQuery: String, uploader: String?): Boolean =
        (reference.contains(referenceQuery.trim(),true) || orderReference.contains(referenceQuery.trim(),true)) && containerNumber.contains(containerQuery.trim()) &&
        (uploader == null || uploaderAccountId == uploader)
}

fun galleryPhotos(jobs: List<JobWithContainers>, photoUploads: List<UploadPhotoEntity>): List<GalleryPhoto> {
    val confirmed = photoUploads.filter { it.uploadedAt != null }.associateBy { it.photoId }
    return jobs.flatMap { batch -> batch.ordered.flatMap { item -> item.photos.sortedBy { it.step }.map { photo ->
        GalleryPhoto(photo.id,batch.job.id,batch.job.draft.kt.ifBlank { batch.job.draft.referenceNumber },item.container.containerNumber,
            photo.step,photo.localPath,confirmed[photo.id]?.uploadedByAccountId,batch.job.draft.referenceNumber)
    } } }
}
