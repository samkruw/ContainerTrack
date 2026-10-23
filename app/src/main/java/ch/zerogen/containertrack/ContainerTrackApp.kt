package ch.zerogen.containertrack

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ContainerTrackApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { Room.databaseBuilder(this, TrackDatabase::class.java, "container-track.db").addMigrations(MIGRATION_1_2).build() }
    val localUsers by lazy { LocalUserStore(this) }
    val microsoftConfig by lazy { configuredMicrosoft(this) }
    val accounts by lazy { MsalAccountProvider(this,microsoftConfig) }
    val network by lazy { NetworkMonitor(this) }
    val uploadScheduler by lazy { WorkManagerUploadScheduler(this) }
    val repository by lazy { TrackRepository(this, database, UploadScheduler {}, localUsers) { localUsers.currentUser.value?.id } }
    val photoSync by lazy { PhotoSyncRepository(database,microsoftConfig,accounts,HttpGraphPhotoClient(microsoftConfig,accounts)) }
    val alarms by lazy { ReminderAlarms(this) }
    override fun onCreate() {
        super.onCreate()
        alarms.createChannel()
        uploadScheduler.stopForLogout()
        scope.launch { repository.reconcilePendingEffects() }
    }
}
