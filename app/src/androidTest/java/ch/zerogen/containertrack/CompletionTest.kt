package ch.zerogen.containertrack

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CompletionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private suspend fun photo(repo: TrackRepository, job: String, container: String, step: Int) {
        repo.savePhoto(job,container,step,File.createTempFile("completion", ".jpg",context.cacheDir).apply { writeBytes(byteArrayOf(step.toByte())) })
    }
    @Test fun threeContainersCanWaitForFilledPhotoAndCompletionFiresOnce(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        val repo = TrackRepository(context,db)
        val job = repo.createJob(JobDraft(JobKind.DELIVERY,"PHASE-${UUID.randomUUID()}","ZRH","20 ft",3),null)
        try {
            val containers = mutableListOf<String>()
            for (number in listOf("0708","0710","0712")) {
                val id = repo.addContainer(job,number);containers += id
                for (step in 1..5) photo(repo,job,id,step)
            }
            assertEquals(WorkflowPhase.READY_FOR_FILL,db.dao().job(job)!!.phase)
            assertNull(db.dao().jobReminder(job));assertNull(db.dao().uploadJob(job))
            for (id in containers.take(2)) photo(repo,job,id,6)
            assertNull(db.dao().jobReminder(job))
            photo(repo,job,containers.last(),6)
            val first = db.dao().jobReminder(job)!!
            val completed = db.dao().job(job)!!.job.completionTriggeredAt!!
            assertEquals(completed+1_800_000L,first.dueAt)
            assertEquals(18,db.dao().uploadPhotos(job).size)
            repo.reconcilePendingEffects();repo.reconcilePendingEffects()
            photo(repo,job,containers.first(),6)
            assertEquals(first,db.dao().jobReminder(job))
            assertEquals(completed,db.dao().job(job)!!.job.completionTriggeredAt)
            assertEquals(18,db.dao().uploadPhotos(job).size)
        } finally { repo.deleteJob(job);db.close() }
    }
    @Test fun returnDocumentationKeepsItsCreationReminderAndCreatesNoUploadQueue(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context,TrackDatabase::class.java).build()
        val repo = TrackRepository(context,db)
        val job = repo.createJob(JobDraft(JobKind.RETURN,"RET-${UUID.randomUUID()}",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),123L)
        try {
            val reminder = db.dao().jobReminder(job)!!
            val id = repo.addContainer(job,"0001")
            repo.saveReturnPhoto(job,id,ReturnPhotoPhase.INITIAL,ReturnPhotoCategory.CONTENT,
                File.createTempFile("return-content", ".jpg",context.cacheDir).apply { writeBytes(byteArrayOf(1)) })
            assertFalse(db.dao().job(job)!!.complete)
            repo.saveReturnPhoto(job,id,ReturnPhotoPhase.CLEAN,ReturnPhotoCategory.CLEAN,
                File.createTempFile("return-clean", ".jpg",context.cacheDir).apply { writeBytes(byteArrayOf(2)) })
            assertTrue(db.dao().job(job)!!.complete)
            assertEquals(ReminderKind.RETURN_CLOSE,reminder.kind)
            assertEquals(db.dao().job(job)!!.job.createdAt+86_400_000L,reminder.dueAt)
            assertEquals(reminder,db.dao().jobReminder(job))
            assertNull(db.dao().uploadJob(job))
            assertTrue(db.dao().uploadPhotos(job).isEmpty())
            assertNull(db.dao().job(job)!!.job.emailSentAt)
        } finally {repo.deleteJob(job);db.close()}
    }
    @Test fun permanentUploadErrorsNeedExplicitRetry(): Unit = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,TrackDatabase::class.java).build()
        val scheduled=mutableListOf<String>()
        val repo=TrackRepository(context,db,UploadScheduler { scheduled+=it })
        // Graph retries remain supported for records created before the email workflow.
        val legacy=JobEntity(draft=JobDraft(JobKind.RETURN,"ERROR-RETRY",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),createdAt=123L,loggerStoppedAt=123L)
        db.dao().insertJob(legacy)
        val job=legacy.id
        try {
            val c=repo.addContainer(job,"0708");photo(repo,job,c,1)
            db.dao().uploadState(job,"UPLOAD_ERROR","Freigabe prüfen",1L)
            scheduled.clear();repo.reconcilePendingEffects()
            assertTrue(scheduled.isEmpty())
            repo.enqueuePhotoUpload(job,"account")
            assertEquals(listOf(job),scheduled)
        } finally {repo.deleteJob(job);db.close()}
    }

}
