package ch.zerogen.containertrack

import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class JobKind(val label: String, val photoCount: Int) {
    DELIVERY("Lieferung", 6), RETURN("Return", 1)
}

data class JobDraft(
    val kind: JobKind,
    val referenceNumber: String = "",
    val kt: String = "",
    val orderValue: String = "",
    val quantity: Int = 1,
    val quick: String = "",
    val additionalWork: String = ""
) {
    fun errors(loggerStoppedAt: Long? = null): Map<String, String> = buildMap {
        if (referenceNumber.isBlank()) put("referenceNumber", "Pflichtfeld")
        else if (!safeReference(referenceNumber)) put("referenceNumber", "Keine Schrägstriche oder Steuerzeichen verwenden.")
        if (kind == JobKind.DELIVERY && kt.isBlank()) put("kt", "Pflichtfeld")
        if (kind == JobKind.RETURN && loggerStoppedAt == null) put("loggerStoppedAt", "Logger zuerst stoppen und bestätigen.")
        if (orderValue.isBlank()) put("orderValue", "Pflichtfeld")
        if (quantity !in 1..50) put("quantity", "1 bis 50 Container")
        if (quick.isBlank()) put("quick", "Pflichtfeld – Nothing und Empty sind erlaubt.")
        if (additionalWork.isBlank()) put("additionalWork", "Pflichtfeld – Nothing und Empty sind erlaubt.")
    }
}

fun safeReference(value: String): Boolean = value.trim().let {
    it.isNotEmpty() && it.length <= 100 && it != "." && it != ".." && it.none { c -> c == '/' || c == '\\' || c.code < 32 || c.code == 127 }
}

fun validContainerNumber(value: String) = Regex("[0-9]{4}").matches(value)

fun containerError(value: String, existing: Set<String>): String? = when {
    !validContainerNumber(value) -> "Exakt 4 Ziffern eingeben."
    value in existing -> "Container bereits erfasst."
    else -> null
}

fun firstContainerNumber(candidates: List<String>): String? {
    for (candidate in candidates) {
        val normalized = candidate.uppercase(Locale.ROOT).map { c ->
            when (c) { 'O', 'Q', 'D' -> '0'; 'I', 'L' -> '1'; 'S' -> '5'; 'B' -> '8'; else -> c }
        }.filter { it in '0'..'9' }.joinToString("")
        if (validContainerNumber(normalized)) return normalized
    }
    return null
}

val deliveryPhotoLabels = listOf("Leerer Innenraum", "Aussenseite – Front", "Aussenseite – Rückseite / Türen", "Aussenseite – links", "Aussenseite – rechts", "Befüllter Innenraum")
fun photoLabel(kind: JobKind, step: Int) = if (kind == JobKind.RETURN) "Inhalt bei Return" else deliveryPhotoLabels[step - 1]
fun nextPhotoStep(kind: JobKind, completed: Set<Int>): Int? = (1..kind.photoCount).firstOrNull { it !in completed }
fun photoName(number: String, step: Int): String {
    require(validContainerNumber(number) && step in 1..6)
    return "${number}_${step.toString().padStart(2, '0')}.jpg"
}
fun firstExportAt(existing: Long?, now: Long) = existing ?: now
fun reminderDueAt(exportedAt: Long) = exportedAt + 30 * 60 * 1000L
fun cleanCell(value: String) = value.replace("\r\n", "\n").replace('\r', '\n').replace("\n", " / ").replace('\t', ' ')

fun excelText(draft: JobDraft, numbers: List<String>, date: LocalDate): String {
    val header = "Datum\tIn/Out\tKT\tOrder\tQuantity\tadditional Work\tContainer Number\tQuick"
    val row = listOf(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        if (draft.kind == JobKind.DELIVERY) "OUT" else "IN",
        if (draft.kind == JobKind.DELIVERY) draft.kt else draft.referenceNumber,
        draft.orderValue, draft.quantity.toString(), draft.additionalWork, numbers.joinToString(","), draft.quick)
    return "$header\r\n${row.joinToString("\t") { cleanCell(it) }}\r\n"
}

data class ExportPhoto(val step: Int, val file: File)
data class ExportContainer(val number: String, val photos: List<ExportPhoto>)

fun writeExportZip(output: OutputStream, draft: JobDraft, containers: List<ExportContainer>, metadata: String, date: LocalDate) {
    require(safeReference(draft.referenceNumber))
    require(containers.size == draft.quantity && containers.map { it.number }.distinct().size == containers.size)
    require(containers.all { c -> validContainerNumber(c.number) &&
        c.photos.map { it.step }.sorted() == (1..draft.kind.photoCount).toList() &&
        c.photos.all { it.file.isFile && it.file.length() > 0 } }) { "Auftrag unvollständig." }
    val root = draft.referenceNumber.trim()
    ZipOutputStream(output).use { zip ->
        for (container in containers) for (photo in container.photos.sortedBy { it.step }) {
            zip.putNextEntry(ZipEntry("$root/${container.number}/${photoName(container.number, photo.step)}"))
            photo.file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        for ((name, text) in listOf("${root}_EXCEL.txt" to excelText(draft, containers.map { it.number }, date), "${root}_META.json" to metadata)) {
            zip.putNextEntry(ZipEntry("$root/$name"))
            zip.write(text.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}
