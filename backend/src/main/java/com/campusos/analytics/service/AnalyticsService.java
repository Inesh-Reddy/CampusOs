package com.campusos.analytics.service;

import com.campusos.analytics.dto.AnalyticsDtos;
import com.campusos.booking.enums.BookingEnums;
import com.campusos.booking.repository.BookingRepository;
import com.campusos.booking.service.BookingService;
import com.campusos.issue.enums.IssueEnums;
import com.campusos.issue.repository.IssueRepository;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    private static final List<IssueEnums.Status> OPEN = List.of(IssueEnums.Status.REPORTED, IssueEnums.Status.TRIAGED,
            IssueEnums.Status.ASSIGNED, IssueEnums.Status.IN_PROGRESS);

    private final IssueRepository issueRepository;
    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final BookingService bookingService;

    public AnalyticsService(IssueRepository issueRepository, BookingRepository bookingRepository,
                            ResourceRepository resourceRepository, BookingService bookingService) {
        this.issueRepository = issueRepository;
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.bookingService = bookingService;
    }

    public AnalyticsDtos.OverviewResponse overview(Instant from, Instant to) {
        long total = issueRepository.count();
        long open = issueRepository.countByStatusIn(OPEN);
        long critical = issueRepository.countByPriorityAndStatusIn(IssueEnums.Priority.CRITICAL, OPEN);
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
        long overdue = issueRepository.findOverdue(OPEN, cutoff).size();

        var resolved = issueRepository.findResolvedBetween(from, to);
        double avgHours = resolved.stream()
                .filter(i -> i.getResolvedAt() != null)
                .mapToDouble(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes() / 60.0)
                .average().orElse(0.0);
        avgHours = Math.round(avgHours * 10.0) / 10.0;

        long resourceCount = resourceRepository.count();
        long activeBookings = bookingRepository.countActiveInWindow(
                List.of(BookingEnums.Status.CONFIRMED, BookingEnums.Status.PENDING), from, to);
        double windowHours = Math.max(1, Duration.between(from, to).toHours());
        // Utilisation estimate: booked resource-windows vs total resource-windows (capped 0..1).
        double utilisation = resourceCount == 0 ? 0
                : Math.min(1.0, (double) activeBookings / (resourceCount * Math.max(1, windowHours / 8.0)));
        utilisation = Math.round(utilisation * 1000.0) / 1000.0;

        return new AnalyticsDtos.OverviewResponse(total, open, critical, overdue, avgHours, resolved.size(),
                utilisation, bookingService.conflictAttempts(), bookingRepository.count(), resourceCount);
    }

    public AnalyticsDtos.IssuesAnalyticsResponse issuesAnalytics() {
        List<AnalyticsDtos.NameCount> byCategory = issueRepository.countByCategory().stream()
                .map(r -> new AnalyticsDtos.NameCount(String.valueOf(r[0]), ((Number) r[1]).longValue())).toList();
        List<AnalyticsDtos.NameCount> byStatus = issueRepository.countByStatus().stream()
                .map(r -> new AnalyticsDtos.NameCount(String.valueOf(r[0]), ((Number) r[1]).longValue())).toList();
        Map<String, Long> buildings = new LinkedHashMap<>();
        issueRepository.findAll().forEach(i -> buildings.merge(i.getBuilding(), 1L, Long::sum));
        List<AnalyticsDtos.NameCount> topBuildings = buildings.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .map(e -> new AnalyticsDtos.NameCount(e.getKey(), e.getValue())).toList();
        return new AnalyticsDtos.IssuesAnalyticsResponse(byCategory, byStatus, topBuildings);
    }

    public List<AnalyticsDtos.ResourceUsageResponse> resourceUsage() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : bookingRepository.countByResource(List.of(BookingEnums.Status.CONFIRMED))) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        List<AnalyticsDtos.ResourceUsageResponse> out = new ArrayList<>();
        for (CampusResource r : resourceRepository.findAll()) {
            long c = counts.getOrDefault(r.getId(), 0L);
            double bookedHours = c * 2.0; // average booking length assumption, documented as estimate
            double util = Math.min(1.0, bookedHours / (7 * 10.0));
            out.add(new AnalyticsDtos.ResourceUsageResponse(r.getId(), r.getName(), c,
                    Math.round(bookedHours * 10.0) / 10.0, Math.round(util * 1000.0) / 1000.0));
        }
        out.sort(Comparator.comparingLong(AnalyticsDtos.ResourceUsageResponse::bookingCount).reversed());
        return out;
    }
}
