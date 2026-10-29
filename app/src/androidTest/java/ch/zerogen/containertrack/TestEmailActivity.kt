package ch.zerogen.containertrack

import android.app.Activity
import android.os.Bundle

/** Mail discovery fixture in the instrumentation APK; no message is ever sent. */
class TestEmailActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        finish()
    }
}
