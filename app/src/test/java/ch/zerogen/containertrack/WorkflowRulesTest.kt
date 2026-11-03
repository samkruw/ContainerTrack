package ch.zerogen.containertrack

import org.junit.Assert.*
import org.junit.Test

class WorkflowRulesTest {
    @Test fun deliveryFiveOfSixIsReadyForFill() {
        assertEquals(WorkflowPhase.READY_FOR_FILL, workflowPhase(JobKind.DELIVERY, 1, listOf((1..5).toSet())))
    }
    @Test fun threeContainersNeedEighteenPhotos() {
        assertEquals(WorkflowPhase.READY_FOR_FILL, workflowPhase(JobKind.DELIVERY, 3, listOf((1..6).toSet(), (1..6).toSet(), (1..5).toSet())))
        assertEquals(WorkflowPhase.COMPLETE, workflowPhase(JobKind.DELIVERY, 3, List(3) { (1..6).toSet() }))
        assertEquals(WorkflowPhase.LOCAL_INCOMPLETE, workflowPhase(JobKind.DELIVERY, 3, listOf((1..6).toSet())))
    }
    @Test fun canContinueAfterFivePhotos() {
        assertTrue(canAddContainer(JobKind.DELIVERY, listOf((1..5).toSet())))
        assertFalse(canAddContainer(JobKind.DELIVERY, listOf((1..4).toSet())))
        assertTrue(canAddContainer(JobKind.DELIVERY, emptyList()))
    }
    @Test fun returnKeepsOnePhotoRule() {
        assertEquals(WorkflowPhase.COMPLETE, workflowPhase(JobKind.RETURN, 1, listOf(setOf(1))))
        assertFalse(canAddContainer(JobKind.RETURN, listOf(emptySet())))
        assertTrue(canAddContainer(JobKind.RETURN, listOf(setOf(1))))
    }
    @Test fun invalidStepsNeverComplete() {
        assertEquals(WorkflowPhase.LOCAL_INCOMPLETE, workflowPhase(JobKind.DELIVERY, 1, listOf((1..7).toSet())))
        assertEquals(WorkflowPhase.LOCAL_INCOMPLETE, workflowPhase(JobKind.RETURN, 1, listOf(setOf(0,1))))
        assertEquals(WorkflowPhase.LOCAL_INCOMPLETE, workflowPhase(JobKind.DELIVERY, 0, emptyList()))
    }
}
