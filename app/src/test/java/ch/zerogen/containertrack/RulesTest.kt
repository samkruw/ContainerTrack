package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipInputStream

class RulesTest {
    private fun draft(kind: JobKind = JobKind.DELIVERY) = JobDraft(
        kind, "ORD-12", "ZRH-8", "20 ft", 1, "Nothing", "Empty"
    )

    @Test fun requiredFieldsAndQuantity() {
        assertTrue(draft().errors().isEmpty())
        for (n in listOf(0, 51)) assertTrue(draft().copy(quantity = n).errors().containsKey("quantity"))
        for (n in listOf(1, 50)) assertTrue(draft().copy(quantity = n).errors().isEmpty())
        assertTrue(draft().copy(referenceNumber = " ").errors().containsKey("referenceNumber"))
        assertTrue(draft().copy(kt = " ").errors().containsKey("kt"))
        assertTrue(draft().copy(orderValue = " ").errors().containsKey("orderValue"))
        assertTrue(draft(JobKind.RETURN).copy(quick = "\n").errors(123L).containsKey("quick"))
        assertTrue(draft(JobKind.RETURN).copy(additionalWork = "").errors(123L).containsKey("additionalWork"))
    }

    @Test fun deliveryNeedsNeitherQuickNorAdditionalWork() {
        assertTrue(draft().copy(quick = "", additionalWork = "").errors().isEmpty())
    }

    @Test fun returnRequiresBothExtraFieldsEvenAfterLoggerStop() {
        val errors = draft(JobKind.RETURN).copy(quick = "", additionalWork = "").errors(123L)
        assertEquals(setOf("quick", "additionalWork"), errors.keys)
    }

    @Test fun returnRequiresLoggerConfirmationButNotSeparateZrh() {
        assertTrue(draft(JobKind.RETURN).copy(kt = "").errors().containsKey("loggerStoppedAt"))
        assertTrue(draft(JobKind.RETURN).copy(kt = "").errors(123L).isEmpty())
    }

    @Test fun unsafeReferenceIsRejected() {
        for (value in listOf("..", "a/b", "a\\b", "x\ny", "x\ty"))
            assertTrue(draft().copy(referenceNumber = value).errors().containsKey("referenceNumber"))
    }

    @Test fun ocrPreservesZerosAndTakesFirstValidToken() {
        assertEquals("0708", firstContainerNumber(listOf("0708", "0710")))
        assertEquals("0105", firstContainerNumber(listOf("O1OS")))
        assertEquals("0708", firstContainerNumber(listOf("12345", "0708")))
        assertNull(firstContainerNumber(listOf("12345", "123", "HELLO")))
        assertFalse(validContainerNumber(" 0708"))
        assertFalse(validContainerNumber("٠٧٠٨"))
    }

    @Test fun duplicatesAreJobLocal() {
        assertEquals("Container bereits erfasst.", containerError("0708", setOf("0708")))
        assertNull(containerError("0708", emptySet()))
        assertNotNull(containerError("708", emptySet()))
    }

    @Test fun photoSequenceCannotSkipOrExceedWorkflow() {
        assertEquals(1, nextPhotoStep(JobKind.DELIVERY, emptySet()))
        assertEquals(3, nextPhotoStep(JobKind.DELIVERY, setOf(1, 2)))
        assertEquals(2, nextPhotoStep(JobKind.DELIVERY, setOf(1, 3)))
        assertNull(nextPhotoStep(JobKind.DELIVERY, (1..6).toSet()))
        assertEquals(1, nextPhotoStep(JobKind.RETURN, emptySet()))
        assertNull(nextPhotoStep(JobKind.RETURN, setOf(1)))
        assertEquals("0708_06.jpg", photoName("0708", 6))
    }

    @Test fun txtDeliveryUsesZrhAndContainerType() {
        val txt = excelText(draft().copy(quantity = 2), listOf("0708", "0710"), LocalDate.of(2026, 10, 20))
        assertEquals("Datum\tIn/Out\tKT\tOrder\tQuantity\tadditional Work\tContainer Number\tQuick\r\n" +
            "20.10.2026\tOUT\tZRH-8\t20 ft\t2\t\t0708,0710\t\r\n", txt)
    }

    @Test fun txtReturnUsesReferenceAndNormalizesNewlines() {
        val txt = excelText(draft(JobKind.RETURN).copy(quick = "A\tB\r\nC"), listOf("0708"), LocalDate.of(2026, 1, 2))
        assertTrue(txt.contains("02.01.2026\tIN\tORD-12\t20 ft\t1"))
        assertTrue(txt.endsWith("\tEmpty\t0708\tA B / C\r\n"))
    }

    @Test fun timerNeverResets() {
        assertEquals(1_801_000L, reminderDueAt(1_000L))
        assertEquals(1_000L, firstExportAt(1_000L, 9_000L))
        assertEquals(9_000L, firstExportAt(null, 9_000L))
    }

    @Test fun zipContainsRequiredPathsAndUtf8Text() {
        val file = File.createTempFile("photo", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            val out = ByteArrayOutputStream()
            writeExportZip(out, draft(JobKind.RETURN), listOf(ExportContainer("0708", listOf(ExportPhoto(1, file)))), "{}", LocalDate.of(2026, 1, 2))
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(out.toByteArray().inputStream()).use { zip ->
                while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes() }
            }
            assertEquals(setOf("ORD-12/0708/0708_01.jpg", "ORD-12/ORD-12_EXCEL.txt", "ORD-12/ORD-12_META.json"), entries.keys)
            assertArrayEquals(byteArrayOf(1, 2, 3), entries.getValue("ORD-12/0708/0708_01.jpg"))
            assertTrue(entries.getValue("ORD-12/ORD-12_EXCEL.txt").toString(Charsets.UTF_8).contains("\tIN\tORD-12\t"))
        } finally { file.delete() }
    }

    @Test fun incompleteExportFailsBeforeWritingAnything() {
        val out = ByteArrayOutputStream()
        try {
            writeExportZip(out, draft(), listOf(ExportContainer("0708", emptyList())), "{}", LocalDate.now())
            fail("Incomplete batch accepted")
        } catch (_: IllegalArgumentException) { assertEquals(0, out.size()) }
    }
}
