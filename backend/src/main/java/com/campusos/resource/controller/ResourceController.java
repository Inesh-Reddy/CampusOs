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

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    private final ResourceService resourceService;
    private final CurrentUser currentUser;

    public ResourceController(ResourceService resourceService, CurrentUser currentUser) {
        this.resourceService = resourceService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ResourceDtos.ResourceResponse> create(@Valid @RequestBody ResourceDtos.CreateResourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(req, currentUser.require()));
    }

    @GetMapping
    public List<ResourceDtos.ResourceResponse> list(@RequestParam(required = false) ResourceEnums.Type type,
                                                    @RequestParam(required = false) String building,
                                                    @RequestParam(required = false) Integer minCapacity,
                                                    @RequestParam(required = false) String q) {
        return resourceService.list(type, building, minCapacity, q);
    }

    @GetMapping("/{id}")
    public ResourceDtos.ResourceResponse detail(@PathVariable Long id) {
        currentUser.require();
        return resourceService.detail(id);
    }

    @PatchMapping("/{id}")
    public ResourceDtos.ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceDtos.UpdateResourceRequest req) {
        return resourceService.update(id, req, currentUser.require());
    }
}
