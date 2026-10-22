package ch.zerogen.containertrack

import android.Manifest
import android.os.ParcelFileDescriptor
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Synthetic fixture records only. Captures the actual native shell and navigation. */
class ReferenceScreenshotsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val ids=mutableListOf<String>()
    private val app get()=compose.activity.application as ContainerTrackApp
    private lateinit var completeId:String
    @Before fun fixture():Unit=runBlocking {
        val ui=InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(ui.executeShellCommand("pm grant ch.zerogen.containertrack android.permission.POST_NOTIFICATIONS")).use{it.readBytes()}
        suspend fun job(kt:String,number:String,steps:Int):String {
            val id=app.repository.createJob(JobDraft(JobKind.DELIVERY,"ORDER-$kt",kt,"20 ft",1),null);ids+=id
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
        compose.waitUntil(10_000){compose.onAllNodesWithText("KT 2481").fetchSemanticsNodes().isNotEmpty()}
    }
    @After fun cleanup():Unit=runBlocking{ids.forEach{app.repository.deleteJob(it)}}
    private fun keyboardAway(){compose.runOnIdle{compose.activity.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.currentFocus?.windowToken,0)};compose.waitForIdle()}
    private fun capture(name:String){
        compose.waitForIdle()
        val directory=File(app.getExternalFilesDir(null),"screenshots").apply{mkdirs()}
        File(directory,"$name.png").outputStream().use{compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun openCompleteDetails(){
        compose.onNodeWithContentDescription("Aufträge filtern").performClick()
        compose.onNodeWithText("Alle").performClick()
        compose.onNodeWithText("Aufträge suchen").performTextInput("2480");keyboardAway()
        compose.waitUntil(10_000){compose.onAllNodesWithText("KT 2480").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("KT 2480").performClick()
        compose.onNodeWithText("Auftragsdetails").performClick()
    }
    @Test fun ordersMatchReference(){capture("20-reference-orders")}
    @Test fun newOrderMatchesReference(){
        compose.onNodeWithContentDescription("Neuer Auftrag").performClick()
        compose.onNodeWithText("KT-Nummer").performTextInput("2482")
        compose.onNodeWithText("Container-Nr.").performTextInput("0708");keyboardAway()
        capture("21-reference-new-order")
    }
    @Test fun photoReviewMatchesReference(){openCompleteDetails();capture("22-reference-photo-review")}
    @Test fun galleryMatchesReference(){
        compose.onNodeWithText("Galerie").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("248");keyboardAway()
        compose.waitUntil(10_000){compose.onAllNodesWithText("KT 2480 · 0042").fetchSemanticsNodes().isNotEmpty()}
        capture("23-reference-gallery")
    }
    @Test fun galleryPhotoSelectionMatchesReference(){
        compose.onNodeWithText("Galerie").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("KT 2480 · 0042").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("KT 2480 · 0042").performClick()
        compose.onNodeWithContentDescription("Foto 1 auswählen").performClick()
        compose.onNodeWithContentDescription("Foto 2 auswählen").performClick()
        compose.onNodeWithText("2 von 6 Fotos ausgewählt").assertExists()
        capture("24-reference-photo-selection")
    }
    @Test fun signInMatchesReference(){
        compose.onNodeWithContentDescription("Profil und Einstellungen").performClick()
        capture("25-reference-sign-in")
    }
    @Test fun uploadMatchesReferenceWithoutPretendingSuccess(){
        openCompleteDetails()
        compose.onNodeWithText("Teams-Übertragung").performScrollTo().performClick()
        compose.onNodeWithText("Alle 6 Fotos erfolgreich gesendet").assertDoesNotExist()
        capture("26-reference-upload")
    }
    @Test fun userAccessShowsOnlyVerifiedPermissions(){
        compose.onNodeWithContentDescription("Profil und Einstellungen").performClick()
        compose.onNodeWithText("Benutzer & Rechte").performScrollTo().performClick()
        compose.onNodeWithText("Kein Firmenkonto verbunden.").assertExists()
        capture("27-reference-access")
    }

}
