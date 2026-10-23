package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class PhotoShareTextTest {
    @Test fun deliveryIncludesReferenceKtAndSelectedContainers() {
        val draft=JobDraft(JobKind.DELIVERY,"ORDER-2481","2481","20 ft")
        assertEquals("Container Track · Lieferung\nKT: 2481\nAuftrag: ORDER-2481\nContainer: 0708, 0042\nContainer-Typ: 20 ft\nFotos: 6",photoShareText(draft,listOf("0708","0042","0708"),6))
    }
    @Test fun returnIncludesWorkDetailsWithoutInventingDeliveryKt() {
        val text=photoShareText(JobDraft(JobKind.RETURN,"RET-42",orderValue="40 ft",quick="Nothing",additionalWork="Empty"),listOf("0042"),1)
        assertTrue(text.contains("Auftrag: RET-42"))
        assertTrue(text.contains("Container: 0042"))
        assertTrue(text.contains("Quick: Nothing"))
        assertTrue(text.contains("Zusatzarbeiten: Empty"))
        assertFalse(text.contains("KT:"))
        assertTrue(text.endsWith("Fotos: 1"))
    }
}
