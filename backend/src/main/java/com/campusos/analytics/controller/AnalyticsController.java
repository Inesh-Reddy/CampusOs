package com.campusos.analytics.controller;

import com.campusos.analytics.dto.AnalyticsDtos;
import com.campusos.analytics.service.AnalyticsService;
import com.campusos.analytics.service.OptimizationService;
import com.campusos.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final OptimizationService optimizationService;
    private final CurrentUser currentUser;

    public AnalyticsController(AnalyticsService analyticsService, OptimizationService optimizationService,
                               CurrentUser currentUser) {
        this.analyticsService = analyticsService;
        this.optimizationService = optimizationService;
        this.currentUser = currentUser;
    }

    @GetMapping("/overview")
    public AnalyticsDtos.OverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        currentUser.require();
        Instant f = from == null ? Instant.now().minus(30, ChronoUnit.DAYS) : from;
        Instant t = to == null ? Instant.now() : to;
        return analyticsService.overview(f, t);
    }

    @GetMapping("/issues")
    public AnalyticsDtos.IssuesAnalyticsResponse issues() {
        currentUser.require();
        return analyticsService.issuesAnalytics();
    }

    @GetMapping("/resources")
    public List<AnalyticsDtos.ResourceUsageResponse> resources() {
        currentUser.require();
        return analyticsService.resourceUsage();
    }

    @GetMapping("/utilization")
    public AnalyticsDtos.OverviewResponse utilization(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return overview(from, to);
    }

    @PostMapping("/optimization/recommendations")
    public List<AnalyticsDtos.RecommendationResponse> recommendations(@Valid @RequestBody AnalyticsDtos.OptimizationRequest req) {
        currentUser.require();
        return optimizationService.recommend(req);
    }
}
