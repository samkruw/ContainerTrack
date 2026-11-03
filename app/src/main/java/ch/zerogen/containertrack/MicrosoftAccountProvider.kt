package ch.zerogen.containertrack

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

data class MicrosoftIdentity(val accountId: String, val displayName: String, val username: String)

interface MicrosoftAccountProvider {
    val identity: StateFlow<MicrosoftIdentity?>
    val status: StateFlow<String>
    suspend fun refresh()
    suspend fun signIn(activity: Activity)
    suspend fun signOut()
    suspend fun accessToken(accountId: String): String
}

class MicrosoftAuthenticationRequired : IllegalStateException("Microsoft-Anmeldung erforderlich.")
