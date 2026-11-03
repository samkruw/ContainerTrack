package ch.zerogen.containertrack

data class GalleryPhoto(val photoId: String, val jobId: String, val reference: String, val containerNumber: String,
    val step: Int, val localPath: String, val uploaderAccountId: String?, val orderReference: String = reference,
    val fileName: String = "${containerNumber}_${step.toString().padStart(2, '0')}.jpg", val label: String = "Foto $step") {
    fun matches(referenceQuery: String, containerQuery: String, uploader: String?): Boolean =
        (reference.contains(referenceQuery.trim(),true) || orderReference.contains(referenceQuery.trim(),true)) && containerNumber.contains(containerQuery.trim()) &&
        (uploader == null || uploaderAccountId == uploader)
}

fun galleryPhotos(jobs: List<JobWithContainers>, photoUploads: List<UploadPhotoEntity>): List<GalleryPhoto> {
    val confirmed = photoUploads.filter { it.uploadedAt != null }.associateBy { it.photoId }
    return jobs.flatMap { batch -> namedPhotos(batch).map { named ->
        val photo = named.photo
        GalleryPhoto(photo.id, batch.job.id, batch.job.draft.referenceNumber, named.containerNumber,
            photo.step, photo.localPath, confirmed[photo.id]?.uploadedByAccountId, batch.job.draft.referenceNumber,
            named.fileName, named.label)
    } }
}

fun selectedGalleryPhotos(rows: List<GalleryPhoto>, selected: Set<String>): List<GalleryPhoto> =
    rows.filter { it.photoId in selected && java.io.File(it.localPath).isFile }.distinctBy { it.photoId }
