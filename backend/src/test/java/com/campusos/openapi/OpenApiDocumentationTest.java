package com.campusos.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void apiDocsArePublicAndDescribeCampusOs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CampusOS API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3")));
    }

    @Test
    void allControllerPathsAreDocumented() throws Exception {
        JsonNode paths = apiDocs().path("paths");
        List<String> expected = List.of(
                "/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/me",
                "/api/v1/issues", "/api/v1/issues/overdue", "/api/v1/issues/{id}",
                "/api/v1/issues/{id}/assign", "/api/v1/issues/{id}/transitions",
                "/api/v1/resources", "/api/v1/resources/{id}", "/api/v1/resources/{resourceId}/schedule",
                "/api/v1/bookings", "/api/v1/bookings/{id}", "/api/v1/bookings/{id}/approve",
                "/api/v1/bookings/{id}/reject", "/api/v1/bookings/{id}/cancel",
                "/api/v1/analytics/overview", "/api/v1/analytics/issues", "/api/v1/analytics/resources",
                "/api/v1/analytics/utilization", "/api/v1/analytics/optimization/recommendations",
                "/api/v1/admin/users", "/api/v1/admin/users/technicians");
        for (String path : expected) {
            assertTrue(paths.has(path), "missing documented path: " + path);
        }
    }

    @Test
    void importantSchemasArePresent() throws Exception {
        JsonNode schemas = apiDocs().path("components").path("schemas");
        for (String schema : List.of("ApiError", "CreateIssueRequest", "IssueResponse",
                "BookingResponse", "ConflictResponse", "OverviewResponse", "LoginRequest")) {
            assertTrue(schemas.has(schema), "missing schema: " + schema + " in " + schemas.fieldNames());
        }
        assertTrue(schemas.path("ApiError").path("properties").has("fieldErrors"));
        assertTrue(schemas.path("ConflictResponse").path("properties").has("alternatives"));
    }

    @Test
    void securitySchemesMatchSessionAndCsrfImplementation() throws Exception {
        JsonNode schemes = apiDocs().path("components").path("securitySchemes");
        assertEquals("apiKey", schemes.path("sessionCookie").path("type").asText());
        assertEquals("cookie", schemes.path("sessionCookie").path("in").asText());
        assertEquals("JSESSIONID", schemes.path("sessionCookie").path("name").asText());
        assertEquals("header", schemes.path("csrfToken").path("in").asText());
        assertEquals("X-XSRF-TOKEN", schemes.path("csrfToken").path("name").asText());
        assertFalse(schemes.has("bearer"), "no bearer-token scheme may be documented");
        assertFalse(apiDocs().toString().contains("\"bearer\""), "spec must not mention bearer auth");
    }

    @Test
    void loginIsPublicAndBookingConflictIsDocumented() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode login = docs.path("paths").path("/api/v1/auth/login").path("post");
        assertTrue(login.path("security").isMissingNode() || login.path("security").isEmpty(),
                "login must not require security schemes");
        JsonNode booking = docs.path("paths").path("/api/v1/bookings").path("post");
        assertTrue(booking.path("responses").has("201"), "booking create documents 201");
        assertTrue(booking.path("responses").has("409"), "booking create documents 409");
    }

    @Test
    void swaggerUiIsReachable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void structuralLintEveryOperationHasResponsesAndTags() throws Exception {
        JsonNode docs = apiDocs();
        List<String> operationIds = new ArrayList<>();
        docs.path("paths").fields().forEachRemaining(pathEntry -> {
            pathEntry.getValue().fields().forEachRemaining(opEntry -> {
                JsonNode operation = opEntry.getValue();
                assertFalse(operation.path("responses").isEmpty(),
                        "operation without responses: " + pathEntry.getKey());
                assertFalse(operation.path("tags").isEmpty(),
                        "operation without tags: " + pathEntry.getKey());
                String id = operation.path("operationId").asText("");
                assertFalse(id.isBlank(), "operation without operationId: " + pathEntry.getKey());
                assertFalse(operationIds.contains(id), "duplicate operationId: " + id);
                operationIds.add(id);
            });
        });
        assertFalse(operationIds.isEmpty());
    }
}
