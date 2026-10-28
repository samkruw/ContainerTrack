package ch.zerogen.containertrack

enum class ReturnPhotoPhase { INITIAL, CLEAN }
enum class ReturnPhotoCategory { CONTENT, DAMAGE, OTHER, CLEAN }
enum class ReminderKind { LOGGER, RETURN_CLOSE }

data class ReturnProgress(
    val initialCount: Int,
    val cleanCount: Int,
    val hasContent: Boolean,
    val complete: Boolean
)

internal data class ReturnPhotoInfo(val step: Int, val phase: ReturnPhotoPhase, val category: ReturnPhotoCategory)

private fun PhotoEntity.returnInfo() = ReturnPhotoInfo(step, returnPhase, returnCategory)

private fun validReturnPhotoInfo(photos: List<ReturnPhotoInfo>) =
    photos.all { it.step > 0 && validReturnCategory(it.phase, it.category) } &&
        photos.map { it.step }.distinct().size == photos.size

private fun hasInitialDocumentation(photos: List<ReturnPhotoInfo>) =
    validReturnPhotoInfo(photos) &&
        photos.count { it.phase == ReturnPhotoPhase.INITIAL && it.category == ReturnPhotoCategory.CONTENT } == 1

internal fun returnProgressFor(photos: List<ReturnPhotoInfo>): ReturnProgress {
    val initialCount = photos.count { it.phase == ReturnPhotoPhase.INITIAL }
    val cleanCount = photos.count { it.phase == ReturnPhotoPhase.CLEAN }
    val hasContent = photos.any { it.phase == ReturnPhotoPhase.INITIAL && it.category == ReturnPhotoCategory.CONTENT }
    return ReturnProgress(initialCount, cleanCount, hasContent,
        hasInitialDocumentation(photos) && initialCount == cleanCount)
}

fun returnProgress(photos: List<PhotoEntity>): ReturnProgress = returnProgressFor(photos.map { it.returnInfo() })

val JobWithContainers.isManagedReturn: Boolean
    get() = job.draft.kind == JobKind.RETURN && job.returnWorkflowVersion >= 1

val JobWithContainers.initialDocumentationComplete: Boolean
    get() = isManagedReturn && job.draft.quantity in 1..50 && containers.size == job.draft.quantity &&
        containers.all { hasInitialDocumentation(it.photos.map { photo -> photo.returnInfo() }) }

fun returnReminderDueAt(createdAt: Long): Long = createdAt + 24 * 60 * 60 * 1000L

private fun validReturnCategory(phase: ReturnPhotoPhase, category: ReturnPhotoCategory) = when (phase) {
    ReturnPhotoPhase.INITIAL -> category in setOf(ReturnPhotoCategory.CONTENT, ReturnPhotoCategory.DAMAGE, ReturnPhotoCategory.OTHER)
    ReturnPhotoPhase.CLEAN -> category == ReturnPhotoCategory.CLEAN
}

fun returnPhotoFileName(number: String, phase: ReturnPhotoPhase, category: ReturnPhotoCategory, ordinal: Int): String {
    require(validContainerNumber(number) && ordinal >= 1 && validReturnCategory(phase, category))
    val phaseName = if (phase == ReturnPhotoPhase.INITIAL) "vorher" else "nachher"
    val categoryName = when (category) {
        ReturnPhotoCategory.CONTENT -> "inhalt"
        ReturnPhotoCategory.DAMAGE -> "schaden"
        ReturnPhotoCategory.OTHER -> "zusatz"
        ReturnPhotoCategory.CLEAN -> "leer_sauber"
    }
    return "${number}_${phaseName}_${ordinal.toString().padStart(2, '0')}_${categoryName}.jpg"
}

fun returnPhotoLabel(phase: ReturnPhotoPhase, category: ReturnPhotoCategory): String {
    require(validReturnCategory(phase, category))
    return when (category) {
        ReturnPhotoCategory.CONTENT -> "Inhalt bei Return"
        ReturnPhotoCategory.DAMAGE -> "Schaden bei Return"
        ReturnPhotoCategory.OTHER -> "Zusatzfoto bei Return"
        ReturnPhotoCategory.CLEAN -> "Leer / sauber"
    }
}

data class NamedPhoto(val containerNumber: String, val photo: PhotoEntity, val fileName: String, val label: String)

internal fun <T> namedReturnPhotos(number: String, photos: List<T>, info: (T) -> ReturnPhotoInfo): List<Pair<T, String>> {
    val ordered = photos.sortedWith(compareBy<T> { info(it).phase.ordinal }
        .thenBy { if (info(it).category == ReturnPhotoCategory.CONTENT) 0 else 1 }
        .thenBy { info(it).step })
    var initialOrdinal = 0
    var cleanOrdinal = 0
    return ordered.map { photo ->
        val details = info(photo)
        val ordinal = if (details.phase == ReturnPhotoPhase.INITIAL) ++initialOrdinal else ++cleanOrdinal
        photo to returnPhotoFileName(number, details.phase, details.category, ordinal)
    }
}

fun namedPhotos(batch: JobWithContainers): List<NamedPhoto> = batch.ordered.flatMap { item ->
    val number = item.container.containerNumber
    if (batch.isManagedReturn) namedReturnPhotos(number, item.photos) { it.returnInfo() }.map { (photo, name) ->
        NamedPhoto(number, photo, name, returnPhotoLabel(photo.returnPhase, photo.returnCategory))
    } else item.photos.sortedBy { it.step }.map { photo ->
        NamedPhoto(number, photo, photoName(number, photo.step), photoLabel(batch.job.draft.kind, photo.step))
    }
}

private val sharePhotoName = Regex(
    "[0-9]{4}_(?:0[1-6]|vorher_(?:0[1-9]|[1-9][0-9]+)_(?:inhalt|schaden|zusatz)|nachher_(?:0[1-9]|[1-9][0-9]+)_leer_sauber)\\.jpg"
)

fun isSharePhotoName(name: String): Boolean = sharePhotoName.matches(name)
