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
    val loggerStoppedAt: Long? = null
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
    val completedCount get() = containers.count { it.container.completedAt != null }
    val status get() = when {
        job.exportedAt != null -> "Exportiert"
        complete -> "Vollständig"
        containers.isEmpty() -> "Geplant"
        else -> "In Bearbeitung"
    }
}

@Dao
interface TrackDao {
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

@Database(entities = [JobEntity::class, ContainerEntity::class, PhotoEntity::class, LoggerReminder::class], version = 1, exportSchema = true)
abstract class TrackDatabase : RoomDatabase() { abstract fun dao(): TrackDao }
