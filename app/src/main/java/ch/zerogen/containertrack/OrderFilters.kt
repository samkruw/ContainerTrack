package ch.zerogen.containertrack

enum class OrderFilter { ACTIVE, NEEDS_FILL, UPLOAD_ERROR, ALL }

fun matchesOrder(batch: JobWithContainers, query: String, filter: OrderFilter, sync: UploadJobEntity?, hasOpenReminder: Boolean = false): Boolean {
    val needle = query.trim()
    if (needle.isNotEmpty() && !batch.job.draft.referenceNumber.contains(needle,true) && batch.containers.none { it.container.containerNumber.contains(needle,true) }) return false
    return when (filter) {
        OrderFilter.ALL -> true
        OrderFilter.ACTIVE -> hasOpenReminder || (batch.job.emailSentAt==null && (batch.isManagedReturn || batch.job.exportedAt==null)) || (sync != null && sync.state != "SYNCED")
        OrderFilter.UPLOAD_ERROR -> sync?.state == "UPLOAD_ERROR"
        OrderFilter.NEEDS_FILL -> batch.job.draft.kind == JobKind.DELIVERY && batch.containers.any { item ->
            val steps = item.photos.map { it.step }.toSet()
            steps.containsAll((1..5).toSet()) && 6 !in steps
        }
    }
}

fun syncLabel(queue: UploadJobEntity?, online: Boolean): String = when {
    queue == null -> "Nur lokal"
    queue.state == "SYNCED" -> "Übertragen"
    !online -> "Wartet auf Verbindung"
    queue.state == "UPLOADING" -> "Wird hochgeladen"
    queue.state == "UPLOAD_ERROR" -> "Fehler"
    queue.lastError != null -> queue.lastError
    queue.ownerAccountId == null -> "Firmenkonto zuordnen"
    else -> "Wartet auf Upload"
}
