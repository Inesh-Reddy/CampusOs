package com.campusos.issue.controller;

import com.campusos.common.response.PageResponse;
import com.campusos.issue.dto.IssueDtos;
import com.campusos.issue.enums.IssueEnums;
import com.campusos.issue.service.IssueService;
import com.campusos.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campusos.common.exception.ApiError;

import java.util.List;

@Tag(name = "Issues", description = "Report, triage, assign, and track campus issues. Lists and details are scoped server-side to what the caller's role may see.")
@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {
    private final IssueService issueService;
    private final CurrentUser currentUser;

    public IssueController(IssueService issueService, CurrentUser currentUser) {
        this.issueService = issueService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Report a new issue", description = "Returns the created issue with its ISS- reference and the explainable priority recommendation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Issue created."),
            @ApiResponse(responseCode = "400", description = "Validation failed; fieldErrors lists each invalid field.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    public ResponseEntity<IssueDtos.IssueResponse> create(@Valid @RequestBody IssueDtos.CreateIssueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.create(req, currentUser.require()));
    }

    @Operation(summary = "List issues visible to the caller", description = "Students and faculty only ever receive their own issues; technicians receive issues assigned to them. Supports page, size, and sort query parameters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged issues."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public PageResponse<IssueDtos.IssueResponse> list(
            @RequestParam(required = false) IssueEnums.Status status,
            @RequestParam(required = false) IssueEnums.Category category,
            @RequestParam(required = false) IssueEnums.Priority priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "false") boolean assignedToMe,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = issueService.list(currentUser.require(), status, category, priority, q, mine, assignedToMe, pageable);
        return PageResponse.from(page, r -> r);
    }

    @Operation(summary = "List overdue open issues (staff only)", description = "Open issues older than the configured overdue threshold (48 hours by default).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overdue issues."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Staff access required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/overdue")
    public List<IssueDtos.IssueResponse> overdue() {
        return issueService.overdue(currentUser.require());
    }

    @Operation(summary = "Get one issue with its activity history and allowed transitions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue detail."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found, or not visible to the caller.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public IssueDtos.IssueDetailResponse detail(@PathVariable Long id) {
        return issueService.detail(id, currentUser.require());
    }

    @Operation(summary = "Assign a technician (administrators only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician assigned; issue moves to ASSIGNED."),
            @ApiResponse(responseCode = "400", description = "Validation failed.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only administrators can assign, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Issue or technician not found.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Issue is in a terminal state and cannot be assigned (ILLEGAL_TRANSITION).", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Selected user is not an active technician (BUSINESS_RULE).", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{id}/assign")
    public IssueDtos.IssueResponse assign(@PathVariable Long id, @Valid @RequestBody IssueDtos.AssignRequest req) {
        return issueService.assign(id, req.technicianId(), currentUser.require());
    }

    @Operation(summary = "Move an issue to a new status", description = "Only transitions allowed by the issue workflow are accepted. Resolving requires a note. Every transition is audited.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue moved."),
            @ApiResponse(responseCode = "400", description = "Validation failed.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller may not act on this issue, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Transition not allowed from the current status (ILLEGAL_TRANSITION).", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violated, e.g. resolving without notes (BUSINESS_RULE).", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{id}/transitions")
    public IssueDtos.IssueResponse transition(@PathVariable Long id, @Valid @RequestBody IssueDtos.TransitionRequest req) {
        return issueService.transition(id, req, currentUser.require());
    }
}
