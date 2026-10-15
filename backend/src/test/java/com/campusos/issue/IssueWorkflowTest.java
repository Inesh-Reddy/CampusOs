package com.campusos.issue;

import com.campusos.issue.enums.IssueEnums.Status;
import com.campusos.issue.service.IssueWorkflow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IssueWorkflowTest {

    @Test
    void reportedCannotJumpToInProgress() {
        assertFalse(IssueWorkflow.canTransition(Status.REPORTED, Status.IN_PROGRESS));
        assertFalse(IssueWorkflow.canTransition(Status.REPORTED, Status.RESOLVED));
    }

    @Test
    void happyPathTransitionsAreAllowed() {
        assertTrue(IssueWorkflow.canTransition(Status.REPORTED, Status.TRIAGED));
        assertTrue(IssueWorkflow.canTransition(Status.TRIAGED, Status.ASSIGNED));
        assertTrue(IssueWorkflow.canTransition(Status.ASSIGNED, Status.IN_PROGRESS));
        assertTrue(IssueWorkflow.canTransition(Status.IN_PROGRESS, Status.RESOLVED));
        assertTrue(IssueWorkflow.canTransition(Status.RESOLVED, Status.CLOSED));
    }

    @Test
    void terminalStatesHaveNoOutgoingTransitions() {
        assertTrue(IssueWorkflow.allowedFrom(Status.REJECTED).isEmpty());
        assertTrue(IssueWorkflow.allowedFrom(Status.CANCELLED).isEmpty());
    }

    @Test
    void reopenPathsExist() {
        assertTrue(IssueWorkflow.canTransition(Status.RESOLVED, Status.IN_PROGRESS));
        assertTrue(IssueWorkflow.canTransition(Status.CLOSED, Status.IN_PROGRESS));
    }

    @Test
    void nullTransitionsAreRejected() {
        assertFalse(IssueWorkflow.canTransition(null, Status.TRIAGED));
        assertFalse(IssueWorkflow.canTransition(Status.REPORTED, null));
    }
}
