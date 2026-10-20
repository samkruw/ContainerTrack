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

    @Test fun deliveryShowsRequiredErrorsAndAcceptsNothingEmpty() {
        compose.onNodeWithText("Lieferung", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Auftrag speichern").performScrollTo().performClick()
        compose.onAllNodesWithText("Pflichtfeld").assertCountEquals(3)
        compose.onNodeWithText("Order Number").performScrollTo().performTextInput("TEST-UI")
        compose.onNodeWithText("ZRH Number").performTextInput("ZRH-UI")
        compose.onNodeWithText("Order / Container-Typ").performTextInput("20 ft")
        compose.onNodeWithText("Quick").performScrollTo().performTextInput("Nothing")
        compose.onNodeWithText("additional Work").performScrollTo().performTextInput("Empty")
        compose.onNodeWithText("Auftrag speichern").performScrollTo().assertIsEnabled()
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
}
