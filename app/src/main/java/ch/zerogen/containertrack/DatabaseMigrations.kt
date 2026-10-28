package ch.zerogen.containertrack

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE jobs ADD COLUMN completionTriggeredAt INTEGER")
        db.execSQL("ALTER TABLE jobs ADD COLUMN createdByAccountId TEXT")
        db.execSQL("UPDATE jobs SET completionTriggeredAt = COALESCE(completedAt,exportedAt) WHERE completedAt IS NOT NULL OR exportedAt IS NOT NULL")
        db.execSQL("""CREATE TABLE IF NOT EXISTS upload_jobs (
            jobId TEXT NOT NULL PRIMARY KEY, ownerAccountId TEXT, state TEXT NOT NULL,
            lastError TEXT, lastAttemptAt INTEGER, nextAttemptAt INTEGER,
            FOREIGN KEY(jobId) REFERENCES jobs(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS upload_photos (
            photoId TEXT NOT NULL PRIMARY KEY, jobId TEXT NOT NULL, remotePath TEXT NOT NULL,
            uploadedAt INTEGER, remoteItemId TEXT, uploadedByAccountId TEXT,
            FOREIGN KEY(photoId) REFERENCES photos(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(jobId) REFERENCES jobs(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_upload_photos_jobId ON upload_photos(jobId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE jobs ADD COLUMN returnWorkflowVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE jobs ADD COLUMN documentationRevision INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE jobs ADD COLUMN emailPreparedRevision INTEGER")
        db.execSQL("ALTER TABLE jobs ADD COLUMN emailPreparedAt INTEGER")
        db.execSQL("ALTER TABLE jobs ADD COLUMN emailSentAt INTEGER")
        db.execSQL("ALTER TABLE photos ADD COLUMN returnPhase TEXT NOT NULL DEFAULT 'INITIAL'")
        db.execSQL("ALTER TABLE photos ADD COLUMN returnCategory TEXT NOT NULL DEFAULT 'CONTENT'")
        db.execSQL("ALTER TABLE reminders ADD COLUMN kind TEXT NOT NULL DEFAULT 'LOGGER'")
        db.execSQL("UPDATE jobs SET returnWorkflowVersion=1 WHERE kind='RETURN' AND completedAt IS NULL AND exportedAt IS NULL")
        // A valid legacy Return had no reminder. Keep any existing row ID if one is present.
        db.execSQL("""UPDATE reminders SET kind='RETURN_CLOSE',
            dueAt=(SELECT createdAt+86400000 FROM jobs WHERE jobs.id=reminders.jobId),confirmedAt=NULL
            WHERE jobId IN (SELECT id FROM jobs WHERE kind='RETURN' AND returnWorkflowVersion=1)""")
        db.execSQL("""INSERT OR IGNORE INTO reminders (jobId,referenceNumber,dueAt,confirmedAt,kind)
            SELECT id,referenceNumber,createdAt+86400000,NULL,'RETURN_CLOSE' FROM jobs
            WHERE kind='RETURN' AND returnWorkflowVersion=1""")
    }
}
