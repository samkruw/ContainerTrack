package ch.zerogen.containertrack

import android.app.Activity
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PhotoSyncTest {
    @Test fun roomConfirmsOnlyTheCurrentAccountAndSurvivesReopening(): Unit = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="sync-${UUID.randomUUID()}.db"
        var db=Room.databaseBuilder(context,TrackDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
        var repo=TrackRepository(context,db,accountId={"account-a"})
        val job=repo.createJob(JobDraft(JobKind.RETURN,"SYNC-${UUID.randomUUID()}",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),123L)
        val config=MicrosoftConfig("11111111-1111-1111-1111-111111111111","22222222-2222-2222-2222-222222222222","msauth://ch.zerogen.containertrack/hash","33333333-3333-3333-3333-333333333333","19:channel@thread.tacv2")
        val accounts=object:MicrosoftAccountProvider {
            override val identity=MutableStateFlow<MicrosoftIdentity?>(MicrosoftIdentity("account-a","Test only","test@example.invalid"))
            override val status=MutableStateFlow("Test only")
            override suspend fun refresh() {}
            override suspend fun signIn(activity:Activity) {}
            override suspend fun signOut() {identity.value=null}
            override suspend fun accessToken(accountId:String)="test-only-token"
        }
        var sent=0
        val graph=object:GraphPhotoClient {
            override suspend fun resolveFolder(config:MicrosoftConfig,accountId:String)=ResolvedTeamsFolder("drive","folder")
            override suspend fun upload(candidate:UploadCandidate,folder:ResolvedTeamsFolder,accountId:String):RemotePhoto {sent++;return RemotePhoto("remote-$sent",null)}
        }
        try {
            val container=repo.addContainer(job,"0708")
            repo.savePhoto(job,container,1,File.createTempFile("sync", ".jpg",context.cacheDir).apply{writeBytes(byteArrayOf(1))})
            val row=db.dao().uploadPhotos(job).single()
            assertEquals(0,db.dao().confirmUpload(row.photoId,"wrong-account","forged",1L))
            assertEquals(SyncOutcome.Done,PhotoSyncRepository(db,config,accounts,graph).sync(job,"account-a"))
            assertEquals(1,sent);assertEquals("account-a",db.dao().uploadPhotos(job).single().uploadedByAccountId)
            db.close()
            db=Room.databaseBuilder(context,TrackDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
            repo=TrackRepository(context,db)
            assertEquals(SyncOutcome.Done,PhotoSyncRepository(db,config,accounts,graph).sync(job,"account-a"))
            assertEquals(1,sent);assertEquals("SYNCED",db.dao().uploadJob(job)!!.state)
            assertTrue(File(db.dao().job(job)!!.containers.single().photos.single().localPath).isFile)
        } finally{repo.deleteJob(job);db.close();context.deleteDatabase(name)}
    }
}
