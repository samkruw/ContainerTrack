package ch.zerogen.containertrack

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReturnMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TrackDatabase::class.java)

    @Test fun unfinishedReturnsGainNewWorkflowWhileLegacyDataAndDeliveryReminderSurvive() {
        val name = "migration-return-v2-v3.db"
        helper.createDatabase(name, 2).apply {
            execSQL("""INSERT INTO jobs (id,kind,referenceNumber,kt,orderValue,quantity,quick,additionalWork,createdAt,updatedAt,completedAt,exportedAt,completionTriggeredAt)
                VALUES ('delivery','DELIVERY','DEL-1','Legacy value','20 ft',1,'','',1000,2000,1900,2000,1900),
                ('open','RETURN','RET-OPEN','','20 ft',1,'Nothing','Empty',3000,3000,NULL,NULL,NULL),
                ('complete','RETURN','RET-DONE','','20 ft',1,'Nothing','Empty',4000,4100,4100,NULL,4100),
                ('exported','RETURN','RET-EXPORTED','','20 ft',1,'Nothing','Empty',5000,5100,NULL,5100,5100),
                ('old-reminder','RETURN','RET-OLD','','20 ft',1,'Nothing','Empty',6000,6000,NULL,NULL,NULL)""")
            execSQL("""INSERT INTO containers (id,jobId,sequenceIndex,containerNumber,completedAt)
                VALUES ('dc','delivery',1,'0708',1900),('rc','open',1,'0709',NULL),('cc','complete',1,'0710',4100)""")
            execSQL("""INSERT INTO photos (id,containerId,step,localPath,capturedAt)
                VALUES ('dp','dc',1,'/existing/delivery.jpg',1800),('rp','rc',1,'/existing/return.jpg',3200),
                ('cp','cc',1,'/existing/complete.jpg',4050)""")
            execSQL("""INSERT INTO reminders (id,jobId,referenceNumber,dueAt,confirmedAt)
                VALUES (12,'delivery','DEL-1',1801900,2200),(13,'old-reminder','RET-OLD',7000,7100)""")
            execSQL("INSERT INTO upload_jobs (jobId,ownerAccountId,state) VALUES ('delivery','owner','UPLOADED')")
            execSQL("""INSERT INTO upload_photos (photoId,jobId,remotePath,uploadedAt,remoteItemId,uploadedByAccountId)
                VALUES ('dp','delivery','delivery/0708/0708_01.jpg',2100,'remote','owner')""")
            close()
        }

        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT id,returnWorkflowVersion,documentationRevision,emailPreparedRevision,emailPreparedAt,emailSentAt FROM jobs ORDER BY id").use { cursor ->
                val expected = mapOf("delivery" to 0, "open" to 1, "complete" to 0, "exported" to 0, "old-reminder" to 1)
                var count = 0
                while (cursor.moveToNext()) {
                    assertEquals(expected.getValue(cursor.getString(0)), cursor.getInt(1))
                    assertEquals(0L, cursor.getLong(2))
                    for (column in 3..5) assertTrue(cursor.isNull(column))
                    count++
                }
                assertEquals(expected.size, count)
            }
            db.query("SELECT localPath,returnPhase,returnCategory FROM photos WHERE id='rp'").use {
                assertTrue(it.moveToFirst())
                assertEquals("/existing/return.jpg", it.getString(0))
                assertEquals("INITIAL", it.getString(1))
                assertEquals("CONTENT", it.getString(2))
            }
            db.query("SELECT id,dueAt,confirmedAt,kind FROM reminders WHERE jobId='delivery'").use {
                assertTrue(it.moveToFirst()); assertEquals(12, it.getInt(0)); assertEquals(1801900L, it.getLong(1))
                assertEquals(2200L, it.getLong(2)); assertEquals("LOGGER", it.getString(3))
            }
            db.query("SELECT dueAt,confirmedAt,kind FROM reminders WHERE jobId='open'").use {
                assertTrue(it.moveToFirst()); assertEquals(returnReminderDueAt(3000), it.getLong(0))
                assertTrue(it.isNull(1)); assertEquals("RETURN_CLOSE", it.getString(2))
            }
            db.query("SELECT id,dueAt,confirmedAt,kind FROM reminders WHERE jobId='old-reminder'").use {
                assertTrue(it.moveToFirst()); assertEquals(13, it.getInt(0))
                assertEquals(returnReminderDueAt(6000), it.getLong(1)); assertTrue(it.isNull(2))
                assertEquals("RETURN_CLOSE", it.getString(3))
            }
            db.query("SELECT COUNT(*) FROM reminders WHERE jobId IN ('complete','exported')").use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
            db.query("SELECT kt FROM jobs WHERE id='delivery'").use {
                assertTrue(it.moveToFirst()); assertEquals("Legacy value", it.getString(0))
            }
            db.query("SELECT state,ownerAccountId FROM upload_jobs").use {
                assertTrue(it.moveToFirst()); assertEquals("UPLOADED", it.getString(0)); assertEquals("owner", it.getString(1))
            }
            db.query("SELECT remoteItemId,uploadedAt FROM upload_photos").use {
                assertTrue(it.moveToFirst()); assertEquals("remote", it.getString(0)); assertEquals(2100L, it.getLong(1))
            }
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
    }
}
