package ch.zerogen.containertrack

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class LocalJobAccessTest {
    @Test fun ownershipIsEnforcedInRepositoryIncludingShareAndLegacyJobs():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val users=LocalUserStore(context,"access-${UUID.randomUUID()}")
        val db=Room.inMemoryDatabaseBuilder(context,TrackDatabase::class.java).build()
        val repo=TrackRepository(context,db,access=users,accountId={users.currentUser.value?.id})
        try {
            try {repo.createJob(JobDraft(JobKind.RETURN,"DENIED",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),1);fail("Login required")} catch(_:IllegalStateException){}
            users.bootstrap("Admin","123456");val admin=users.requireUser()
            val legacy=TrackRepository(context,db).createJob(JobDraft(JobKind.RETURN,"OLD",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),1)
            users.add("Employee",LocalRole.EMPLOYEE,"654321");val employee=users.users.value.last()
            users.login(employee.id,"654321")
            val own=repo.createJob(JobDraft(JobKind.RETURN,"OWN",orderValue="20 ft",quick="Nothing",additionalWork="Empty"),1)
            val c=repo.addContainer(own,"0708")
            repo.savePhoto(own,c,1,File.createTempFile("access", ".jpg",context.cacheDir).apply{writeBytes(byteArrayOf(1))})
            assertNull(db.dao().uploadJob(own))
            val photo=db.dao().job(own)!!.ordered.single().photos.single()
            assertEquals(listOf(own),repo.jobs.first().map{it.job.id})
            try {repo.deleteJob(legacy);fail("Foreign delete must fail")}catch(_:IllegalStateException){}
            try {repo.preparePhotoShare(legacy);fail("Foreign share must fail")}catch(_:IllegalStateException){}
            users.login(admin.id,"123456")
            assertEquals(2,repo.jobs.first().size)
            users.add("Second",LocalRole.EMPLOYEE,"112233");val second=users.users.value.last();users.login(second.id,"112233")
            try {repo.prepareSelectedPhotoShare(setOf(photo.id));fail("Foreign selected photo must fail")}catch(_:IllegalStateException){}
            assertTrue(repo.jobs.first().isEmpty())
            users.logout();assertTrue(repo.jobs.first().isEmpty())
            assertNotNull(db.dao().job(legacy));assertNotNull(db.dao().job(own))
        } finally {db.close()}
    }
}
