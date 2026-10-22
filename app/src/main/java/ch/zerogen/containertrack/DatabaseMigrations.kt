package ch.zerogen.containertrack

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE jobs ADD COLUMN completionTriggeredAt INTEGER")
        db.execSQL("UPDATE jobs SET completionTriggeredAt = COALESCE(completedAt,exportedAt) WHERE completedAt IS NOT NULL OR exportedAt IS NOT NULL")
        db.execSQL("""CREATE TABLE IF NOT EXISTS upload_jobs (
            jobId TEXT NOT NULL PRIMARY KEY, ownerAccountId TEXT, state TEXT NOT NULL,
            lastError TEXT, lastAttemptAt INTEGER,
            FOREIGN KEY(jobId) REFERENCES jobs(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS upload_photos (
            photoId TEXT NOT NULL PRIMARY KEY, jobId TEXT NOT NULL, remotePath TEXT NOT NULL,
            uploadedAt INTEGER, remoteItemId TEXT, uploadedByAccountId TEXT,
            FOREIGN KEY(photoId) REFERENCES photos(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(jobId) REFERENCES jobs(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_upload_photos_jobId ON upload_photos(jobId)")
    }
}
