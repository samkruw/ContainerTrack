package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class OrderFiltersTest {
    private fun batch(exported:Boolean=false, secondIncomplete:Boolean=false):JobWithContainers {
        val job=JobEntity(id="job",draft=JobDraft(JobKind.DELIVERY,"ORDER-LONG","ZRH-007","20 ft",if(secondIncomplete)2 else 1),createdAt=1L,exportedAt=if(exported)2L else null)
        val items=(1..job.draft.quantity).map { index ->
            val c=ContainerEntity(id="c$index",jobId=job.id,sequenceIndex=index,containerNumber="070$index")
            ContainerWithPhotos(c,(1..if(index==2)2 else 5).map{PhotoEntity(containerId=c.id,step=it,localPath="fixture",capturedAt=1L)})
        }
        return JobWithContainers(job,items)
    }
    @Test fun searchesKtAndReferenceAndPreservesLeadingZeros() {
        assertTrue(matchesOrder(batch(),"zrh-007",OrderFilter.ALL,null))
        assertTrue(matchesOrder(batch(),"order-long",OrderFilter.ALL,null))
        assertFalse(matchesOrder(batch(),"not-present",OrderFilter.ALL,null))
    }
    @Test fun oldArchivesStayUnderAllAndActiveRemindersStayVisible() {
        assertFalse(matchesOrder(batch(true),"",OrderFilter.ACTIVE,null))
        assertTrue(matchesOrder(batch(true),"",OrderFilter.ALL,null))
        assertTrue(matchesOrder(batch(true),"",OrderFilter.ACTIVE,null,true))
    }
    @Test fun filterUsesActualMissingFilledPhotoAndRealSyncErrors() {
        assertTrue(matchesOrder(batch(secondIncomplete=true),"",OrderFilter.NEEDS_FILL,null))
        assertFalse(matchesOrder(batch(),"",OrderFilter.UPLOAD_ERROR,null))
        assertTrue(matchesOrder(batch(),"",OrderFilter.UPLOAD_ERROR,UploadJobEntity("job",state="UPLOAD_ERROR")))
    }
}
