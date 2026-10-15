package com.campusos.booking.entity;

import com.campusos.booking.enums.BookingEnums;
import com.campusos.resource.entity.CampusResource;
import com.campusos.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private CampusResource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 300)
    private String purpose;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingEnums.Status status;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected Booking() {}

    public Booking(CampusResource resource, User owner, String purpose, Instant startAt, Instant endAt,
                   BookingEnums.Status status) {
        this.resource = resource;
        this.owner = owner;
        this.purpose = purpose;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public CampusResource getResource() { return resource; }
    public User getOwner() { return owner; }
    public String getPurpose() { return purpose; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public BookingEnums.Status getStatus() { return status; }
    public void setStatus(BookingEnums.Status status) { this.status = status; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
}
