package com.campusos.issue.dto;

import com.campusos.issue.enums.IssueEnums;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public final class IssueDtos {
    private IssueDtos() {}

    public record CreateIssueRequest(
            @Schema(example = "Projector not turning on in Lab A") @NotBlank(message = "Title is required.") @Size(max = 200) String title,
            @NotBlank(message = "Description is required.") @Size(max = 4000) String description,
            @NotNull(message = "Category is required.") IssueEnums.Category category,
            @NotBlank(message = "Building is required.") @Size(max = 120) String building,
            @Size(max = 200) String locationDetail,
            IssueEnums.Priority priority,
            @Min(value = 1, message = "Affected people must be at least 1.") Integer affectedPeople,
            Boolean safetyRisk) {}

    public record AssignRequest(@NotNull(message = "Technician is required.") Long technicianId) {}

    public record TransitionRequest(
            @NotNull(message = "Target status is required.") IssueEnums.Status toStatus,
            @Size(max = 2000) String note) {}

    public record IssueResponse(Long id, @Schema(example = "ISS-0158") String reference, String title, String description,
                                String category, String priority, String status, String building,
                                String locationDetail, Long reporterId, String reporterName,
                                Long assigneeId, String assigneeName, Instant createdAt, Instant updatedAt,
                                Instant resolvedAt, String resolutionNotes,
                                String priorityExplanation) {}

    public record ActivityResponse(Long id, String actorName, String action, String fromStatus,
                                   String toStatus, String note, Instant createdAt) {}

    public record IssueDetailResponse(IssueResponse issue, List<ActivityResponse> activity,
                                      List<String> allowedTransitions) {}
}
