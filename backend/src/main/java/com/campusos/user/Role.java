package com.campusos.user;

public enum Role {
    STUDENT, FACULTY, TECHNICIAN, DEPT_ADMIN, CAMPUS_ADMIN;

    public boolean isStaff() {
        return this == TECHNICIAN || this == DEPT_ADMIN || this == CAMPUS_ADMIN;
    }

    public boolean isAdmin() {
        return this == DEPT_ADMIN || this == CAMPUS_ADMIN;
    }
}
