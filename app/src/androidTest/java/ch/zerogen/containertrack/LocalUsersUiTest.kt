package ch.zerogen.containertrack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class LocalUsersUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun adminAddsAndDeactivatesUserThroughTheActualEditor() {
        val app=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
        val users=LocalUserStore(app,"ui-users-${UUID.randomUUID()}")
        users.bootstrap("Owner","123456")
        compose.setContent {
            val list by users.users.collectAsState()
            ContainerTheme {LocalUserAccessScreen(users.requireUser(),list,false,users::add,users::update)}
        }
        compose.onNodeWithText("Benutzer hinzufügen").performScrollTo().performClick()
        compose.onNodeWithText("Benutzername").performTextInput("Worker")
        compose.onNodeWithText("PIN (6–12 Ziffern)").performTextInput("654321")
        compose.onNodeWithText("Speichern").performClick()
        compose.onNodeWithText("Worker").assertExists()
        compose.runOnIdle {assertEquals(LocalRole.EMPLOYEE,users.users.value.last().role)}
        compose.onNodeWithContentDescription("Benutzer Worker bearbeiten").performScrollTo().performClick()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Speichern").performClick()
        compose.runOnIdle {assertFalse(users.users.value.last().active)}
    }
    @Test fun employeeCannotSeeManagementActions() {
        val employee=LocalUser("worker","Worker",LocalRole.EMPLOYEE)
        compose.setContent {ContainerTheme {LocalUserAccessScreen(employee,listOf(employee),false,{_,_,_->},{_,_,_,_,_->})}}
        compose.onNodeWithText("Nur Administratoren dürfen Benutzer verwalten.").assertExists()
        compose.onNodeWithText("Benutzer hinzufügen").assertDoesNotExist()
    }
}
