package com.campusos.resource.service;

import com.campusos.common.exception.BusinessException;
import com.campusos.resource.dto.ResourceDtos;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import com.campusos.security.AppUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public ResourceDtos.ResourceResponse create(ResourceDtos.CreateResourceRequest req, AppUserDetails actor) {
        if (!actor.getRole().isAdmin()) throw BusinessException.forbidden("Only administrators can create resources.");
        CampusResource r = new CampusResource(req.name().trim(), req.type(), req.building().trim(),
                trimOrNull(req.location()), req.capacity(), trimOrNull(req.description()),
                Boolean.TRUE.equals(req.requiresApproval()), joinEquipment(req.equipment()));
        return toResponse(resourceRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<ResourceDtos.ResourceResponse> list(ResourceEnums.Type type, String building, Integer minCapacity, String q) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        String b = (building == null || building.isBlank()) ? null : building.trim();
        return resourceRepository.search(type, b, minCapacity, query).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ResourceDtos.ResourceResponse detail(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public ResourceDtos.ResourceResponse update(Long id, ResourceDtos.UpdateResourceRequest req, AppUserDetails actor) {
        if (!actor.getRole().isAdmin()) throw BusinessException.forbidden("Only administrators can update resources.");
        CampusResource r = load(id);
        if (req.name() != null && !req.name().isBlank()) r.setName(req.name().trim());
        if (req.type() != null) r.setType(req.type());
        if (req.building() != null && !req.building().isBlank()) r.setBuilding(req.building().trim());
        if (req.location() != null) r.setLocation(trimOrNull(req.location()));
        if (req.capacity() != null) r.setCapacity(req.capacity());
        if (req.description() != null) r.setDescription(trimOrNull(req.description()));
        if (req.status() != null) r.setStatus(req.status());
        if (req.requiresApproval() != null) r.setRequiresApproval(req.requiresApproval());
        if (req.equipment() != null) r.setEquipmentTags(joinEquipment(req.equipment()));
        r.touch();
        return toResponse(resourceRepository.save(r));
    }

    public CampusResource load(Long id) {
        return resourceRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Resource not found."));
    }

    public ResourceDtos.ResourceResponse toResponse(CampusResource r) {
        List<String> equipment = (r.getEquipmentTags() == null || r.getEquipmentTags().isBlank())
                ? List.of()
                : Arrays.stream(r.getEquipmentTags().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new ResourceDtos.ResourceResponse(r.getId(), r.getName(), r.getType().name(), r.getBuilding(),
                r.getLocation(), r.getCapacity(), r.getDescription(), r.getStatus().name(), r.isRequiresApproval(),
                equipment, r.getCreatedAt(), r.getUpdatedAt());
    }

    private String joinEquipment(List<String> equipment) {
        if (equipment == null || equipment.isEmpty()) return null;
        return String.join(",", equipment.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private String trimOrNull(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}
