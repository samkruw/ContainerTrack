package ch.zerogen.containertrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@Composable
fun CameraSurface(scan: Boolean, enabled: Boolean, onNumber: (String) -> Unit = {}, onPhoto: (File) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (!permission) {
        CameraPermissionContent(modifier, scan,
            onAllow = { permissionRequest.launch(Manifest.permission.CAMERA) },
            onSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) })
        return
    }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var torch by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val latestNumber by rememberUpdatedState(onNumber)
    val latestPhoto by rememberUpdatedState(onPhoto)
    val latestEnabled by rememberUpdatedState(enabled)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val disposed = remember { AtomicBoolean(false) }

    DisposableEffect(previewView, scan, owner) {
        disposed.set(false)
        val recognizer = if (scan) TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) else null
        val analyzing = AtomicBoolean(false)
        val accepted = AtomicBoolean(false)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var bound: List<UseCase> = emptyList()
        val listener = Runnable {
            if (disposed.get()) return@Runnable
            try {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(Size(1650, 2200), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                    .build()
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                if (scan) analysis.setAnalyzer(executor) { proxy ->
                    if (disposed.get() || accepted.get() || !latestEnabled || !analyzing.compareAndSet(false, true)) { proxy.close(); return@setAnalyzer }
                    var roi: Bitmap? = null
                    try {
                        val original = proxy.toBitmap()
                        val rect = proxy.cropRect
                        val visible = Bitmap.createBitmap(original, rect.left, rect.top, rect.width(), rect.height())
                        val upright = Bitmap.createBitmap(visible, 0, 0, visible.width, visible.height, Matrix().apply { postRotate(proxy.imageInfo.rotationDegrees.toFloat()) }, true)
                        val left = (upright.width * 0.1f).roundToInt()
                        val top = (upright.height * 0.375f).roundToInt()
                        val width = (upright.width * 0.8f).roundToInt().coerceAtMost(upright.width - left)
                        val height = (upright.height * 0.25f).roundToInt().coerceAtMost(upright.height - top)
                        val cropped = Bitmap.createBitmap(upright, left, top, width, height)
                        roi = cropped
                        listOf(original, visible, upright).distinct().filter { it !== cropped }.forEach { it.recycle() }
                        recognizer!!.process(InputImage.fromBitmap(cropped, 0))
                            .addOnSuccessListener(mainExecutor) { result ->
                                val candidates = result.textBlocks.flatMap { block -> block.lines.flatMap { line ->
                                    line.elements.map { it.text } + line.text
                                } }
                                val number = firstContainerNumber(candidates)
                                if (!disposed.get() && latestEnabled && number != null && accepted.compareAndSet(false, true)) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                    latestNumber(number)
                                }
                            }.addOnFailureListener(mainExecutor) {
                                if (!disposed.get()) error = "Erkennung nicht möglich. Nummer bitte manuell eingeben."
                            }.addOnCompleteListener {
                                cropped.recycle(); proxy.close(); analyzing.set(false)
                            }
                    } catch (_: Exception) {
                        roi?.recycle(); proxy.close(); analyzing.set(false)
                        mainExecutor.execute { if (!disposed.get()) error = "Bildanalyse nicht möglich. Manuelle Eingabe verwenden." }
                    }
                }
                val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(imageCapture)
                if (scan) group.addUseCase(analysis)
                previewView.viewPort?.let { group.setViewPort(it) }
                bound = if (scan) listOf(preview, imageCapture, analysis) else listOf(preview, imageCapture)
                camera = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, group.build())
                capture = imageCapture
            } catch (_: Exception) { error = "Kamera nicht verfügbar. Zugriff und Kamera-Hardware prüfen." }
        }
        previewView.doOnLayout { providerFuture.addListener(listener, mainExecutor) }
        onDispose {
            disposed.set(true)
            provider?.unbind(*bound.toTypedArray())
            recognizer?.close()
            executor.shutdown()
        }
    }
    CameraCaptureLayout(modifier = modifier, scan = scan, enabled = capture != null && !capturing && enabled,
        capturing = capturing, onCapture = capturePhoto@ {
            val imageCapture = capture ?: return@capturePhoto
            capturing = true; error = null
            imageCapture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            val file = File(context.cacheDir, "capture-${UUID.randomUUID()}.jpg")
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        try {
                            normalizePhoto(file)
                            mainExecutor.execute { if (!disposed.get()) { capturing = false; latestPhoto(file) } else file.delete() }
                        } catch (_: Exception) {
                            file.delete()
                            mainExecutor.execute { if (!disposed.get()) { capturing = false; error = "Foto konnte nicht gespeichert werden. Speicher prüfen und erneut aufnehmen." } }
                        }
                    }
                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                        mainExecutor.execute { if (!disposed.get()) { capturing = false; error = "Aufnahme fehlgeschlagen. Bitte erneut versuchen." } }
                    }
                })
        }) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (scan) Canvas(Modifier.fillMaxSize()) {
                val x = size.width * .1f; val y = size.height * .375f
                val w = size.width * .8f; val h = size.height * .25f
                drawRect(Color.Black.copy(alpha = .45f), Offset.Zero, ComposeSize(size.width, y))
                drawRect(Color.Black.copy(alpha = .45f), Offset(0f, y + h), ComposeSize(size.width, size.height - y - h))
                drawRect(Color.Black.copy(alpha = .45f), Offset(0f, y), ComposeSize(x, h))
                drawRect(Color.Black.copy(alpha = .45f), Offset(x + w, y), ComposeSize(x, h))
                drawRect(Lime, Offset(x, y), ComposeSize(w, h), style = Stroke(3.dp.toPx()))
            }
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                FilledTonalButton(onClick = {
                    val next = !torch
                    val future = camera?.cameraControl?.enableTorch(next)
                    future?.addListener({
                        try { future.get(); torch = next }
                        catch (_: Exception) { error = "Kameralicht nicht verfügbar. Bitte ohne Licht fortfahren." }
                    }, mainExecutor)
                }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Text(if (torch) "Licht aus" else "Licht an") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Navy.copy(alpha = .92f)).padding(8.dp),
                maxLines = 3, overflow = TextOverflow.Ellipsis) }
        }
}

