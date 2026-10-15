package com.campusos.resource.entity;

import com.campusos.resource.enums.ResourceEnums;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resources")
public class CampusResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ResourceEnums.Type type;

    @Column(nullable = false, length = 120)
    private String building;

    @Column(length = 160)
    private String location;

    private Integer capacity;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceEnums.Status status = ResourceEnums.Status.ACTIVE;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "equipment_tags", length = 500)
    private String equipmentTags;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CampusResource() {}

    public CampusResource(String name, ResourceEnums.Type type, String building, String location,
                          Integer capacity, String description, boolean requiresApproval, String equipmentTags) {
        this.name = name;
        this.type = type;
        this.building = building;
        this.location = location;
        this.capacity = capacity;
        this.description = description;
        this.requiresApproval = requiresApproval;
        this.equipmentTags = equipmentTags;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ResourceEnums.Type getType() { return type; }
    public String getBuilding() { return building; }
    public String getLocation() { return location; }
    public Integer getCapacity() { return capacity; }
    public String getDescription() { return description; }
    public ResourceEnums.Status getStatus() { return status; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public String getEquipmentTags() { return equipmentTags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setType(ResourceEnums.Type type) { this.type = type; }
    public void setBuilding(String building) { this.building = building; }
    public void setLocation(String location) { this.location = location; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(ResourceEnums.Status status) { this.status = status; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public void setEquipmentTags(String equipmentTags) { this.equipmentTags = equipmentTags; }
    public void touch() { this.updatedAt = Instant.now(); }
}
