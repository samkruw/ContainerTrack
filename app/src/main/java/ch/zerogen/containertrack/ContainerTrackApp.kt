package ch.zerogen.containertrack

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ContainerTrackApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { Room.databaseBuilder(this, TrackDatabase::class.java, "container-track.db").build() }
    val repository by lazy { TrackRepository(this, database) }
    val alarms by lazy { ReminderAlarms(this) }
    override fun onCreate() {
        super.onCreate()
        alarms.createChannel()
        scope.launch { database.dao().pendingReminders().forEach { alarms.schedule(it) } }
    }
}
