package com.campusos.booking.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class BookingDtos {
    private BookingDtos() {}

    public record CreateBookingRequest(
            @NotNull(message = "Resource is required.") Long resourceId,
            @NotBlank(message = "Purpose is required.") @Size(max = 300) String purpose,
            @NotNull(message = "Start time is required.") Instant startAt,
            @NotNull(message = "End time is required.") Instant endAt) {}

    public record ActionRequest(@Size(max = 500) String reason) {}

    public record BookingResponse(Long id, Long resourceId, String resourceName, Long ownerId, String ownerName,
                                  String purpose, Instant startAt, Instant endAt, String status,
                                  String cancelReason, Instant createdAt) {}

    public record AlternativeSlot(Instant startAt, Instant endAt, String reason) {}

    public record ConflictResponse(String code, String message, List<AlternativeSlot> alternatives) {}
}
