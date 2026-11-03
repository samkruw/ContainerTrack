package ch.zerogen.containertrack

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LocalJobAccessTest {
    @Test fun legacyOwnersRemainAccessibleWithoutLoginAndCompletionCreatesNoUploadQueue():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val db=Room.inMemoryDatabaseBuilder(context,TrackDatabase::class.java).build()
        val repo=TrackRepository(context,db,automaticUploads=false)
        val ids=mutableListOf<String>()
        try {
            for(owner in listOf(null,"local:former-admin","local:former-employee","old-microsoft-account")) {
                val legacy=TrackRepository(context,db,automaticUploads=false,accountId={owner})
                val id=legacy.createJob(JobDraft(JobKind.DELIVERY,"OLD-${ids.size}",kt="2481",orderValue="20 ft",quantity=1),null)
                ids+=id
                val c=repo.addContainer(id,"0708")
                val count=db.dao().job(id)!!.job.draft.kind.photoCount
                for(step in 1..count) repo.savePhoto(id,c,step,File.createTempFile("legacy", ".jpg",context.cacheDir).apply{writeBytes(byteArrayOf(1,2,3))})
                assertTrue(db.dao().job(id)!!.complete)
                assertEquals(owner,db.dao().job(id)!!.job.createdByAccountId)
                assertNull(db.dao().uploadJob(id))
                val photo=db.dao().job(id)!!.ordered.single().photos.first()
                assertNotNull(repo.prepareSelectedPhotoShare(setOf(photo.id)))
                assertNotNull(repo.preparePhotoShare(id,requireComplete=true))
                assertNull(db.dao().uploadJob(id))
                assertEquals(owner,db.dao().job(id)!!.job.createdByAccountId)
                assertEquals(count,db.dao().job(id)!!.ordered.single().photos.size)
            }
            assertEquals(ids.toSet(),repo.jobs.first().map{it.job.id}.toSet())
        } finally {ids.forEach{repo.deleteJob(it)};db.close()}
    }
}
