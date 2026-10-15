package com.campusos.resource.dto;

import com.campusos.resource.enums.ResourceEnums;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class ResourceDtos {
    private ResourceDtos() {}

    public record CreateResourceRequest(
            @NotBlank(message = "Name is required.") @Size(max = 160) String name,
            @NotNull(message = "Type is required.") ResourceEnums.Type type,
            @NotBlank(message = "Building is required.") @Size(max = 120) String building,
            @Size(max = 160) String location,
            @Min(value = 1, message = "Capacity must be at least 1.") Integer capacity,
            @Size(max = 1000) String description,
            Boolean requiresApproval,
            List<String> equipment) {}

    public record UpdateResourceRequest(
            @Size(max = 160) String name,
            ResourceEnums.Type type,
            @Size(max = 120) String building,
            @Size(max = 160) String location,
            @Min(value = 1) Integer capacity,
            @Size(max = 1000) String description,
            ResourceEnums.Status status,
            Boolean requiresApproval,
            List<String> equipment) {}

    public record ResourceResponse(Long id, String name, String type, String building, String location,
                                   Integer capacity, String description, String status, boolean requiresApproval,
                                   List<String> equipment, Instant createdAt, Instant updatedAt) {}
}
