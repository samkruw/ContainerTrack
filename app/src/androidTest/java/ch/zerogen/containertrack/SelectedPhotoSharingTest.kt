package ch.zerogen.containertrack
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class SelectedPhotoSharingTest {
    @Suppress("DEPRECATION")
    @Test fun onlySelectedOriginalJpegIsSharedWithoutCompletionSideEffects():Unit=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val db=Room.inMemoryDatabaseBuilder(app,TrackDatabase::class.java).build()
        val repo=TrackRepository(app,db)
        val id=repo.createJob(JobDraft(JobKind.RETURN,"SELECTED",orderValue="20 ft",quantity=2,quick="Nothing",additionalWork="Empty"),123L)
        try {
            for(n in 1..2) {
                val c=repo.addContainer(id,"070$n")
                repo.savePhoto(id,c,1,File.createTempFile("selected", ".jpg",app.cacheDir).apply{writeBytes(byteArrayOf(n.toByte()))})
            }
            val before=db.dao().job(id)!!.job.completionTriggeredAt
            val photo=db.dao().job(id)!!.ordered.last().photos.single()
            val expired=File(app.cacheDir,"shared-photos/selection-expired/$id").apply{mkdirs()}
            File(expired,"0701_01.jpg").writeBytes(byteArrayOf(9))
            assertTrue(expired.setLastModified(System.currentTimeMillis()-8*24*60*60*1000L))
            val intent=repo.prepareSelectedPhotoShare(setOf(photo.id))
            assertFalse(expired.parentFile!!.exists())
            assertEquals("image/jpeg",intent.type);assertEquals(Intent.ACTION_SEND,intent.action)
            val uri=intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
            assertArrayEquals(byteArrayOf(2),app.contentResolver.openInputStream(uri)!!.use{it.readBytes()})
            val teamsIntent=repo.prepareSelectedPhotoShare(setOf(photo.id),teamsPackage="com.microsoft.teams")
            assertEquals("com.microsoft.teams",teamsIntent.`package`)
            assertArrayEquals(byteArrayOf(2),app.contentResolver.openInputStream(teamsIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!)!!.use{it.readBytes()})
            assertEquals(before,db.dao().job(id)!!.job.completionTriggeredAt)
            assertTrue(db.dao().uploadPhotos(id).all{it.uploadedAt==null})
        } finally {repo.deleteJob(id);db.close()}
    }
}
