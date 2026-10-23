package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class LocalUserPolicyTest {
    @Test fun employeesAccessOnlyTheirOwnJobsAndInactiveUsersAccessNothing() {
        val employee=LocalUser("a","Anna",LocalRole.EMPLOYEE)
        assertTrue(mayAccessLocalJob(employee,"a"))
        assertFalse(mayAccessLocalJob(employee,"b"))
        assertFalse(mayAccessLocalJob(employee,null))
        assertFalse(mayAccessLocalJob(employee.copy(active=false),"a"))
        assertFalse(mayAccessLocalJob(null,"a"))
    }
    @Test fun activeAdminsCanAccessPreservedLegacyJobs() {
        val admin=LocalUser("admin","Admin",LocalRole.ADMIN)
        assertTrue(mayAccessLocalJob(admin,null))
        assertTrue(mayAccessLocalJob(admin,"previous-microsoft-account"))
        assertFalse(mayAccessLocalJob(admin.copy(active=false),null))
    }
    @Test fun pinHashesAreSaltedAndRejectWrongPins() {
        val first=LocalPin.hash("123456")
        val second=LocalPin.hash("123456")
        assertNotEquals(first,second)
        assertTrue(LocalPin.verify("123456",first))
        assertFalse(LocalPin.verify("654321",first))
        assertFalse(LocalPin.verify("123456","invalid"))
    }
}
