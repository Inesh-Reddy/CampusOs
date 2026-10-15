package com.campusos.security;

import com.campusos.booking.repository.BookingRepository;
import com.campusos.issue.repository.IssueActivityRepository;
import com.campusos.issue.repository.IssueRepository;
import com.campusos.resource.repository.ResourceRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired IssueRepository issues;
    @Autowired IssueActivityRepository activities;
    @Autowired ResourceRepository resources;
    @Autowired BookingRepository bookings;
    @Autowired PasswordEncoder encoder;

    private User student;
    private User student2;
    private User tech;
    private User deptAdmin;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        activities.deleteAll();
        issues.deleteAll();
        resources.deleteAll();
        users.deleteAll();
        student = save("student@test.edu", "Student@123", "Stu One", Role.STUDENT);
        student2 = save("student2@test.edu", "Student@123", "Stu Two", Role.STUDENT);
        tech = save("tech@test.edu", "Tech@123", "Tech One", Role.TECHNICIAN);
        deptAdmin = save("dept@test.edu", "Dept@123", "Dept Admin", Role.DEPT_ADMIN);
    }

    private User save(String email, String raw, String name, Role role) {
        return users.save(new User(email, encoder.encode(raw), name, role, "CS"));
    }

    private String issueJson(String title) {
        return "{\"title\":\"" + title + "\",\"description\":\"Something is broken.\","
                + "\"category\":\"NETWORK\",\"building\":\"Library\",\"locationDetail\":\"Hall\"}";
    }

    @Test
    void csrfCookieIsIssuedForSpa() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mockMvc.perform(get("/api/v1/issues")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsBadPasswordAndAcceptsGood() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.edu\",\"password\":\"Student@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@test.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void studentCannotReadAnotherStudentsIssue() throws Exception {
        String body = mockMvc.perform(post("/api/v1/issues").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(issueJson("Wi-Fi down")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(org.hamcrest.Matchers.startsWith("ISS-")))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(get("/api/v1/issues/" + id).with(user(new AppUserDetails(student2))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/issues/" + id).with(user(new AppUserDetails(student))))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotAssignTechnician() throws Exception {
        String body = mockMvc.perform(post("/api/v1/issues").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(issueJson("Projector broken")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(post("/api/v1/issues/" + id + "/assign").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":" + tech.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAssignThenTechnicianProgressesWithAudit() throws Exception {
        String body = mockMvc.perform(post("/api/v1/issues").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(issueJson("AC broken")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(post("/api/v1/issues/" + id + "/assign").with(user(new AppUserDetails(deptAdmin))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":" + tech.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // Illegal: ASSIGNED -> RESOLVED skips IN_PROGRESS and needs notes anyway.
        mockMvc.perform(post("/api/v1/issues/" + id + "/transitions").with(user(new AppUserDetails(tech))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"RESOLVED\",\"note\":\"done\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/issues/" + id + "/transitions").with(user(new AppUserDetails(tech))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\",\"note\":\"started\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/v1/issues/" + id).with(user(new AppUserDetails(tech))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void invalidIssuePayloadIs400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/issues").with(user(new AppUserDetails(student))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"\",\"category\":\"NETWORK\",\"building\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }
}
