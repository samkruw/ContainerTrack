package ch.zerogen.containertrack

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    // Grant before launching MainActivity so reminder setup cannot open a system dialog.
    @get:Rule(order = 0) val notifications: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun returnFieldsAreLockedUntilLoggerStopped() {
        compose.onNodeWithContentDescription("Neuer Auftrag").performClick()
        compose.onNodeWithText("Return", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Order Number").assertIsNotEnabled()
        compose.onNodeWithText("Order / Container-Typ").assertIsNotEnabled()
        compose.onNodeWithText("Logger gestoppt – bestätigen").performClick()
        compose.onNodeWithText("Order Number").assertIsEnabled()
        compose.onNodeWithText("Order / Container-Typ").assertIsEnabled()
    }

    @Test fun deliveryNeedsNoQuickOrAdditionalWork() {
        compose.onNodeWithContentDescription("Neuer Auftrag").performClick()
        compose.onNodeWithText("Lieferung", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Fotos starten").performClick()
        compose.onAllNodesWithText("Pflichtfeld").assertCountEquals(2)
        compose.onNodeWithText("Order Number").performScrollTo().performTextInput("TEST-UI")
        compose.onNodeWithText("KT-Nummer").assertDoesNotExist()
        compose.onNodeWithText("Order / Container-Typ").performTextInput("20 ft")
        compose.onNodeWithText("Quick").assertDoesNotExist()
        compose.onNodeWithText("additional Work").assertDoesNotExist()
        compose.onNodeWithText("Fotos starten").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Container 1 / 1 erfassen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Fotos starten").assertDoesNotExist()
        compose.onNodeWithText("TEST-UI").assertIsDisplayed()
    }

    @Test fun returnStillRequiresBothExtraFieldsAndAcceptsNothingEmpty() {
        compose.onNodeWithContentDescription("Neuer Auftrag").performClick()
        compose.onNodeWithText("Return", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Quick").assertIsNotEnabled()
        compose.onNodeWithText("additional Work").assertIsNotEnabled()
        compose.onNodeWithText("Logger gestoppt – bestätigen").performClick()
        compose.onNodeWithText("Order Number").performTextInput("RETURN-UI")
        compose.onNodeWithText("Order / Container-Typ").performTextInput("20 ft")
        compose.onNodeWithText("Fotos starten").performClick()
        compose.onAllNodesWithText("Pflichtfeld – Nothing und Empty sind erlaubt.").assertCountEquals(2)
        compose.onNodeWithText("Quick").performScrollTo().performTextInput("Nothing")
        compose.onNodeWithText("additional Work").performScrollTo().performTextInput("Empty")
        compose.onNodeWithText("Fotos starten").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Container 1 / 1 erfassen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Fotos starten").assertDoesNotExist()
        compose.onNodeWithText("Quick · Nothing\nadditional Work · Empty").performScrollTo().assertIsDisplayed()
    }

    @Test fun bottomNavigationOpensAllDestinations() {
        compose.onNodeWithText("Meine Aufträge").assertIsDisplayed()
        compose.onNodeWithText("Kamera").performClick()
        compose.onNodeWithText("Auftrag für die nächste Erfassung wählen").assertIsDisplayed()
        compose.onNodeWithText("Galerie").performClick()
        compose.onNodeWithText("AUF DIESEM GERÄT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Einstellungen").assertExists()
        compose.onNodeWithText("Lokal auf diesem Gerät").assertExists()
        compose.onNodeWithText("Zurück zu Aufträgen").performScrollTo().performClick()
        compose.onNodeWithText("Meine Aufträge").assertIsDisplayed()
    }

    @Test fun activityRecreationPreservesReturnGateAndInput() {
        compose.onNodeWithContentDescription("Neuer Auftrag").performClick()
        compose.onNodeWithText("Return", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Logger gestoppt – bestätigen").performClick()
        compose.onNodeWithText("Order Number").performTextInput("RETURN-0708")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Order Number").assertIsEnabled().assertTextContains("RETURN-0708")
        compose.onNodeWithText("Logger gestoppt – bestätigen").assertDoesNotExist()
    }
}
