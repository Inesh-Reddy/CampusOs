package com.campusos.issue.entity;

import com.campusos.issue.enums.IssueEnums;
import com.campusos.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IssueEnums.Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueEnums.Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueEnums.Status status;

    @Column(nullable = false, length = 120)
    private String building;

    @Column(name = "location_detail", length = 200)
    private String locationDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "resolution_notes", length = 4000)
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    private Long version;

    protected Issue() {}

    public Issue(String reference, String title, String description, IssueEnums.Category category,
                 IssueEnums.Priority priority, String building, String locationDetail, User reporter) {
        this.reference = reference;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = IssueEnums.Status.REPORTED;
        this.building = building;
        this.locationDetail = locationDetail;
        this.reporter = reporter;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void touch() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IssueEnums.Category getCategory() { return category; }
    public IssueEnums.Priority getPriority() { return priority; }
    public void setPriority(IssueEnums.Priority priority) { this.priority = priority; }
    public IssueEnums.Status getStatus() { return status; }
    public void setStatus(IssueEnums.Status status) { this.status = status; }
    public String getBuilding() { return building; }
    public String getLocationDetail() { return locationDetail; }
    public User getReporter() { return reporter; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; this.updatedAt = createdAt; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
}
