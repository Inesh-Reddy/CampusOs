package com.campusos.analytics.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record OverviewResponse(long totalIssues, long openIssues, long criticalIssues, long overdueIssues,
                                   double avgResolutionHours, long resolvedInRange, double utilizationRate,
                                   long conflictAttempts, long totalBookings, long totalResources) {}

    public record NameCount(String name, long count) {}

    public record IssuesAnalyticsResponse(List<NameCount> byCategory, List<NameCount> byStatus,
                                          List<NameCount> topBuildings) {}

    public record ResourceUsageResponse(Long resourceId, String resourceName, long bookingCount,
                                        double bookedHours, double utilizationRate) {}

    public record OptimizationRequest(
            @NotNull(message = "Resource type is required.") String resourceType,
            Integer capacity,
            List<String> equipment,
            @NotNull(message = "Start time is required.") Instant startAt,
            @NotNull(message = "End time is required.") Instant endAt,
            String building) {}

    public record RecommendationResponse(Long resourceId, String resourceName, String building, Integer capacity,
                                         double score, String explanation, double utilizationEstimate,
                                         Instant startAt, Instant endAt) {}
}
