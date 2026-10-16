package com.campusos.resource.controller;

import com.campusos.resource.dto.ResourceDtos;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.service.ResourceService;
import com.campusos.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campusos.common.exception.ApiError;

@Tag(name = "Resources", description = "Campus resource catalogue. Creating and updating resources is restricted to administrators.")
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    private final ResourceService resourceService;
    private final CurrentUser currentUser;

    public ResourceController(ResourceService resourceService, CurrentUser currentUser) {
        this.resourceService = resourceService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Create a resource (administrators only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resource created."),
            @ApiResponse(responseCode = "400", description = "Validation failed.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only administrators can create resources, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    public ResponseEntity<ResourceDtos.ResourceResponse> create(@Valid @RequestBody ResourceDtos.CreateResourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(req, currentUser.require()));
    }

    @Operation(summary = "List resources with optional type, building, capacity, and text filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching resources."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public List<ResourceDtos.ResourceResponse> list(@RequestParam(required = false) ResourceEnums.Type type,
                                                    @RequestParam(required = false) String building,
                                                    @RequestParam(required = false) Integer minCapacity,
                                                    @RequestParam(required = false) String q) {
        return resourceService.list(type, building, minCapacity, q);
    }

    @Operation(summary = "Get one resource")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public ResourceDtos.ResourceResponse detail(@PathVariable Long id) {
        currentUser.require();
        return resourceService.detail(id);
    }

    @Operation(summary = "Update a resource (administrators only)", description = "Only supplied, non-empty fields are changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource updated."),
            @ApiResponse(responseCode = "400", description = "Validation failed.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only administrators can update resources, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PatchMapping("/{id}")
    public ResourceDtos.ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceDtos.UpdateResourceRequest req) {
        return resourceService.update(id, req, currentUser.require());
    }
}
