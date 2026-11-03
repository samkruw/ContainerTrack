package ch.zerogen.containertrack

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TrackDatabase::class.java)
    @Test fun preservesExistingPhotosJobsAndRemindersWithoutHistoricalUpload() {
        val name = "migration-v13.db"
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO jobs (id,kind,referenceNumber,kt,orderValue,quantity,quick,additionalWork,createdAt,updatedAt,completedAt,exportedAt) VALUES ('job','DELIVERY','OLD','ZRH','20 ft',1,'Nothing','Empty',1,900,900,1000)")
            execSQL("INSERT INTO containers (id,jobId,sequenceIndex,containerNumber,completedAt) VALUES ('container','job',1,'0708',900)")
            for (step in 1..6) execSQL("INSERT INTO photos (id,containerId,step,localPath,capturedAt) VALUES ('p$step','container',$step,'/existing/photos/$step.jpg',800)")
            execSQL("INSERT INTO reminders (id,jobId,referenceNumber,dueAt,confirmedAt) VALUES (12,'job','OLD',1801000,NULL)")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT completionTriggeredAt, referenceNumber FROM jobs WHERE id='job'").use {
                assertTrue(it.moveToFirst()); assertEquals(900L,it.getLong(0));assertEquals("OLD",it.getString(1))
            }
            db.query("SELECT localPath FROM photos WHERE id='p1'").use { assertTrue(it.moveToFirst());assertEquals("/existing/photos/1.jpg",it.getString(0)) }
            db.query("SELECT id,dueAt,confirmedAt FROM reminders").use { assertTrue(it.moveToFirst());assertEquals(12,it.getInt(0));assertEquals(1801000L,it.getLong(1));assertTrue(it.isNull(2)) }
            db.query("SELECT COUNT(*) FROM upload_jobs").use { assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0)) }
        }
    }
}
