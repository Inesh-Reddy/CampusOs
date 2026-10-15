package com.campusos.booking.service;

import com.campusos.booking.dto.BookingDtos;
import com.campusos.booking.entity.Booking;
import com.campusos.booking.enums.BookingEnums;
import com.campusos.booking.repository.BookingRepository;
import com.campusos.common.exception.BusinessException;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import com.campusos.security.AppUserDetails;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final AtomicLong conflictAttempts = new AtomicLong();

    public BookingService(BookingRepository bookingRepository, ResourceRepository resourceRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public long conflictAttempts() { return conflictAttempts.get(); }

    @Transactional
    public BookingDtos.BookingResponse create(BookingDtos.CreateBookingRequest req, AppUserDetails actor) {
        validateWindow(req.startAt(), req.endAt());
        // Pessimistic lock on the resource row serialises concurrent bookings for this resource.
        CampusResource resource = resourceRepository.findByIdForUpdate(req.resourceId())
                .orElseThrow(() -> BusinessException.notFound("Resource not found."));
        if (resource.getStatus() != ResourceEnums.Status.ACTIVE) {
            throw BusinessException.rule("Resource is not available for booking (status: " + resource.getStatus() + ").");
        }
        List<BookingEnums.Status> blocking = resource.isRequiresApproval()
                ? List.of(BookingEnums.Status.PENDING, BookingEnums.Status.CONFIRMED)
                : List.of(BookingEnums.Status.CONFIRMED, BookingEnums.Status.PENDING);
        List<Booking> conflicts = bookingRepository.findOverlapping(resource.getId(), blocking, req.startAt(), req.endAt());
        if (!conflicts.isEmpty()) {
            conflictAttempts.incrementAndGet();
            throw new BookingConflictException(suggestAlternatives(resource.getId(), req.startAt(), req.endAt()));
        }
        User owner = userRepository.findById(actor.getId())
                .orElseThrow(() -> BusinessException.notFound("User not found."));
        BookingEnums.Status initial = resource.isRequiresApproval()
                ? BookingEnums.Status.PENDING : BookingEnums.Status.CONFIRMED;
        Booking booking = new Booking(resource, owner, req.purpose().trim(), req.startAt(), req.endAt(), initial);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Page<BookingDtos.BookingResponse> list(AppUserDetails actor, Long resourceId,
                                                  BookingEnums.Status status, boolean mine, Pageable pageable) {
        Long ownerId = mine ? actor.getId() : null;
        if ((actor.getRole() == com.campusos.user.Role.STUDENT || actor.getRole() == com.campusos.user.Role.FACULTY)
                && !actor.getRole().isAdmin()) ownerId = actor.getId();
        return bookingRepository.search(ownerId, resourceId, status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingDtos.BookingResponse detail(Long id, AppUserDetails actor) {
        Booking b = loadVisible(id, actor);
        return toResponse(b);
    }

    @Transactional
    public BookingDtos.BookingResponse approve(Long id, AppUserDetails actor) {
        if (!actor.getRole().isAdmin()) throw BusinessException.forbidden("Only administrators can approve bookings.");
        Booking b = load(id);
        if (b.getStatus() != BookingEnums.Status.PENDING) {
            throw BusinessException.conflict("ILLEGAL_TRANSITION", "Only PENDING bookings can be approved.");
        }
        b.setStatus(BookingEnums.Status.CONFIRMED);
        return toResponse(bookingRepository.save(b));
    }

    @Transactional
    public BookingDtos.BookingResponse reject(Long id, BookingDtos.ActionRequest req, AppUserDetails actor) {
        if (!actor.getRole().isAdmin()) throw BusinessException.forbidden("Only administrators can reject bookings.");
        Booking b = load(id);
        if (b.getStatus() != BookingEnums.Status.PENDING) {
            throw BusinessException.conflict("ILLEGAL_TRANSITION", "Only PENDING bookings can be rejected.");
        }
        b.setStatus(BookingEnums.Status.REJECTED);
        if (req != null) b.setCancelReason(req.reason());
        return toResponse(bookingRepository.save(b));
    }

    @Transactional
    public BookingDtos.BookingResponse cancel(Long id, BookingDtos.ActionRequest req, AppUserDetails actor) {
        Booking b = load(id);
        boolean owner = b.getOwner().getId().equals(actor.getId());
        if (!owner && !actor.getRole().isAdmin()) {
            throw BusinessException.forbidden("You can only cancel your own bookings.");
        }
        if (b.getStatus() == BookingEnums.Status.CANCELLED || b.getStatus() == BookingEnums.Status.REJECTED) {
            throw BusinessException.conflict("ILLEGAL_TRANSITION", "Booking is already " + b.getStatus() + ".");
        }
        if (b.getEndAt().isBefore(Instant.now()) && !actor.getRole().isAdmin()) {
            throw BusinessException.rule("Cannot cancel a booking that has already ended.");
        }
        b.setStatus(BookingEnums.Status.CANCELLED);
        if (req != null) b.setCancelReason(req.reason());
        return toResponse(bookingRepository.save(b));
    }

    @Transactional(readOnly = true)
    public List<BookingDtos.BookingResponse> schedule(Long resourceId, Instant from, Instant to) {
        return bookingRepository.findSchedule(resourceId,
                List.of(BookingEnums.Status.PENDING, BookingEnums.Status.CONFIRMED), from, to)
                .stream().map(this::toResponse).toList();
    }

    public List<BookingDtos.AlternativeSlot> suggestAlternatives(Long resourceId, Instant start, Instant end) {
        Duration len = Duration.between(start, end);
        List<BookingDtos.AlternativeSlot> out = new ArrayList<>();
        List<BookingEnums.Status> blocking = List.of(BookingEnums.Status.PENDING, BookingEnums.Status.CONFIRMED);
        Instant cursor = end;
        for (int i = 0; i < 3; i++) {
            Instant s = cursor;
            Instant e = s.plus(len);
            List<Booking> clash = bookingRepository.findOverlapping(resourceId, blocking, s, e);
            if (clash.isEmpty()) {
                out.add(new BookingDtos.AlternativeSlot(s, e, "Next free slot after the requested window."));
                cursor = e.plus(30, ChronoUnit.MINUTES);
            } else {
                cursor = clash.get(clash.size() - 1).getEndAt();
            }
        }
        if (out.isEmpty()) {
            Instant s = start.plus(1, ChronoUnit.DAYS);
            out.add(new BookingDtos.AlternativeSlot(s, s.plus(len), "Same time tomorrow (please verify in schedule)."));
        }
        return out;
    }

    private void validateWindow(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw BusinessException.rule("Start time must be before end time.");
        }
        if (end.isBefore(Instant.now().minus(5, ChronoUnit.MINUTES))) {
            throw BusinessException.rule("Cannot book a slot that has already ended.");
        }
        if (Duration.between(start, end).toHours() > 12) {
            throw BusinessException.rule("A single booking cannot exceed 12 hours.");
        }
    }

    private Booking load(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Booking not found."));
    }

    private Booking loadVisible(Long id, AppUserDetails actor) {
        Booking b = load(id);
        if (actor.getRole().isAdmin() || actor.getRole().name().equals("TECHNICIAN")) return b;
        if (b.getOwner().getId().equals(actor.getId())) return b;
        throw BusinessException.notFound("Booking not found.");
    }

    public BookingDtos.BookingResponse toResponse(Booking b) {
        return new BookingDtos.BookingResponse(b.getId(), b.getResource().getId(), b.getResource().getName(),
                b.getOwner().getId(), b.getOwner().getDisplayName(), b.getPurpose(), b.getStartAt(), b.getEndAt(),
                b.getStatus().name(), b.getCancelReason(), b.getCreatedAt());
    }

    public static class BookingConflictException extends RuntimeException {
        private final List<BookingDtos.AlternativeSlot> alternatives;
        public BookingConflictException(List<BookingDtos.AlternativeSlot> alternatives) {
            super("The resource is already booked for an overlapping time.");
            this.alternatives = alternatives;
        }
        public List<BookingDtos.AlternativeSlot> getAlternatives() { return alternatives; }
    }
}
