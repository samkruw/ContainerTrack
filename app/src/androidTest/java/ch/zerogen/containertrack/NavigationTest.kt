package ch.zerogen.containertrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun returnFieldsAreLockedUntilLoggerStopped() {
        compose.onNodeWithText("Return", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Return Number").assertIsNotEnabled()
        compose.onNodeWithText("Order / Container-Typ").assertIsNotEnabled()
        compose.onNodeWithText("Logger gestoppt – bestätigen").performClick()
        compose.onNodeWithText("Return Number").assertIsEnabled()
        compose.onNodeWithText("Order / Container-Typ").assertIsEnabled()
    }

    @Test fun deliveryNeedsNoQuickOrAdditionalWork() {
        compose.onNodeWithText("Lieferung", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Auftrag speichern").performScrollTo().performClick()
        compose.onAllNodesWithText("Pflichtfeld").assertCountEquals(3)
        compose.onNodeWithText("Order Number").performScrollTo().performTextInput("TEST-UI")
        compose.onNodeWithText("ZRH Number").performTextInput("ZRH-UI")
        compose.onNodeWithText("Order / Container-Typ").performTextInput("20 ft")
        compose.onNodeWithText("Quick").assertDoesNotExist()
        compose.onNodeWithText("additional Work").assertDoesNotExist()
        compose.onNodeWithText("Auftrag speichern").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Container 1 / 1 erfassen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Auftrag speichern").assertDoesNotExist()
        compose.onNodeWithText("TEST-UI").assertIsDisplayed()
    }

    @Test fun returnStillRequiresBothExtraFieldsAndAcceptsNothingEmpty() {
        compose.onNodeWithText("Return", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Quick").assertIsNotEnabled()
        compose.onNodeWithText("additional Work").assertIsNotEnabled()
        compose.onNodeWithText("Logger gestoppt – bestätigen").performClick()
        compose.onNodeWithText("Return Number").performTextInput("RETURN-UI")
        compose.onNodeWithText("Order / Container-Typ").performTextInput("20 ft")
        compose.onNodeWithText("Auftrag speichern").performScrollTo().performClick()
        compose.onAllNodesWithText("Pflichtfeld – Nothing und Empty sind erlaubt.").assertCountEquals(2)
        compose.onNodeWithText("Quick").performScrollTo().performTextInput("Nothing")
        compose.onNodeWithText("additional Work").performScrollTo().performTextInput("Empty")
        compose.onNodeWithText("Auftrag speichern").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Container 1 / 1 erfassen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Auftrag speichern").assertDoesNotExist()
        compose.onNodeWithText("Quick · Nothing\nadditional Work · Empty").performScrollTo().assertIsDisplayed()
    }

    @Test fun bottomNavigationOpensAllDestinations() {
        compose.onNodeWithText("Aufträge").performClick()
        compose.onNodeWithText("Fortsetzen, prüfen und bewusst löschen").assertIsDisplayed()
        compose.onNodeWithText("Scannen").performClick()
        compose.onNodeWithText("Auftrag für die nächste Erfassung wählen").assertIsDisplayed()
        compose.onNodeWithText("Export").performClick()
        compose.onNodeWithText("Nur vollständige Aufträge können exportiert werden.").assertIsDisplayed()
        compose.onNodeWithText("Mehr").performClick()
        compose.onNodeWithText("Berechtigungen und lokale Daten").assertIsDisplayed()
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Erfassung").assertIsDisplayed()
    }

    @Test fun activityRecreationPreservesReturnGateAndInput() {
        compose.onNodeWithText("Return", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Logger gestoppt – bestätigen").performClick()
        compose.onNodeWithText("Return Number").performTextInput("RETURN-0708")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Return Number").assertIsEnabled().assertTextContains("RETURN-0708")
        compose.onNodeWithText("Logger gestoppt – bestätigen").assertDoesNotExist()
    }
}
