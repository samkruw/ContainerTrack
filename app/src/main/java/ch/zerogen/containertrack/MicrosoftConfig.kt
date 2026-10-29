package ch.zerogen.containertrack

import java.net.URI

data class MicrosoftConfig(
    val tenantId: String = "", val clientId: String = "", val redirectUri: String = "",
    val teamId: String = "", val channelId: String = ""
) {
    val configured: Boolean get() {
        val uuid = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
        val redirect = runCatching { URI(redirectUri) }.getOrNull() ?: return false
        return uuid.matches(tenantId) && uuid.matches(clientId) && uuid.matches(teamId) &&
            channelId.isNotBlank() && channelId.length <= 256 && channelId.none { it.code < 32 || it == '/' } &&
            redirect.scheme == "msauth" && redirect.host == "ch.zerogen.containertrack" &&
            redirect.path.orEmpty().length > 1 && redirect.userInfo == null && redirect.query == null && redirect.fragment == null
    }
}

fun accountMatches(expected: String, current: String?) = expected.isNotBlank() && expected == current
