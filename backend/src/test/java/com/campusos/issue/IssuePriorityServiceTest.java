package com.campusos.issue;

import com.campusos.issue.enums.IssueEnums;
import com.campusos.issue.service.IssuePriorityService;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class IssuePriorityServiceTest {

    private final IssuePriorityService service = new IssuePriorityService();

    @Test
    void safetyRiskScoresHighWithExplanation() {
        var rec = service.recommend(IssueEnums.Category.SAFETY, IssueEnums.Priority.LOW, 1, true, Instant.now());
        assertTrue(rec.score() >= 40, "safety issue should score at least 40, was " + rec.score());
        assertTrue(rec.explanation().contains("safety"), rec.explanation());
    }

    @Test
    void manyAffectedPeopleRaisesPriority() {
        var few = service.recommend(IssueEnums.Category.OTHER, IssueEnums.Priority.LOW, 1, false, Instant.now());
        var many = service.recommend(IssueEnums.Category.OTHER, IssueEnums.Priority.LOW, 60, false, Instant.now());
        assertTrue(many.score() > few.score());
        assertTrue(many.explanation().contains("50+"), many.explanation());
    }

    @Test
    void criticalRequestReachesCriticalBand() {
        var rec = service.recommend(IssueEnums.Category.SAFETY, IssueEnums.Priority.CRITICAL, 60, true, Instant.now());
        assertEquals(IssueEnums.Priority.CRITICAL, rec.priority());
    }

    @Test
    void baselineIssueIsLow() {
        var rec = service.recommend(IssueEnums.Category.CLEANING, IssueEnums.Priority.LOW, 1, false, Instant.now());
        assertEquals(IssueEnums.Priority.LOW, rec.priority());
        assertTrue(rec.explanation().contains("/100"), rec.explanation());
    }
}
