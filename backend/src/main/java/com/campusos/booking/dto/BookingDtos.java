package com.campusos.booking.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public final class BookingDtos {
    private BookingDtos() {}

    public record CreateBookingRequest(
            @NotNull(message = "Resource is required.") Long resourceId,
            @NotBlank(message = "Purpose is required.") @Size(max = 300) String purpose,
            @Schema(example = "2026-10-20T09:00:00Z") @NotNull(message = "Start time is required.") Instant startAt,
            @NotNull(message = "End time is required.") Instant endAt) {}

    public record ActionRequest(@Size(max = 500) String reason) {}

    public record BookingResponse(Long id, Long resourceId, String resourceName, Long ownerId, String ownerName,
                                  String purpose, Instant startAt, Instant endAt, String status,
                                  String cancelReason, Instant createdAt) {}

    public record AlternativeSlot(Instant startAt, Instant endAt, String reason) {}

    public record ConflictResponse(@Schema(example = "BOOKING_CONFLICT") String code, String message, List<AlternativeSlot> alternatives) {}
}
