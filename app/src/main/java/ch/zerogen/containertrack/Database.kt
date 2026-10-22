package ch.zerogen.containertrack

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @Embedded val draft: JobDraft,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val completedAt: Long? = null,
    val exportedAt: Long? = null,
    val loggerStoppedAt: Long? = null,
    val completionTriggeredAt: Long? = null
)

@Entity(tableName = "containers", foreignKeys = [ForeignKey(entity = JobEntity::class,
    parentColumns = ["id"], childColumns = ["jobId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["jobId", "containerNumber"], unique = true), Index(value = ["jobId", "sequenceIndex"], unique = true)])
data class ContainerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val jobId: String,
    val sequenceIndex: Int,
    val containerNumber: String,
    val completedAt: Long? = null
)

@Entity(tableName = "photos", foreignKeys = [ForeignKey(entity = ContainerEntity::class,
    parentColumns = ["id"], childColumns = ["containerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["containerId", "step"], unique = true)])
data class PhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val containerId: String,
    val step: Int,
    val localPath: String,
    val capturedAt: Long
)

@Entity(tableName = "reminders", foreignKeys = [ForeignKey(entity = JobEntity::class,
    parentColumns = ["id"], childColumns = ["jobId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["jobId"], unique = true)])
data class LoggerReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: String,
    val referenceNumber: String,
    val dueAt: Long,
    val confirmedAt: Long? = null
) {
    @get:Ignore val notificationId: Int get() = id
}

data class ContainerWithPhotos(
    @Embedded val container: ContainerEntity,
    @Relation(parentColumn = "id", entityColumn = "containerId") val photos: List<PhotoEntity>
)
data class JobWithContainers(
    @Embedded val job: JobEntity,
    @Relation(entity = ContainerEntity::class, parentColumn = "id", entityColumn = "jobId")
    val containers: List<ContainerWithPhotos>
) {
    val ordered get() = containers.sortedBy { it.container.sequenceIndex }
    val complete get() = containers.size == job.draft.quantity && containers.all {
        it.photos.map { p -> p.step }.sorted() == (1..job.draft.kind.photoCount).toList()
    }
    val phase get() = workflowPhase(job.draft.kind, job.draft.quantity, containers.map { it.photos.map { photo -> photo.step }.toSet() })
    val completedCount get() = containers.count { it.container.completedAt != null }
    val status get() = when {
        job.exportedAt != null -> "Exportiert"
        complete -> "Vollständig"
        phase == WorkflowPhase.READY_FOR_FILL -> "Bereit zum Befüllen"
        containers.isEmpty() -> "Geplant"
        else -> "In Bearbeitung"
    }
}

@Entity(tableName = "upload_jobs", foreignKeys = [ForeignKey(entity = JobEntity::class,
    parentColumns = ["id"], childColumns = ["jobId"], onDelete = ForeignKey.CASCADE)])
data class UploadJobEntity(
    @PrimaryKey val jobId: String,
    val ownerAccountId: String? = null,
    val state: String = "COMPLETE_PENDING_UPLOAD",
    val lastError: String? = null,
    val lastAttemptAt: Long? = null
)

@Entity(tableName = "upload_photos", foreignKeys = [
    ForeignKey(entity = PhotoEntity::class, parentColumns = ["id"], childColumns = ["photoId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = JobEntity::class, parentColumns = ["id"], childColumns = ["jobId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("jobId")])
data class UploadPhotoEntity(
    @PrimaryKey val photoId: String,
    val jobId: String,
    val remotePath: String,
    val uploadedAt: Long? = null,
    val remoteItemId: String? = null,
    val uploadedByAccountId: String? = null
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM upload_jobs") fun observeUploadJobs(): Flow<List<UploadJobEntity>>
    @Query("SELECT * FROM upload_photos") fun observePhotoUploads(): Flow<List<UploadPhotoEntity>>
    @Query("SELECT * FROM upload_jobs") suspend fun uploadJobs(): List<UploadJobEntity>
    @Query("SELECT * FROM upload_jobs WHERE jobId=:jobId") suspend fun uploadJob(jobId: String): UploadJobEntity?
    @Query("SELECT * FROM upload_photos WHERE jobId=:jobId") suspend fun uploadPhotos(jobId: String): List<UploadPhotoEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertUploadJob(job: UploadJobEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertUploadPhoto(photo: UploadPhotoEntity): Long
    @Query("UPDATE upload_jobs SET state=:state,lastError=:error,lastAttemptAt=:at WHERE jobId=:jobId")
    suspend fun uploadState(jobId: String, state: String, error: String?, at: Long)
    @Query("UPDATE upload_jobs SET ownerAccountId=:accountId,state='COMPLETE_PENDING_UPLOAD',lastError=NULL WHERE jobId=:jobId AND ownerAccountId IS NULL")
    suspend fun bindUpload(jobId: String, accountId: String): Int
    @Query("UPDATE upload_photos SET uploadedAt=:at,remoteItemId=:remoteId,uploadedByAccountId=:accountId WHERE photoId=:photoId AND EXISTS (SELECT 1 FROM upload_jobs WHERE upload_jobs.jobId=upload_photos.jobId AND ownerAccountId=:accountId)")
    suspend fun confirmUpload(photoId: String, accountId: String, remoteId: String, at: Long): Int
    @Transaction @Query("SELECT * FROM jobs ORDER BY updatedAt DESC")
    fun observeJobs(): Flow<List<JobWithContainers>>
    @Transaction @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun job(id: String): JobWithContainers?
    @Query("SELECT * FROM reminders WHERE confirmedAt IS NULL ORDER BY dueAt")
    fun observeReminders(): Flow<List<LoggerReminder>>
    @Query("SELECT * FROM reminders WHERE confirmedAt IS NULL ORDER BY dueAt")
    suspend fun pendingReminders(): List<LoggerReminder>
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun reminder(id: Int): LoggerReminder?
    @Query("SELECT * FROM reminders WHERE jobId = :jobId")
    suspend fun jobReminder(jobId: String): LoggerReminder?
    @Insert suspend fun insertJob(job: JobEntity)
    @Update suspend fun updateJob(job: JobEntity)
    @Insert suspend fun insertContainer(container: ContainerEntity)
    @Update suspend fun updateContainer(container: ContainerEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPhoto(photo: PhotoEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReminder(reminder: LoggerReminder): Long
    @Query("UPDATE reminders SET confirmedAt = :at WHERE id = :id AND confirmedAt IS NULL")
    suspend fun confirmReminder(id: Int, at: Long)
    @Query("DELETE FROM photos WHERE containerId = :id AND step >= :fromStep")
    suspend fun deletePhotosFrom(id: String, fromStep: Int)
    @Query("DELETE FROM jobs WHERE id = :id") suspend fun deleteJob(id: String)
}

@Database(entities = [JobEntity::class, ContainerEntity::class, PhotoEntity::class, LoggerReminder::class, UploadJobEntity::class, UploadPhotoEntity::class], version = 2, exportSchema = true)
abstract class TrackDatabase : RoomDatabase() { abstract fun dao(): TrackDao }
