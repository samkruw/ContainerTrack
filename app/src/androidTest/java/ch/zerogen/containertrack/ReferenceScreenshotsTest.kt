package ch.zerogen.containertrack

import android.os.Build
import androidx.test.rule.GrantPermissionRule

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/** Synthetic fixture records only. Captures the actual native shell and navigation. */
class ReferenceScreenshotsTest {
    // Grant before launching MainActivity so reminder setup cannot open a system dialog.
    @get:Rule(order = 0) val notifications: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    )
    @get:Rule(order = 1) val compose=createAndroidComposeRule<MainActivity>()
    private val ids=mutableListOf<String>()
    private val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
    private lateinit var completeId:String
    @Before fun fixture():Unit=runBlocking {
        // Finish the first activity frame before Room fixture updates trigger recomposition.
        compose.waitForIdle()
        suspend fun job(kt:String,number:String,steps:Int):String {
            val id=app.repository.createJob(JobDraft(JobKind.DELIVERY,"ORDER-$kt",kt,"20 ft",1),null);ids+=id
            if(steps==6) app.database.dao().insertReminder(LoggerReminder(jobId=id,referenceNumber="ORDER-$kt",dueAt=System.currentTimeMillis(),confirmedAt=System.currentTimeMillis()))
            val c=app.repository.addContainer(id,number)
            for(step in 1..steps) {
                val file=File.createTempFile("reference", ".jpg",app.cacheDir)
                InstrumentationRegistry.getInstrumentation().context.assets.open("container_front.jpg").use{input->file.outputStream().use{input.copyTo(it)}}
                app.repository.savePhoto(id,c,step,file)
            }
            app.database.dao().jobReminder(id)?.let{app.repository.confirmReminder(it.id)}
            return id
        }
        job("2479","3310",0)
        completeId=job("2480","0042",6)
        job("2481","0708",3)
        compose.waitUntil(10_000){compose.onAllNodesWithText("ORDER-2481").fetchSemanticsNodes().isNotEmpty()}
    }
    @After fun cleanup():Unit=runBlocking{ids.forEach{app.repository.deleteJob(it)}}
    private fun keyboardAway(){compose.runOnIdle{compose.activity.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.currentFocus?.windowToken,0)};compose.waitForIdle()}
    private fun capture(name:String){
        compose.waitForIdle()
        val directory=File(app.getExternalFilesDir(null),"screenshots").apply{mkdirs()}
        File(directory,"$name.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun openCompleteDetails(){
        compose.onNodeWithContentDescription("Aufträge filtern").performClick()
        compose.onNodeWithText("Alle").performClick()
        // Keep 2481 visible as the active summary so 2480 remains an actionable list row.
        compose.onNodeWithText("Aufträge suchen").performTextReplacement("248");keyboardAway()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("ORDER-2480"))
        compose.onNodeWithText("ORDER-2480").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("Auftragsdetails").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Auftragsdetails").assertIsEnabled().performClick()
    }
    @Test fun ordersMatchReference(){capture("20-reference-orders")}
    @Test fun newOrderMatchesReference(){
        compose.onNodeWithContentDescription("Neuer Auftrag").performClick()
        compose.onNodeWithText("KT-Nummer").assertDoesNotExist()
        compose.onNodeWithText("Order Number").performTextInput("ORDER-2482")
        compose.onNodeWithText("Container-Nr.").performTextInput("0708")
        compose.onNodeWithText("Container-Nr.").performImeAction();keyboardAway()
        compose.onNodeWithText("Neuer Auftrag").performScrollTo()
        capture("21-reference-new-order")
    }
    @Test fun photoReviewMatchesReference(){
        openCompleteDetails()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Fotos prüfen"))
        compose.onNodeWithText("Fotos prüfen").performClick()
        capture("22-reference-photo-review")
    }
    @Suppress("DEPRECATION")
    @Test fun finishReviewPreparesEmailWithoutClaimingItWasSent():Unit=runBlocking {
        openCompleteDetails()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Fotos prüfen"))
        compose.onNodeWithText("Fotos prüfen").performClick()
        val marker=app.database.dao().job(completeId)!!.job.completionTriggeredAt
        val reminder=app.database.dao().jobReminder(completeId)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val captures=ConcurrentLinkedQueue<Intent>()
        val monitor=object:Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent:Intent):Instrumentation.ActivityResult? {
                val email=intent.action in setOf(Intent.ACTION_SEND,Intent.ACTION_SEND_MULTIPLE) && intent.type=="image/jpeg"
                if(intent.action!=Intent.ACTION_CHOOSER && !email)return null
                captures.add(Intent(intent))
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.onNodeWithText("Auftrag per E-Mail").assertIsEnabled().performClick()
            compose.waitUntil(60_000){captures.isNotEmpty()}
            org.junit.Assert.assertEquals(1,captures.size)
            val outbound=captures.single()
            val targets=if(outbound.action==Intent.ACTION_CHOOSER) {
                listOf(requireNotNull(outbound.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)))+
                    outbound.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS).orEmpty().map{it as Intent}
            } else listOf(outbound)
            org.junit.Assert.assertTrue(targets.isNotEmpty())
            targets.forEach { target ->
                org.junit.Assert.assertEquals(Intent.ACTION_SEND_MULTIPLE,target.action)
                org.junit.Assert.assertEquals("image/jpeg",target.type)
                org.junit.Assert.assertEquals("ORDER-2480",target.getStringExtra(Intent.EXTRA_SUBJECT))
                org.junit.Assert.assertNotNull(target.component)
                org.junit.Assert.assertNotEquals("com.microsoft.teams",target.component!!.packageName)
                val attachments=requireNotNull(target.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))
                org.junit.Assert.assertEquals((1..6).map{"0042_${it.toString().padStart(2,'0')}.jpg"},attachments.map{it.lastPathSegment})
                org.junit.Assert.assertEquals(6,target.clipData!!.itemCount)
                org.junit.Assert.assertTrue(target.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
                org.junit.Assert.assertEquals(0,target.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                attachments.forEachIndexed { index,uri ->
                    org.junit.Assert.assertEquals(uri,target.clipData!!.getItemAt(index).uri)
                    org.junit.Assert.assertEquals("content",uri.scheme)
                    org.junit.Assert.assertEquals("image/jpeg",app.contentResolver.getType(uri))
                }
            }
            val job=app.database.dao().job(completeId)!!.job
            org.junit.Assert.assertNull(job.emailSentAt)
            org.junit.Assert.assertEquals(job.documentationRevision,job.emailPreparedRevision)
            org.junit.Assert.assertNotNull(job.emailPreparedAt)
            org.junit.Assert.assertEquals(marker,job.completionTriggeredAt)
            org.junit.Assert.assertEquals(reminder,app.database.dao().jobReminder(completeId))
            org.junit.Assert.assertTrue(app.database.dao().uploadPhotos(completeId).all{it.uploadedAt==null})
        } finally {
            // Cancel the activity's collectors before removing interception, including on timeout.
            try { compose.activityRule.scenario.close() }
            finally { instrumentation.removeMonitor(monitor) }
        }
    }
    @Test fun galleryMatchesReference(){
        compose.onNodeWithText("Galerie").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("ORDER-2480 · 0042").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Container Track").assertIsDisplayed()
        listOf("Aufträge","Kamera","Galerie").forEach { label ->
            compose.onNode(hasText(label) and isSelectable()).assertIsDisplayed()
        }
        capture("23-reference-gallery")
    }
    @Test fun galleryPhotoSelectionMatchesReference(){
        compose.onNodeWithText("Galerie").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("ORDER-2480 · 0042").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("ORDER-2480 · 0042").performClick()
        compose.onNodeWithContentDescription("Foto 1 auswählen").performClick()
        compose.onNodeWithContentDescription("Foto 2 auswählen").performClick()
        // Selection is global: six photos for 2480 and three for the in-progress 2481 order.
        compose.onNodeWithText("2 von 9 Fotos ausgewählt").assertExists()
        compose.onNodeWithText("Die E-Mail enthält alle 6 Fotos aller Container dieses Auftrags.").assertExists()
        capture("24-reference-photo-selection")
    }
    @Test fun settingsWithoutLogin(){
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Einstellungen").assertExists()
        compose.onNodeWithText("PIN").assertDoesNotExist()
        compose.onNodeWithText("Benutzer & Rechte").assertDoesNotExist()
        capture("25-reference-settings")
    }
    @Test fun uploadMatchesReferenceWithoutPretendingSuccess(){
        openCompleteDetails()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Auftrag per E-Mail"))
        compose.onNodeWithText("Auftrag per E-Mail").performClick()
        compose.onNodeWithText("Alle 6 Fotos erfolgreich gesendet").assertDoesNotExist()
        compose.onNodeWithText("E-Mail vorbereiten").assertIsEnabled()
        compose.onNodeWithText("E-Mail gesendet").assertIsNotEnabled()
        compose.onNodeWithText("Betreff: ORDER-2480").assertExists()
        capture("26-reference-upload")
    }

}
