package ch.zerogen.containertrack

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun configuredMicrosoft(context: Context): MicrosoftConfig {
    val source = JSONObject(context.resources.openRawResource(R.raw.microsoft_config).bufferedReader().use { it.readText() })
    fun value(built: String, key: String) = built.ifBlank { source.optString(key) }
    return MicrosoftConfig(value(BuildConfig.MICROSOFT_TENANT_ID,"tenant_id"),
        value(BuildConfig.MICROSOFT_CLIENT_ID,"client_id"), value(BuildConfig.MICROSOFT_REDIRECT_URI,"redirect_uri"),
        value(BuildConfig.MICROSOFT_TEAM_ID,"team_id"), value(BuildConfig.MICROSOFT_CHANNEL_ID,"channel_id"))
}

/** MSAL alone owns tokens. Only the non-secret account identity is exposed to the rest of the app. */
class MsalAccountProvider(private val context: Context, val config: MicrosoftConfig) : MicrosoftAccountProvider {
    private val current = MutableStateFlow<MicrosoftIdentity?>(null)
    override val identity = current.asStateFlow()
    private val message = MutableStateFlow(if (config.configured) "Nicht angemeldet" else "Microsoft nicht eingerichtet")
    override val status = message.asStateFlow()
    private val mutex = Mutex()
    private var client: ISingleAccountPublicClientApplication? = null
    private val scopes = listOf("Files.ReadWrite.All", "Channel.ReadBasic.All")

    private fun accept(account: IAccount?): MicrosoftIdentity? {
        if (account == null) { current.value = null;message.value = "Nicht angemeldet";return null }
        if (!account.tenantId.equals(config.tenantId,true)) throw MicrosoftAuthenticationRequired()
        return MicrosoftIdentity(account.id,account.claims?.get("name")?.toString().orEmpty().ifBlank { account.username.orEmpty() },account.username.orEmpty())
            .also { current.value = it;message.value = "Firmenkonto angemeldet" }
    }

    private suspend fun application(): ISingleAccountPublicClientApplication = mutex.withLock {
        check(config.configured) { "Microsoft nicht eingerichtet." }
        client?.let { return@withLock it }
        Logger.getInstance().setEnablePII(false)
        Logger.getInstance().setEnableLogcatLog(false)
        val configuration = JSONObject().put("client_id",config.clientId).put("redirect_uri",config.redirectUri)
            .put("account_mode","SINGLE").put("authorization_user_agent","BROWSER")
            .put("broker_redirect_uri_registered",false)
            .put("authorities",JSONArray().put(JSONObject().put("type","AAD").put("audience",
                JSONObject().put("type","AzureADMyOrg").put("tenant_id",config.tenantId))))
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir,"msal-public-config.json").apply { writeText(configuration.toString()) }
        }
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                PublicClientApplication.createSingleAccountPublicClientApplication(context,file,
                    object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                        override fun onCreated(application: ISingleAccountPublicClientApplication) {
                            client = application
                            if (continuation.isActive) continuation.resume(application)
                        }
                        override fun onError(exception: MsalException) {
                            message.value = "Microsoft-Konfiguration oder APK-Redirect prüfen"
                            if (continuation.isActive) continuation.resumeWithException(MicrosoftAuthenticationRequired())
                        }
                    })
            }
        }
    }

    override suspend fun refresh() {
        if (!config.configured) return
        try { withContext(Dispatchers.IO) { accept(application().currentAccount.currentAccount) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { current.value = null;message.value = "Microsoft-Anmeldung oder Konfiguration prüfen" }
    }

    override suspend fun signIn(activity: Activity) {
        val api = application()
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Unit> { continuation ->
                api.signIn(SignInParameters.builder().withActivity(activity).withScopes(scopes)
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(result: IAuthenticationResult) {
                            try { accept(result.account);if (continuation.isActive) continuation.resume(Unit) }
                            catch (_: Exception) { if (continuation.isActive) continuation.resumeWithException(MicrosoftAuthenticationRequired()) }
                        }
                        override fun onError(exception: MsalException) {
                            message.value = "Anmeldung nicht bestätigt · Konto und Firmenfreigabe prüfen"
                            if (continuation.isActive) continuation.resumeWithException(MicrosoftAuthenticationRequired())
                        }
                        override fun onCancel() {
                            message.value = "Anmeldung abgebrochen"
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }).build())
            }
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) { if (client != null && !client!!.signOut()) throw MicrosoftAuthenticationRequired() }
        current.value = null;message.value = "Abgemeldet"
    }

    override suspend fun accessToken(accountId: String): String = withContext(Dispatchers.IO) {
        if (!accountMatches(accountId,current.value?.accountId)) throw MicrosoftAuthenticationRequired()
        val api = application()
        val account = api.currentAccount.currentAccount ?: throw MicrosoftAuthenticationRequired()
        if (!accountMatches(accountId,account.id) || !account.tenantId.equals(config.tenantId,true)) throw MicrosoftAuthenticationRequired()
        val parameters = AcquireTokenSilentParameters.Builder().withScopes(scopes).forAccount(account).fromAuthority(account.authority).build()
        val result = try { api.acquireTokenSilent(parameters) } catch (_: MsalException) { throw MicrosoftAuthenticationRequired() }
        if (!accountMatches(accountId,current.value?.accountId) || !accountMatches(accountId,result.account.id)) throw MicrosoftAuthenticationRequired()
        result.accessToken
    }
}
