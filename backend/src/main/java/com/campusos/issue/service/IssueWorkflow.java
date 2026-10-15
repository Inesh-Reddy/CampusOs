package com.campusos.issue.service;

import com.campusos.issue.enums.IssueEnums.Status;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class IssueWorkflow {
    private static final Map<Status, Set<Status>> ALLOWED = new EnumMap<>(Status.class);

    static {
        ALLOWED.put(Status.REPORTED, Set.of(Status.TRIAGED, Status.CANCELLED));
        ALLOWED.put(Status.TRIAGED, Set.of(Status.ASSIGNED, Status.CANCELLED, Status.REJECTED));
        ALLOWED.put(Status.ASSIGNED, Set.of(Status.IN_PROGRESS, Status.TRIAGED, Status.CANCELLED));
        ALLOWED.put(Status.IN_PROGRESS, Set.of(Status.RESOLVED, Status.ASSIGNED));
        ALLOWED.put(Status.RESOLVED, Set.of(Status.CLOSED, Status.IN_PROGRESS));
        ALLOWED.put(Status.CLOSED, Set.of(Status.IN_PROGRESS));
        ALLOWED.put(Status.REJECTED, Set.of());
        ALLOWED.put(Status.CANCELLED, Set.of());
    }

    private IssueWorkflow() {}

    public static boolean canTransition(Status from, Status to) {
        if (from == null || to == null) return false;
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<Status> allowedFrom(Status from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }
}
