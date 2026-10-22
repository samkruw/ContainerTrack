package ch.zerogen.containertrack

data class CompletionEffects(val reminder: LoggerReminder? = null, val uploadJobId: String? = null)
fun interface UploadScheduler { fun enqueue(jobId: String) }
