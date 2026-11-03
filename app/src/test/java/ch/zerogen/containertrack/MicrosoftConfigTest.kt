package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class MicrosoftConfigTest {
    private val valid = MicrosoftConfig("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222",
        "msauth://ch.zerogen.containertrack/registered%2Bhash%3D", "33333333-3333-3333-3333-333333333333", "19:channel@thread.tacv2")
    @Test fun emptyConfigurationDoesNotInventAnAccount() { assertFalse(MicrosoftConfig().configured) }
    @Test fun onlyTenantScopedNativeRedirectIsAccepted() {
        assertTrue(valid.configured)
        assertFalse(valid.copy(tenantId="common").configured)
        assertFalse(valid.copy(redirectUri="https://example.invalid/redirect").configured)
        assertFalse(valid.copy(redirectUri="msauth://another.app/hash").configured)
        assertFalse(valid.copy(channelId="19:bad\nchannel").configured)
    }
    @Test fun accountMismatchFailsClosed() {
        assertTrue(accountMatches("person-a","person-a"))
        assertFalse(accountMatches("person-a","person-b"))
        assertFalse(accountMatches("person-a",null))
        assertFalse(accountMatches("",null))
    }
}
