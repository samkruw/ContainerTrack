package ch.zerogen.containertrack

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncOutcome {
    data object Done : SyncOutcome
    data object Retry : SyncOutcome
    data class Blocked(val reason: String) : SyncOutcome
    data class Failed(val reason: String) : SyncOutcome
}
data class SyncSnapshot(val batch: JobWithContainers, val queue: UploadJobEntity, val uploads: List<UploadPhotoEntity>)
interface PhotoSyncStore {
    suspend fun snapshot(jobId: String): SyncSnapshot?
    suspend fun state(jobId: String, state: String, error: String?, next: Long?)
    suspend fun confirm(photoId: String, owner: String, remoteId: String, at: Long): Boolean
    suspend fun completeIfCurrent(jobId: String, owner: String): Boolean
}

/** Serial processing and immutable photo IDs prevent stale acknowledgements after a replacement. */
class PhotoSyncEngine(private val config: MicrosoftConfig, private val accounts: MicrosoftAccountProvider,
    private val graph: GraphPhotoClient, private val store: PhotoSyncStore) {
    private val serial = Mutex()
    suspend fun sync(jobId: String, accountId: String): SyncOutcome = serial.withLock {
        try {
            var folder: ResolvedTeamsFolder? = null
            while (true) {
                currentCoroutineContext().ensureActive()
                val snapshot = store.snapshot(jobId) ?: return@withLock SyncOutcome.Done
                fun blocked(reason: String) = SyncOutcome.Blocked(reason)
                val reason = when {
                    !config.configured -> "Microsoft nicht eingerichtet"
                    snapshot.queue.ownerAccountId == null -> "Zum Übertragen mit einem Firmenkonto verbinden"
                    !accountMatches(accountId,snapshot.queue.ownerAccountId) || !accountMatches(accountId,accounts.identity.value?.accountId) -> "Mit dem ursprünglichen Firmenkonto anmelden"
                    !snapshot.batch.complete -> "Pflichtfotos fehlen · Upload pausiert"
                    else -> null
                }
                if (reason != null) {
                    store.state(jobId,"COMPLETE_PENDING_UPLOAD",reason,null)
                    return@withLock blocked(reason)
                }
                if ((snapshot.queue.nextAttemptAt ?: 0) > System.currentTimeMillis()) return@withLock SyncOutcome.Retry
                val candidates = uploadManifest(snapshot.batch)
                val candidate = candidates.firstOrNull { item -> snapshot.uploads.none { it.photoId == item.photoId && it.uploadedAt != null } }
                if (candidate == null) {
                    if (store.completeIfCurrent(jobId,accountId)) return@withLock SyncOutcome.Done
                    continue
                }
                if (snapshot.uploads.none { it.photoId == candidate.photoId }) throw SyncFailure("Upload-Warteschlange prüfen")
                store.state(jobId,"UPLOADING",null,null)
                if (folder == null) folder = graph.resolveFolder(config,accountId)
                val remote = graph.upload(candidate,folder,accountId)
                currentCoroutineContext().ensureActive()
                // A replaced/deleted photo has a different ID; its old success cannot acknowledge the new row.
                store.confirm(candidate.photoId,accountId,remote.id,System.currentTimeMillis())
            }
            @Suppress("UNREACHABLE_CODE") SyncOutcome.Done
        } catch (e: CancellationException) { throw e }
        catch (_: MicrosoftAuthenticationRequired) {
            val reason = "Microsoft-Anmeldung erneuern"
            store.state(jobId,"COMPLETE_PENDING_UPLOAD",reason,null);SyncOutcome.Blocked(reason)
        } catch (e: SyncFailure) {
            val next = if (e.retryAfterSeconds <= 0) null else {
                val now = System.currentTimeMillis()
                if (e.retryAfterSeconds > (Long.MAX_VALUE-now)/1000) Long.MAX_VALUE else now+e.retryAfterSeconds*1000
            }
            store.state(jobId,if (e.retryable) "COMPLETE_PENDING_UPLOAD" else "UPLOAD_ERROR",e.safeMessage,next)
            if (e.retryable) SyncOutcome.Retry else SyncOutcome.Failed(e.safeMessage)
        } catch (_: Exception) {
            val reason = "Fotoübertragung fehlgeschlagen · lokale Bilder bleiben erhalten"
            store.state(jobId,"UPLOAD_ERROR",reason,null);SyncOutcome.Failed(reason)
        }
    }
}
