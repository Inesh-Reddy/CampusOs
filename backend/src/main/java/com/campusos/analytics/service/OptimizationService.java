package com.campusos.analytics.service;

import com.campusos.analytics.dto.AnalyticsDtos;
import com.campusos.booking.enums.BookingEnums;
import com.campusos.booking.repository.BookingRepository;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class OptimizationService {
    private final com.campusos.resource.repository.ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;

    public OptimizationService(com.campusos.resource.repository.ResourceRepository resourceRepository,
                               BookingRepository bookingRepository) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<AnalyticsDtos.RecommendationResponse> recommend(AnalyticsDtos.OptimizationRequest req) {
        ResourceEnums.Type type;
        try {
            type = ResourceEnums.Type.valueOf(req.resourceType());
        } catch (IllegalArgumentException ex) {
            throw new com.campusos.common.exception.BusinessException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Unknown resourceType: " + req.resourceType());
        }
        int needCapacity = req.capacity() == null ? 1 : req.capacity();
        Set<String> needEquipment = req.equipment() == null ? Set.of()
                : req.equipment().stream().map(s -> s.trim().toLowerCase()).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());

        List<AnalyticsDtos.RecommendationResponse> scored = new ArrayList<>();
        for (CampusResource r : resourceRepository.findByTypeAndStatus(type, ResourceEnums.Status.ACTIVE)) {
            if (r.getCapacity() != null && r.getCapacity() < needCapacity) continue;
            Set<String> have = r.getEquipmentTags() == null ? Set.of()
                    : Arrays.stream(r.getEquipmentTags().split(",")).map(s -> s.trim().toLowerCase())
                        .filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
            if (!have.containsAll(needEquipment)) continue;

            boolean free = bookingRepository.findOverlapping(r.getId(),
                    List.of(BookingEnums.Status.PENDING, BookingEnums.Status.CONFIRMED),
                    req.startAt(), req.endAt()).isEmpty();

            double score = 0;
            List<String> why = new ArrayList<>();
            // Capacity fit (30): closest sufficient capacity scores highest.
            int cap = r.getCapacity() == null ? needCapacity : r.getCapacity();
            double capScore = Math.max(0, 30 - Math.min(30, (cap - needCapacity) * 1.0));
            score += capScore;
            why.add(String.format("capacity %d for %d needed (+%.0f)", cap, needCapacity, capScore));
            // Equipment fit (25)
            double eqScore = needEquipment.isEmpty() ? 25 : 25.0 * needEquipment.stream().filter(have::contains).count() / needEquipment.size();
            score += eqScore;
            why.add(String.format("equipment match (+%.0f)", eqScore));
            // Availability (25)
            if (free) { score += 25; why.add("free at requested time (+25)"); }
            else { why.add("busy at requested time (+0)"); }
            // Location (10)
            if (req.building() != null && req.building().equalsIgnoreCase(r.getBuilding())) {
                score += 10; why.add("same building (+10)");
            }
            // Utilisation balance (10): prefer less-booked resources.
            long bookings = bookingRepository.countByResource(List.of(BookingEnums.Status.CONFIRMED)).stream()
                    .filter(row -> ((Number) row[0]).longValue() == r.getId())
                    .mapToLong(row -> ((Number) row[1]).longValue()).findFirst().orElse(0);
            double bal = Math.max(0, 10 - Math.min(10, bookings));
            score += bal;
            why.add(String.format("balances utilisation, %d prior bookings (+%.0f)", bookings, bal));

            double utilEstimate = Math.round(Math.min(1.0, (bookings * 2.0 + 2.0) / 70.0) * 1000.0) / 1000.0;
            scored.add(new AnalyticsDtos.RecommendationResponse(r.getId(), r.getName(), r.getBuilding(),
                    r.getCapacity(), Math.round(score * 10.0) / 10.0,
                    "Score " + String.format("%.1f", score) + "/100: " + String.join("; ", why)
                            + ". Estimate only — availability is re-checked when you book.",
                    utilEstimate, req.startAt(), req.endAt()));
        }
        scored.sort(Comparator.comparingDouble(AnalyticsDtos.RecommendationResponse::score).reversed());
        return scored.stream().limit(5).toList();
    }
}
