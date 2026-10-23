package ch.zerogen.containertrack
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class InitialContainerTest {
    @Test fun initialContainerPreservesLeadingZeroAndInvalidInputCreatesNoJob():Unit=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val db=Room.inMemoryDatabaseBuilder(app,TrackDatabase::class.java).build()
        val repo=TrackRepository(app,db)
        try {
            val draft=JobDraft(JobKind.DELIVERY,"INITIAL","2482","20 ft")
            val id=repo.createJob(draft,null,"0708")
            assertEquals("0708",db.dao().job(id)!!.ordered.single().container.containerNumber)
            val count=repo.jobs.first().size
            try {repo.createJob(draft.copy(referenceNumber="BAD"),null,"708");fail("Invalid number must fail before insertion")}
            catch(_:IllegalArgumentException){}
            assertEquals(count,repo.jobs.first().size)
            repo.deleteJob(id)
        } finally {db.close()}
    }
}
