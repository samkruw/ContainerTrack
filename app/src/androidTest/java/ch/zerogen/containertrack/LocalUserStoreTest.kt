package ch.zerogen.containertrack

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LocalUserStoreTest {
    private val context=ApplicationProvider.getApplicationContext<ContainerTrackApp>()
    @Test fun profilesPersistButSessionsAndRawPinsDoNot() {
        val name="users-${UUID.randomUUID()}"
        val users=LocalUserStore(context,name)
        users.bootstrap("Owner","192837")
        users.add("Employee",LocalRole.EMPLOYEE,"746281")
        val restored=LocalUserStore(context,name)
        assertEquals(users.users.value,restored.users.value);assertNull(restored.currentUser.value)
        val raw=context.getSharedPreferences(name,0).getString("profiles","")!!
        assertFalse(raw.contains("192837"));assertFalse(raw.contains("746281"))
        restored.login(restored.users.value.last().id,"746281")
        assertEquals(LocalRole.EMPLOYEE,restored.requireUser().role)
        assertThrows(IllegalStateException::class.java){restored.add("Escalation",LocalRole.ADMIN,"123456")}
    }
    @Test fun lastAdminAndNamesAreProtectedAndDeactivationPreservesUsers() {
        val users=LocalUserStore(context,"users-${UUID.randomUUID()}")
        users.bootstrap("Owner","192837");val owner=users.requireUser()
        assertThrows(IllegalStateException::class.java){users.update(owner.id,owner.name,LocalRole.EMPLOYEE,true)}
        assertThrows(IllegalStateException::class.java){users.update(owner.id,owner.name,owner.role,false)}
        assertThrows(IllegalArgumentException::class.java){users.add("owner",LocalRole.ADMIN,"123456")}
        users.add("Other admin",LocalRole.ADMIN,"746281")
        users.update(owner.id,owner.name,owner.role,false)
        assertNull(users.currentUser.value)
        assertEquals(2,users.users.value.size)
        assertThrows(IllegalStateException::class.java){users.login(owner.id,"192837")}
    }
    @Test fun failedLoginLockSurvivesReopeningAndAdminCanResetPin() {
        val name="users-${UUID.randomUUID()}";var now=1000L
        val users=LocalUserStore(context,name){now}
        users.bootstrap("Owner","192837");users.add("Employee",LocalRole.EMPLOYEE,"746281")
        val employee=users.users.value.last()
        repeat(5){assertThrows(IllegalStateException::class.java){users.login(employee.id,"000000")}}
        val reopened=LocalUserStore(context,name){now}
        assertThrows(IllegalStateException::class.java){reopened.login(employee.id,"746281")}
        users.update(employee.id,employee.name,employee.role,true,"654321")
        users.login(employee.id,"654321")
        assertEquals(employee.id,users.requireUser().id)
        users.changeOwnPin("654321","112233")
        users.logout();assertThrows(IllegalStateException::class.java){users.login(employee.id,"654321")}
        users.login(employee.id,"112233")
    }
}
