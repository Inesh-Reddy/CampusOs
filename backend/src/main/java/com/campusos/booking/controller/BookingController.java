package com.campusos.booking.controller;

import com.campusos.booking.dto.BookingDtos;
import com.campusos.booking.enums.BookingEnums;
import com.campusos.booking.service.BookingService;
import com.campusos.common.response.PageResponse;
import com.campusos.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campusos.common.exception.ApiError;

@Tag(name = "Bookings", description = "Book resources without overlap. Approval-required resources start as PENDING until an administrator approves.")
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUser currentUser;

    public BookingController(BookingService bookingService, CurrentUser currentUser) {
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Create a booking", description = "Overlapping active bookings are rejected with alternative free slots. Adjacent bookings (one ends exactly when the next starts) are allowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created (CONFIRMED, or PENDING when the resource requires approval).", content = @Content(schema = @Schema(implementation = BookingDtos.BookingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Overlapping booking exists (BOOKING_CONFLICT); alternatives lists free slots.", content = @Content(schema = @Schema(implementation = BookingDtos.ConflictResponse.class))),
            @ApiResponse(responseCode = "422", description = "Resource unavailable or invalid time window (BUSINESS_RULE).", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BookingDtos.CreateBookingRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(req, currentUser.require()));
        } catch (BookingService.BookingConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "BOOKING_CONFLICT",
                    "message", ex.getMessage(),
                    "alternatives", ex.getAlternatives()));
        }
    }

    @Operation(summary = "List bookings visible to the caller", description = "Students and faculty only receive their own bookings. Supports page, size, and sort query parameters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged bookings."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public PageResponse<BookingDtos.BookingResponse> list(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) BookingEnums.Status status,
            @RequestParam(defaultValue = "false") boolean mine,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(bookingService.list(currentUser.require(), resourceId, status, mine, pageable), r -> r);
    }

    @Operation(summary = "Get one booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found, or not visible to the caller.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public BookingDtos.BookingResponse detail(@PathVariable Long id) {
        return bookingService.detail(id, currentUser.require());
    }

    @Operation(summary = "Approve a pending booking (administrators only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only administrators can approve, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Only PENDING bookings can be approved (ILLEGAL_TRANSITION).", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{id}/approve")
    public BookingDtos.BookingResponse approve(@PathVariable Long id) {
        return bookingService.approve(id, currentUser.require());
    }

    @Operation(summary = "Reject a pending booking (administrators only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking rejected."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only administrators can reject, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Only PENDING bookings can be rejected (ILLEGAL_TRANSITION).", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{id}/reject")
    public BookingDtos.BookingResponse reject(@PathVariable Long id, @RequestBody(required = false) BookingDtos.ActionRequest req) {
        return bookingService.reject(id, req, currentUser.require());
    }

    @Operation(summary = "Cancel a booking (owner or administrators)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only the owner or an administrator can cancel, or CSRF token missing/invalid.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Booking is already cancelled or rejected (ILLEGAL_TRANSITION).", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Booking has already ended (BUSINESS_RULE).", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{id}/cancel")
    public BookingDtos.BookingResponse cancel(@PathVariable Long id, @RequestBody(required = false) BookingDtos.ActionRequest req) {
        return bookingService.cancel(id, req, currentUser.require());
    }
}

@Tag(name = "Bookings", description = "Book resources without overlap. Approval-required resources start as PENDING until an administrator approves.")
@RestController
@RequestMapping("/api/v1/resources/{resourceId}/schedule")
class ScheduleController {
    private final BookingService bookingService;
    private final CurrentUser currentUser;

    ScheduleController(BookingService bookingService, CurrentUser currentUser) {
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List bookings for one resource in a time window", description = "Used to render availability. from and to are ISO-8601 instants.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings in the window."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid from/to parameters.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public List<BookingDtos.BookingResponse> schedule(@PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        currentUser.require();
        return bookingService.schedule(resourceId, from, to);
    }
}
