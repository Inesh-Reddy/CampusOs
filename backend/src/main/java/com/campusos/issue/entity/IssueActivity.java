package com.campusos.issue.entity;

import com.campusos.issue.enums.IssueEnums;
import com.campusos.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "issue_activities")
public class IssueActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false, length = 60)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private IssueEnums.Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private IssueEnums.Status toStatus;

    @Column(length = 2000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IssueActivity() {}

    public IssueActivity(Issue issue, User actor, String action, IssueEnums.Status fromStatus,
                         IssueEnums.Status toStatus, String note) {
        this.issue = issue;
        this.actor = actor;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Issue getIssue() { return issue; }
    public User getActor() { return actor; }
    public String getAction() { return action; }
    public IssueEnums.Status getFromStatus() { return fromStatus; }
    public IssueEnums.Status getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
