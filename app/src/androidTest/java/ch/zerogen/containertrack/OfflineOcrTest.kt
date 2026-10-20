package ch.zerogen.containertrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfflineOcrTest {
    @Suppress("DEPRECATION")
    @Test fun bundledModelRecognizesLeadingZeroInAirplaneMode() {
        val app = ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val permissions = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse("The installed APK must not request INTERNET", Manifest.permission.INTERNET in permissions)
        val airplaneBefore = Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        fun airplane(enabled: Boolean) {
            if (Build.VERSION.SDK_INT < 29) return
            val command = "cmd connectivity airplane-mode ${if (enabled) "enable" else "disable"}"
            ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
                .use { it.readBytes() }
        }
        val bitmap = Bitmap.createBitmap(800, 280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawText("0708", 45f, 210f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 180f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            airplane(true)
            if (Build.VERSION.SDK_INT >= 29) assertEquals(1, Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0))
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
            val candidates = result.textBlocks.flatMap { it.lines.flatMap { line -> line.elements.map { element -> element.text } + line.text } }
            assertEquals("0708", firstContainerNumber(candidates))
        } finally {
            recognizer.close(); bitmap.recycle(); airplane(airplaneBefore)
        }
    }
}