@Composable
fun CameraPermissionContent(modifier: Modifier = Modifier, scan: Boolean = false,
                            onAllow: () -> Unit, onSettings: () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxHeight < 170.dp
        val showTitle = maxHeight >= 90.dp
        val showExplanation = maxHeight > 220.dp
        Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.Center) {
            if (showTitle) Text("Kamerazugriff benötigt", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            if (showExplanation) Text(if (scan) "Kamera erlauben oder Nummer manuell eingeben." else "Für das Foto bitte Kamera erlauben.",
                style = MaterialTheme.typography.bodySmall)
            if (compact) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CaptureAction("Kamera erlauben", primary = true, modifier = Modifier.weight(1f), onClick = onAllow)
                CaptureAction("Einstellungen", modifier = Modifier.weight(1f), onClick = onSettings)
            } else {
                CaptureAction("Kamera erlauben", primary = true, onClick = onAllow)
                CaptureAction("App-Einstellungen öffnen", onClick = onSettings)
            }
        }
    }
}

/** Camera-free layout seam used by viewport tests; capture IO stays in CameraSurface. */
@Composable
fun CameraCaptureLayout(modifier: Modifier = Modifier, scan: Boolean = false, enabled: Boolean = true,
                        capturing: Boolean = false, onCapture: () -> Unit = {},
                        preview: @Composable BoxScope.() -> Unit) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black)
            .testTag("camera-preview"), content = preview)
        if (!scan) CaptureAction(if (capturing) "Foto wird gespeichert …" else "Foto aufnehmen",
            enabled = enabled, primary = true, modifier = Modifier.testTag("camera-shutter"), onClick = onCapture)
    }
}

fun normalizePhoto(file: File) {
    val orientation = ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 4400) inSampleSize *= 2
    }
    val source = requireNotNull(BitmapFactory.decodeFile(file.path, options))
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        }
    }
    val upright = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    val ratio = minOf(1f, 2200f / maxOf(upright.width, upright.height))
    val scaled = Bitmap.createScaledBitmap(upright, (upright.width * ratio).roundToInt().coerceAtLeast(1), (upright.height * ratio).roundToInt().coerceAtLeast(1), true)
    val staged = File(file.parentFile, "${file.name}.normalized")
    try {
        staged.outputStream().use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, it)); it.fd.sync() }
        check(staged.renameTo(file))
    } finally {
        staged.delete()
        listOf(source, upright, scaled).distinct().forEach { it.recycle() }
    }
}
