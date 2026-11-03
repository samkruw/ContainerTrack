package ch.zerogen.containertrack

class SyncFailure(val safeMessage: String, val retryable: Boolean = false, val retryAfterSeconds: Long = 0) : Exception(safeMessage)
