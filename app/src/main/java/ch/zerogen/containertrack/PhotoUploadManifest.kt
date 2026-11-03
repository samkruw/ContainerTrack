package ch.zerogen.containertrack

import java.io.File

data class UploadCandidate(val photoId: String, val jobId: String, val file: File, val remotePath: String, val fileName: String)
const val MAX_PHOTO_UPLOAD_BYTES = 250L * 1024 * 1024

fun uploadManifest(batch: JobWithContainers): List<UploadCandidate> {
    require(batch.complete) { "Auftrag unvollständig." }
    require(Regex("[A-Za-z0-9-]{1,64}").matches(batch.job.id)) { "Ungültige Auftrags-ID." }
    return batch.ordered.flatMap { container ->
        container.photos.sortedBy { it.step }.map { photo ->
            val name = photoName(container.container.containerNumber,photo.step)
            val file = File(photo.localPath)
            require(file.isFile && file.length() in 1..MAX_PHOTO_UPLOAD_BYTES) { "Bild fehlt oder überschreitet das Uploadlimit." }
            UploadCandidate(photo.id,batch.job.id,file,"${batch.job.id}/${container.container.containerNumber}/$name",name)
        }
    }
}
