package ch.zerogen.containertrack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun graphSegment(value: String): String = URLEncoder.encode(value,"UTF-8").replace("+","%20")
data class ResolvedTeamsFolder(val driveId: String, val itemId: String)
data class RemotePhoto(val id: String, val eTag: String?)
data class GraphRequest(val method: String, val path: String, val accessToken: String, val file: File? = null, val json: String? = null)
data class GraphResponse(val code: Int, val json: String, val retryAfterSeconds: Long = 0)
fun interface GraphTransport { suspend fun execute(request: GraphRequest): GraphResponse }
interface GraphPhotoClient {
    suspend fun resolveFolder(config: MicrosoftConfig, accountId: String): ResolvedTeamsFolder
    suspend fun upload(candidate: UploadCandidate, folder: ResolvedTeamsFolder, accountId: String): RemotePhoto
}

/** No redirects: never forward a bearer token to a different host. No request/response logging. */
class MicrosoftGraphTransport : GraphTransport {
    override suspend fun execute(request: GraphRequest): GraphResponse = withContext(Dispatchers.IO) {
        require(request.path.startsWith("/") && !request.path.contains("://"))
        val connection = URL("https://graph.microsoft.com/v1.0" + request.path).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000;connection.readTimeout = 60_000
            connection.requestMethod = request.method
            connection.setRequestProperty("Authorization","Bearer ${request.accessToken}")
            connection.setRequestProperty("Accept","application/json")
            if (request.file != null) {
                require(request.file.length() in 1..MAX_PHOTO_UPLOAD_BYTES)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type","image/jpeg")
                connection.setFixedLengthStreamingMode(request.file.length())
                connection.outputStream.use { output -> request.file.inputStream().use { it.copyTo(output) } }
            } else if (request.json != null) {
                val bytes = request.json.toByteArray(Charsets.UTF_8)
                connection.doOutput = true;connection.setRequestProperty("Content-Type","application/json")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { input ->
                val bytes = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (bytes.size() + count > 1024*1024) throw SyncFailure("Microsoft-Antwort überschreitet das sichere Metadatenlimit")
                    bytes.write(buffer,0,count)
                }
                String(bytes.toByteArray(),Charsets.UTF_8)
            }.orEmpty()
            val header = connection.getHeaderField("Retry-After").orEmpty()
            val retry = header.toLongOrNull() ?: runCatching {
                java.time.Duration.between(java.time.Instant.now(),ZonedDateTime.parse(header,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).seconds
            }.getOrDefault(0)
            GraphResponse(code,text,retry.coerceAtLeast(0))
        } catch (_: IOException) { throw SyncFailure("Verbindung unterbrochen · erneuter Versuch folgt",true) }
        finally { connection.disconnect() }
    }
}

class HttpGraphPhotoClient(private val configuration: MicrosoftConfig, private val accounts: MicrosoftAccountProvider,
                          private val transport: GraphTransport = MicrosoftGraphTransport()) : GraphPhotoClient {
    private suspend fun request(method: String, path: String, owner: String, file: File? = null, json: String? = null): GraphResponse {
        if (!configuration.configured) throw SyncFailure("Microsoft nicht eingerichtet")
        val token = accounts.accessToken(owner)
        return transport.execute(GraphRequest(method,path,token,file,json))
    }
    private fun checked(response: GraphResponse): JSONObject {
        when (response.code) {
            401 -> throw SyncFailure("Microsoft-Anmeldung erneuern")
            403 -> throw SyncFailure("Kein Zugriff auf den Teams-Kanal · Firmenfreigabe prüfen")
            429 -> throw SyncFailure("Microsoft begrenzt die Anfragen · erneuter Versuch folgt",true,response.retryAfterSeconds)
        }
        if (response.code >= 500) throw SyncFailure("Microsoft vorübergehend nicht erreichbar",true,response.retryAfterSeconds)
        if (response.code !in setOf(200,201)) throw SyncFailure("Übertragung nicht bestätigt · Kanal und Firmenfreigabe prüfen")
        return try { JSONObject(response.json) } catch (_: Exception) { throw SyncFailure("Ungültige Microsoft-Antwort") }
    }
    private fun required(json: JSONObject, key: String): String = json.optString(key).takeIf { it.isNotBlank() && it != "null" }
        ?: throw SyncFailure("Microsoft hat keine gültige Dateiidentität bestätigt")

    override suspend fun resolveFolder(config: MicrosoftConfig, accountId: String): ResolvedTeamsFolder {
        check(config == configuration)
        val channel = "/teams/${graphSegment(config.teamId)}/channels/${graphSegment(config.channelId)}"
        val info = checked(request("GET",channel,accountId))
        if (info.optString("displayName") != "Op530-Lal") throw SyncFailure("Der konfigurierte Kanal ist nicht Op530-Lal")
        val folder = checked(request("GET","$channel/filesFolder",accountId))
        val parent = folder.optJSONObject("parentReference") ?: throw SyncFailure("Teams-Dateiablage nicht verfügbar")
        return ResolvedTeamsFolder(required(parent,"driveId"),required(folder,"id"))
    }

    private suspend fun folder(parent: ResolvedTeamsFolder, name: String, owner: String): ResolvedTeamsFolder {
        val root = "/drives/${graphSegment(parent.driveId)}/items/${graphSegment(parent.itemId)}"
        val lookup = "$root:/${graphSegment(name)}"
        var response = request("GET",lookup,owner)
        if (response.code == 404) {
            response = request("POST","$root/children",owner,json=JSONObject().put("name",name)
                .put("folder",JSONObject()).put("@microsoft.graph.conflictBehavior","fail").toString())
            if (response.code == 409) response = request("GET",lookup,owner)
        }
        val json = checked(response)
        if (!json.has("folder")) throw SyncFailure("Die Teams-Zielablage ist kein Ordner")
        return ResolvedTeamsFolder(parent.driveId,required(json,"id"))
    }

    override suspend fun upload(candidate: UploadCandidate, folder: ResolvedTeamsFolder, accountId: String): RemotePhoto {
        val parts = candidate.remotePath.split('/')
        require(parts.size == 3 && parts[0] == candidate.jobId && Regex("[A-Za-z0-9-]{1,64}").matches(parts[0]) &&
            validContainerNumber(parts[1]) && parts[2] == candidate.fileName && Regex("[0-9]{4}_0[1-6]\\.jpg").matches(parts[2]))
        val jobFolder = folder(folder,parts[0],accountId)
        val containerFolder = folder(jobFolder,parts[1],accountId)
        val path = "/drives/${graphSegment(containerFolder.driveId)}/items/${graphSegment(containerFolder.itemId)}:/${graphSegment(candidate.fileName)}:/content"
        val result = checked(request("PUT",path,accountId,file=candidate.file))
        if (result.optString("name") != candidate.fileName || result.optLong("size",-1) != candidate.file.length())
            throw SyncFailure("Microsoft hat die vollständige Bilddatei nicht bestätigt")
        return RemotePhoto(required(result,"id"),result.optString("eTag").takeIf { it.isNotBlank() })
    }
}
