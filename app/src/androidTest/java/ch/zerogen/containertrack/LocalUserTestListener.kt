package ch.zerogen.containertrack

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.Description
import org.junit.runner.notification.RunListener

/** Real local authentication for each test; no production bypass of the login or permission checks. */
class LocalUserTestListener:RunListener() {
    override fun testStarted(description:Description) {
        val users=ApplicationProvider.getApplicationContext<ContainerTrackApp>().localUsers
        if(users.users.value.isEmpty())users.bootstrap("Testadmin","123456")
        val admin=users.users.value.first{it.name=="Testadmin"}
        users.login(admin.id,"123456")
    }
}
