package com.campusos.config;

import com.campusos.booking.entity.Booking;
import com.campusos.booking.enums.BookingEnums;
import com.campusos.booking.repository.BookingRepository;
import com.campusos.issue.entity.Issue;
import com.campusos.issue.entity.IssueActivity;
import com.campusos.issue.enums.IssueEnums;
import com.campusos.issue.repository.IssueActivityRepository;
import com.campusos.issue.repository.IssueRepository;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import com.campusos.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Demo data — local profile only. Passwords are local-demo-only, documented in README. */
@Component
@Profile({"local", "test"})
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final IssueRepository issues;
    private final IssueActivityRepository activities;
    private final ResourceRepository resources;
    private final BookingRepository bookings;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, IssueRepository issues, IssueActivityRepository activities,
                      ResourceRepository resources, BookingRepository bookings, PasswordEncoder encoder) {
        this.users = users;
        this.issues = issues;
        this.activities = activities;
        this.resources = resources;
        this.bookings = bookings;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        log.info("Seeding CampusOS demo data (local profile)");

        User admin = save("admin@campus.edu", "Admin@123", "Aisha Khan (Campus Admin)", Role.CAMPUS_ADMIN, "Administration");
        User dept = save("dept@campus.edu", "Dept@123", "Rohan Mehta (Dept Admin)", Role.DEPT_ADMIN, "Computer Science");
        User tech1 = save("tech1@campus.edu", "Tech@123", "Vikram Rao (Technician)", Role.TECHNICIAN, "Facilities");
        User tech2 = save("tech2@campus.edu", "Tech@123", "Sara Thomas (Technician)", Role.TECHNICIAN, "Facilities");
        User faculty = save("faculty@campus.edu", "Faculty@123", "Dr. Neha Patel (Faculty)", Role.FACULTY, "Computer Science");
        User student = save("student@campus.edu", "Student@123", "Arjun Nair (Student)", Role.STUDENT, "Computer Science");
        User student2 = save("student2@campus.edu", "Student@123", "Meera Iyer (Student)", Role.STUDENT, "Electronics");

        CampusResource labA = resources.save(new CampusResource("Lab A — Computing Lab", ResourceEnums.Type.COMPUTER_LAB,
                "Engineering Block", "Room 101", 40, "40-seat programming lab with dual monitors.", false, "projector,whiteboard,linux"));
        CampusResource labB = resources.save(new CampusResource("Lab B — Electronics Lab", ResourceEnums.Type.COMPUTER_LAB,
                "Engineering Block", "Room 204", 30, "Electronics workbenches and oscilloscopes.", true, "oscilloscope,whiteboard"));
        CampusResource room = resources.save(new CampusResource("Seminar Hall 1", ResourceEnums.Type.CLASSROOM,
                "Main Block", "Hall 1", 120, "Tiered seminar hall.", true, "projector,mic"));
        resources.save(new CampusResource("Projector — Portable HD", ResourceEnums.Type.PROJECTOR,
                "Main Block", "Store", null, "Portable HD projector with HDMI.", false, "hdmi,remote"));
        resources.save(new CampusResource("Meeting Room 3", ResourceEnums.Type.MEETING_ROOM,
                "Admin Block", "Room 3", 12, "Board-style meeting room.", false, "tv,whiteboard"));

        Issue i1 = seedIssue("ISS-9001", "Projector not turning on in Lab A", "The ceiling projector in Lab A flickers and shuts off after 2 minutes.",
                IssueEnums.Category.CLASSROOM_EQUIPMENT, IssueEnums.Priority.HIGH, "Engineering Block", "Lab A", student, tech1,
                IssueEnums.Status.IN_PROGRESS, -2);
        Issue i2 = seedIssue("ISS-9002", "Water leakage near washroom", "Water pooling near the washroom entrance on floor 2 — slipping hazard.",
                IssueEnums.Category.SAFETY, IssueEnums.Priority.CRITICAL, "Main Block", "Floor 2", faculty, null,
                IssueEnums.Status.REPORTED, 0);
        seedIssue("ISS-9003", "Wi-Fi drops in library", "Wi-Fi disconnects every few minutes during evening study hours.",
                IssueEnums.Category.NETWORK, IssueEnums.Priority.MEDIUM, "Library", "Reading Hall", student2, tech2,
                IssueEnums.Status.ASSIGNED, -1);
        seedIssue("ISS-9004", "AC not cooling in Seminar Hall 1", "Air conditioning is ineffective during afternoon sessions.",
                IssueEnums.Category.ELECTRICAL, IssueEnums.Priority.MEDIUM, "Main Block", "Seminar Hall 1", faculty, tech1,
                IssueEnums.Status.RESOLVED, -5);

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        bookings.save(new Booking(labA, faculty, "CS201 lab session", start, start.plus(2, ChronoUnit.HOURS), BookingEnums.Status.CONFIRMED));
        bookings.save(new Booking(room, dept, "Department review meeting", start.plus(3, ChronoUnit.HOURS),
                start.plus(4, ChronoUnit.HOURS), BookingEnums.Status.PENDING));
        bookings.save(new Booking(labB, student, "Project work (demo booking)", start.plus(5, ChronoUnit.HOURS),
                start.plus(6, ChronoUnit.HOURS), BookingEnums.Status.CONFIRMED));

        activities.save(new IssueActivity(i1, student, "CREATED", null, IssueEnums.Status.REPORTED, "Issue reported."));
        activities.save(new IssueActivity(i1, dept, "ASSIGNED", IssueEnums.Status.REPORTED, IssueEnums.Status.ASSIGNED, "Assigned to Vikram Rao."));
        activities.save(new IssueActivity(i2, faculty, "CREATED", null, IssueEnums.Status.REPORTED, "Issue reported. Safety risk flagged."));
        log.info("Demo seed complete: 7 users, 5 resources, 4 issues, 3 bookings");
    }

    private User save(String email, String raw, String name, Role role, String dept) {
        return users.save(new User(email, encoder.encode(raw), name, role, dept));
    }

    private Issue seedIssue(String ref, String title, String desc, IssueEnums.Category cat, IssueEnums.Priority pri,
                            String building, String loc, User reporter, User assignee, IssueEnums.Status status, int daysAgo) {
        Issue issue = new Issue(ref, title, desc, cat, pri, building, loc, reporter);
        issue.setCreatedAt(Instant.now().plus(daysAgo, ChronoUnit.DAYS));
        issue.setAssignee(assignee);
        issue.setStatus(status);
        if (status == IssueEnums.Status.RESOLVED) {
            issue.setResolvedAt(Instant.now().minus(1, ChronoUnit.DAYS));
            issue.setResolutionNotes("Replaced faulty unit and verified cooling. (Demo resolution)");
        }
        return issues.save(issue);
    }
}
