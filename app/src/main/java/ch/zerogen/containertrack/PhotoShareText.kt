package ch.zerogen.containertrack

/** Plain text remains usable when a receiving app discards captions for image attachments. */
fun photoShareText(draft:JobDraft,containers:List<String>,photoCount:Int):String = buildList {
    add("Container Track · ${draft.kind.label}")
    add("Auftrag: ${draft.referenceNumber}")
    add("Container: ${containers.distinct().joinToString(", ")}")
    add("Container-Typ: ${draft.orderValue}")
    add("Fotos: $photoCount")
}.joinToString("\n")
