package ch.zerogen.containertrack

import androidx.room.withTransaction

class RoomPhotoSyncStore(private val database: TrackDatabase) : PhotoSyncStore {
    private val dao = database.dao()
    override suspend fun snapshot(jobId: String): SyncSnapshot? = database.withTransaction {
        val batch = dao.job(jobId) ?: return@withTransaction null
        val queue = dao.uploadJob(jobId) ?: return@withTransaction null
        SyncSnapshot(batch,queue,dao.uploadPhotos(jobId))
    }
    override suspend fun state(jobId: String, state: String, error: String?, next: Long?) {
        database.withTransaction {
            dao.uploadState(jobId,state,error,System.currentTimeMillis())
            dao.deferUpload(jobId,next)
        }
    }
    override suspend fun confirm(photoId: String, owner: String, remoteId: String, at: Long) =
        dao.confirmUpload(photoId,owner,remoteId,at) > 0
    override suspend fun completeIfCurrent(jobId: String, owner: String): Boolean = database.withTransaction {
        val batch = dao.job(jobId) ?: return@withTransaction true
        val queue = dao.uploadJob(jobId) ?: return@withTransaction true
        val states = dao.uploadPhotos(jobId)
        val ids = batch.containers.flatMap { it.photos }.map { it.id }.toSet()
        if (!batch.complete || queue.ownerAccountId != owner || states.map { it.photoId }.toSet() != ids ||
            states.any { it.uploadedAt == null || it.uploadedByAccountId != owner }) return@withTransaction false
        dao.uploadState(jobId,"SYNCED",null,System.currentTimeMillis());dao.deferUpload(jobId,null);true
    }
}

class PhotoSyncRepository(database: TrackDatabase, config: MicrosoftConfig, accounts: MicrosoftAccountProvider, graph: GraphPhotoClient) {
    private val engine = PhotoSyncEngine(config,accounts,graph,RoomPhotoSyncStore(database))
    suspend fun sync(jobId: String, accountId: String) = engine.sync(jobId,accountId)
}
