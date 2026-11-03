package ch.zerogen.containertrack

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TestMicrosoftAccounts : MicrosoftAccountProvider {
    private val value=MutableStateFlow<MicrosoftIdentity?>(MicrosoftIdentity("account-a","Test only","test@example.invalid"))
    override val identity=value
    override val status=MutableStateFlow("Test only")
    override suspend fun refresh() {}
    override suspend fun signIn(activity: Activity) {}
    override suspend fun signOut() {value.value=null}
    override suspend fun accessToken(accountId: String): String {
        if (!accountMatches(accountId,value.value?.accountId)) throw MicrosoftAuthenticationRequired()
        return "test-only-token"
    }
}

class GraphPhotoClientTest {
    private val config=MicrosoftConfig("11111111-1111-1111-1111-111111111111","22222222-2222-2222-2222-222222222222",
        "msauth://ch.zerogen.containertrack/hash","33333333-3333-3333-3333-333333333333","19:channel@thread.tacv2")
    @Test fun resolvesOnlyTheExpectedChannelAndDrive(): Unit = runBlocking {
        val paths=mutableListOf<String>()
        val client=HttpGraphPhotoClient(config,TestMicrosoftAccounts(),GraphTransport { request ->
            paths += request.path
            if (request.path.endsWith("filesFolder")) GraphResponse(200,"""{"id":"folder","parentReference":{"driveId":"drive"}}""")
            else GraphResponse(200,"""{"displayName":"Op530-Lal"}""")
        })
        assertEquals(ResolvedTeamsFolder("drive","folder"),client.resolveFolder(config,"account-a"))
        assertTrue(paths.first().contains("19%3Achannel%40thread.tacv2"))
    }
    @Test fun neverAcceptsTheWrongChannel(): Unit = runBlocking {
        val client=HttpGraphPhotoClient(config,TestMicrosoftAccounts(),GraphTransport { GraphResponse(200,"""{"displayName":"Other"}""") })
        assertThrows(SyncFailure::class.java) {runBlocking{client.resolveFolder(config,"account-a")}}
    }
    @Test fun uploadNeedsConfirmedIdentityNameAndSize(): Unit = runBlocking {
        val file=File.createTempFile("graph", ".jpg").apply{writeBytes(byteArrayOf(1,2,3))}
        try {
            val candidate=UploadCandidate("photo","job",file,"job/0708/0708_01.jpg","0708_01.jpg")
            var sends=0
            val client=HttpGraphPhotoClient(config,TestMicrosoftAccounts(),GraphTransport { request ->
                if(request.method=="PUT") {sends++;GraphResponse(201,"""{"id":"remote","name":"0708_01.jpg","size":3}""")}
                else GraphResponse(200,"""{"id":"folder","folder":{}}""")
            })
            assertEquals("remote",client.upload(candidate,ResolvedTeamsFolder("drive","folder"),"account-a").id)
            assertEquals(1,sends)
            val malformed=HttpGraphPhotoClient(config,TestMicrosoftAccounts(),GraphTransport {GraphResponse(200,"{}")})
            assertThrows(SyncFailure::class.java){runBlocking{malformed.upload(candidate,ResolvedTeamsFolder("drive","folder"),"account-a")}}
        } finally{file.delete()}
    }
    @Test fun deniedAndThrottledResponsesAreNotSuccess(): Unit = runBlocking {
        for(code in listOf(401,403,429,500)) {
            val client=HttpGraphPhotoClient(config,TestMicrosoftAccounts(),GraphTransport {GraphResponse(code,"secret server details",120)})
            val failure=assertThrows(SyncFailure::class.java){runBlocking{client.resolveFolder(config,"account-a")}}
            assertFalse(failure.message.orEmpty().contains("secret server details"))
            assertEquals(code==429||code==500,failure.retryable)
            if(code==429)assertEquals(120L,failure.retryAfterSeconds)
        }
    }
    @Test fun wrongAccountNeverReachesTransport(): Unit = runBlocking {
        var requests=0
        val client=HttpGraphPhotoClient(config,TestMicrosoftAccounts(),GraphTransport{requests++;GraphResponse(200,"{}")})
        assertThrows(MicrosoftAuthenticationRequired::class.java){runBlocking{client.resolveFolder(config,"account-b")}}
        assertEquals(0,requests)
    }
}
