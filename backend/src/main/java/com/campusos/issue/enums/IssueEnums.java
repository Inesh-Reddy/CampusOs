package com.campusos.issue.enums;

public final class IssueEnums {
    private IssueEnums() {}

    public enum Category {
        ELECTRICAL, PLUMBING, LAB_EQUIPMENT, COMPUTER_SYSTEMS, CLASSROOM_EQUIPMENT, NETWORK, CLEANING, SAFETY, OTHER
    }

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    public enum Status { REPORTED, TRIAGED, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REJECTED, CANCELLED }
}
