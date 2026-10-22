package ch.zerogen.containertrack

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PhotoSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ContainerTrackApp
        val id = inputData.getString("jobId") ?: return Result.failure()
        app.accounts.refresh()
        val owner = app.database.dao().uploadJob(id)?.ownerAccountId.orEmpty()
        return when (app.photoSync.sync(id,owner)) {
            SyncOutcome.Retry -> Result.retry()
            is SyncOutcome.Failed -> Result.failure()
            else -> Result.success() // Blocked is durable, and is resumed on explicit login/configuration/retry.
        }
    }
}
