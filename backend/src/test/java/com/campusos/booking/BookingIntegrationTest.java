package com.campusos.booking;

import com.campusos.booking.repository.BookingRepository;
import com.campusos.issue.repository.IssueActivityRepository;
import com.campusos.issue.repository.IssueRepository;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import com.campusos.security.AppUserDetails;
import com.campusos.user.Role;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired IssueRepository issues;
    @Autowired IssueActivityRepository activities;
    @Autowired ResourceRepository resources;
    @Autowired BookingRepository bookings;
    @Autowired PasswordEncoder encoder;

    private User student;
    private User admin;
    private CampusResource openLab;
    private CampusResource approvalHall;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        activities.deleteAll();
        issues.deleteAll();
        resources.deleteAll();
        users.deleteAll();
        student = users.save(new User("student@test.edu", encoder.encode("Student@123"), "Stu", Role.STUDENT, "CS"));
        admin = users.save(new User("admin@test.edu", encoder.encode("Admin@123"), "Admin", Role.CAMPUS_ADMIN, "Admin"));
        openLab = resources.save(new CampusResource("Lab A", ResourceEnums.Type.COMPUTER_LAB,
                "Engineering Block", "101", 40, "lab", false, "projector"));
        approvalHall = resources.save(new CampusResource("Seminar Hall", ResourceEnums.Type.CLASSROOM,
                "Main Block", "H1", 100, "hall", true, "projector,mic"));
    }

    private String bookingJson(Long resourceId, Instant start, Instant end) {
        return "{\"resourceId\":" + resourceId + ",\"purpose\":\"Lab session\",\"startAt\":\""
                + start + "\",\"endAt\":\"" + end + "\"}";
    }

    @Test
    void overlappingBookingConflictsButAdjacentSucceeds() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/v1/bookings").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(openLab.getId(), start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/v1/bookings").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(openLab.getId(), start.plus(1, ChronoUnit.HOURS), end.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"))
                .andExpect(jsonPath("$.alternatives.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Adjacent: starts exactly when the first ends.
        mockMvc.perform(post("/api/v1/bookings").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(openLab.getId(), end, end.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isCreated());
    }

    @Test
    void approvalRequiredBookingWaitsForAdmin() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        String body = mockMvc.perform(post("/api/v1/bookings").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(approvalHall.getId(), start, start.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(post("/api/v1/bookings/" + id + "/approve").with(user(new AppUserDetails(student))).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/bookings/" + id + "/approve").with(user(new AppUserDetails(admin))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void analyticsOverviewReturnsKpis() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview").with(user(new AppUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResources").value(2));
    }
}
