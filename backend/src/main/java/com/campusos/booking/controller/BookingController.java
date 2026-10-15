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

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUser currentUser;

    public BookingController(BookingService bookingService, CurrentUser currentUser) {
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

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

    @GetMapping
    public PageResponse<BookingDtos.BookingResponse> list(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) BookingEnums.Status status,
            @RequestParam(defaultValue = "false") boolean mine,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(bookingService.list(currentUser.require(), resourceId, status, mine, pageable), r -> r);
    }

    @GetMapping("/{id}")
    public BookingDtos.BookingResponse detail(@PathVariable Long id) {
        return bookingService.detail(id, currentUser.require());
    }

    @PostMapping("/{id}/approve")
    public BookingDtos.BookingResponse approve(@PathVariable Long id) {
        return bookingService.approve(id, currentUser.require());
    }

    @PostMapping("/{id}/reject")
    public BookingDtos.BookingResponse reject(@PathVariable Long id, @RequestBody(required = false) BookingDtos.ActionRequest req) {
        return bookingService.reject(id, req, currentUser.require());
    }

    @PostMapping("/{id}/cancel")
    public BookingDtos.BookingResponse cancel(@PathVariable Long id, @RequestBody(required = false) BookingDtos.ActionRequest req) {
        return bookingService.cancel(id, req, currentUser.require());
    }
}

@RestController
@RequestMapping("/api/v1/resources/{resourceId}/schedule")
class ScheduleController {
    private final BookingService bookingService;
    private final CurrentUser currentUser;

    ScheduleController(BookingService bookingService, CurrentUser currentUser) {
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<BookingDtos.BookingResponse> schedule(@PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        currentUser.require();
        return bookingService.schedule(resourceId, from, to);
    }
}
