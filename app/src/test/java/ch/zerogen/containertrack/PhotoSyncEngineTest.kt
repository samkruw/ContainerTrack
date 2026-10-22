package ch.zerogen.containertrack

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PhotoSyncEngineTest {
    private val config=MicrosoftConfig("11111111-1111-1111-1111-111111111111","22222222-2222-2222-2222-222222222222","msauth://ch.zerogen.containertrack/hash","33333333-3333-3333-3333-333333333333","19:channel@thread.tacv2")
    private class Store(var snapshot: SyncSnapshot) : PhotoSyncStore {
        override suspend fun snapshot(jobId: String) = snapshot
        override suspend fun state(jobId: String, state: String, error: String?, next: Long?) {
            snapshot=snapshot.copy(queue=snapshot.queue.copy(state=state,lastError=error,nextAttemptAt=next))
        }
        override suspend fun confirm(photoId: String, owner: String, remoteId: String, at: Long): Boolean {
            if(snapshot.queue.ownerAccountId!=owner || snapshot.uploads.none{it.photoId==photoId})return false
            snapshot=snapshot.copy(uploads=snapshot.uploads.map{if(it.photoId==photoId)it.copy(uploadedAt=at,remoteItemId=remoteId,uploadedByAccountId=owner)else it});return true
        }
        override suspend fun completeIfCurrent(jobId: String, owner: String): Boolean {
            if(!snapshot.batch.complete||snapshot.queue.ownerAccountId!=owner||snapshot.uploads.any{it.uploadedAt==null})return false
            state(jobId,"SYNCED",null,null);return true
        }
    }
    private fun store(file: File): Store {
        val job=JobEntity(id="job",draft=JobDraft(JobKind.RETURN,"RET",orderValue="20 ft",quantity=2,quick="Nothing",additionalWork="Empty"),createdAt=1L)
        val containers=(1..2).map { n ->
            val c=ContainerEntity(id="c$n",jobId="job",sequenceIndex=n,containerNumber="070$n")
            ContainerWithPhotos(c,listOf(PhotoEntity(id="p$n",containerId=c.id,step=1,localPath=file.path,capturedAt=1L)))
        }
        val batch=JobWithContainers(job,containers)
        return Store(SyncSnapshot(batch,UploadJobEntity("job","account-a"),uploadManifest(batch).map{UploadPhotoEntity(it.photoId,"job",it.remotePath)}))
    }
    @Test fun partialFailureRetriesOnlyTheUnconfirmedImage(): Unit = runBlocking {
        val file=File.createTempFile("sync", ".jpg").apply{writeBytes(byteArrayOf(1))}
        try {
            val store=store(file);val sent=mutableListOf<String>();var fail=true
            val client=object:GraphPhotoClient {
                override suspend fun resolveFolder(config:MicrosoftConfig,accountId:String)=ResolvedTeamsFolder("d","f")
                override suspend fun upload(candidate:UploadCandidate,folder:ResolvedTeamsFolder,accountId:String):RemotePhoto {
                    sent+=candidate.photoId
                    if(candidate.photoId=="p2"&&fail){fail=false;throw SyncFailure("Offline",true)}
                    return RemotePhoto("remote-${candidate.photoId}",null)
                }
            }
            val engine=PhotoSyncEngine(config,TestMicrosoftAccounts(),client,store)
            assertEquals(SyncOutcome.Retry,engine.sync("job","account-a"))
            assertEquals(1,store.snapshot.uploads.count{it.uploadedAt!=null})
            assertEquals(SyncOutcome.Done,engine.sync("job","account-a"))
            assertEquals(listOf("p1","p2","p2"),sent);assertEquals("SYNCED",store.snapshot.queue.state)
            assertTrue(file.isFile)
        } finally{file.delete()}
    }
    @Test fun wrongAccountNeverUploads(): Unit = runBlocking {
        val file=File.createTempFile("sync", ".jpg").apply{writeBytes(byteArrayOf(1))}
        try {
            var calls=0;val store=store(file)
            val client=object:GraphPhotoClient {
                override suspend fun resolveFolder(config:MicrosoftConfig,accountId:String):ResolvedTeamsFolder{calls++;return ResolvedTeamsFolder("d","f")}
                override suspend fun upload(candidate:UploadCandidate,folder:ResolvedTeamsFolder,accountId:String)=RemotePhoto("id",null)
            }
            val result=PhotoSyncEngine(config,TestMicrosoftAccounts(),client,store).sync("job","account-b")
            assertTrue(result is SyncOutcome.Blocked);assertEquals(0,calls);assertTrue(file.isFile)
        }finally{file.delete()}
    }
    @Test fun replacedPhotoCannotBeAcknowledgedAsTheOldVersion(): Unit = runBlocking {
        val file=File.createTempFile("sync", ".jpg").apply{writeBytes(byteArrayOf(1))}
        try {
            val store=store(file);val sent=mutableListOf<String>()
            val client=object:GraphPhotoClient {
                override suspend fun resolveFolder(config:MicrosoftConfig,accountId:String)=ResolvedTeamsFolder("d","f")
                override suspend fun upload(candidate:UploadCandidate,folder:ResolvedTeamsFolder,accountId:String):RemotePhoto {
                    sent+=candidate.photoId
                    if(candidate.photoId=="p1") {
                        val batch=store.snapshot.batch.copy(containers=store.snapshot.batch.containers.map { c ->
                            c.copy(photos=c.photos.map{if(it.id=="p1")it.copy(id="new-version")else it})
                        })
                        store.snapshot=store.snapshot.copy(batch=batch,uploads=store.snapshot.uploads.map {if(it.photoId=="p1")it.copy(photoId="new-version")else it})
                    }
                    return RemotePhoto("remote-${candidate.photoId}",null)
                }
            }
            assertEquals(SyncOutcome.Done,PhotoSyncEngine(config,TestMicrosoftAccounts(),client,store).sync("job","account-a"))
            assertTrue("new-version" in sent);assertFalse(store.snapshot.uploads.any{it.photoId=="p1"})
            assertTrue(store.snapshot.uploads.all{it.uploadedAt!=null})
        }finally{file.delete()}
    }
}
