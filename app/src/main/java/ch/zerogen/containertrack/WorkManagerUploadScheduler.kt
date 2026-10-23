package ch.zerogen.containertrack

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class WorkManagerUploadScheduler(context: Context) : UploadScheduler {
    private val manager = WorkManager.getInstance(context)
    override fun enqueue(jobId: String) {
        val work = OneTimeWorkRequestBuilder<PhotoSyncWorker>()
            .setInputData(workDataOf("jobId" to jobId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
            .addTag("photo-sync").addTag("photo-sync:$jobId").build()
        manager.enqueueUniqueWork("photo-sync:$jobId",ExistingWorkPolicy.APPEND_OR_REPLACE,work)
    }
    override fun cancel(jobId: String) { manager.cancelUniqueWork("photo-sync:$jobId") }
    fun stopForLogout() { manager.cancelAllWorkByTag("photo-sync") }
}
