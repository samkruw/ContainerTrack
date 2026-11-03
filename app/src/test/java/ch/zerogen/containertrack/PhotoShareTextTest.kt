package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class PhotoShareTextTest {
    @Test fun deliveryIncludesReferenceAndSelectedContainersWithoutKt() {
        val draft=JobDraft(JobKind.DELIVERY,"ORDER-2481","2481","20 ft")
        assertEquals("Container Track · Lieferung\nAuftrag: ORDER-2481\nContainer: 0708, 0042\nContainer-Typ: 20 ft\nFotos: 6",photoShareText(draft,listOf("0708","0042","0708"),6))
    }
    @Test fun returnOmitsLegacyExtrasWithoutInventingDeliveryKt() {
        val text=photoShareText(JobDraft(JobKind.RETURN,"RET-42",orderValue="40 ft",quick="LEGACY-QUICK",additionalWork="LEGACY-WORK"),listOf("0042"),1)
        assertEquals("Container Track · Return\nAuftrag: RET-42\nContainer: 0042\nContainer-Typ: 40 ft\nFotos: 1", text)
    }
}
