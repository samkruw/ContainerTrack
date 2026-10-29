package ch.zerogen.containertrack

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Cancels the old automatic transport safely; manual Teams sharing is user initiated. */
class PhotoSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result = Result.success()
}
