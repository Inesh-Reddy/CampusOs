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

import java.util.List;

@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {
    private final IssueService issueService;
    private final CurrentUser currentUser;

    public IssueController(IssueService issueService, CurrentUser currentUser) {
        this.issueService = issueService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<IssueDtos.IssueResponse> create(@Valid @RequestBody IssueDtos.CreateIssueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.create(req, currentUser.require()));
    }

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

    @GetMapping("/overdue")
    public List<IssueDtos.IssueResponse> overdue() {
        return issueService.overdue(currentUser.require());
    }

    @GetMapping("/{id}")
    public IssueDtos.IssueDetailResponse detail(@PathVariable Long id) {
        return issueService.detail(id, currentUser.require());
    }

    @PostMapping("/{id}/assign")
    public IssueDtos.IssueResponse assign(@PathVariable Long id, @Valid @RequestBody IssueDtos.AssignRequest req) {
        return issueService.assign(id, req.technicianId(), currentUser.require());
    }

    @PostMapping("/{id}/transitions")
    public IssueDtos.IssueResponse transition(@PathVariable Long id, @Valid @RequestBody IssueDtos.TransitionRequest req) {
        return issueService.transition(id, req, currentUser.require());
    }
}
