package com.campusos.analytics.controller;

import com.campusos.analytics.dto.AnalyticsDtos;
import com.campusos.analytics.service.AnalyticsService;
import com.campusos.analytics.service.OptimizationService;
import com.campusos.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campusos.common.exception.ApiError;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Tag(name = "Analytics", description = "Campus KPIs and the resource optimization simulator. Utilisation figures are labelled estimates.")
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

    @Operation(summary = "Campus overview KPIs for a date range", description = "Defaults to the last 30 days when from/to are omitted. Empty ranges return zeros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK."),
            @ApiResponse(responseCode = "400", description = "Validation failed, including an unknown resourceType.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/overview")
    public AnalyticsDtos.OverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        currentUser.require();
        Instant f = from == null ? Instant.now().minus(30, ChronoUnit.DAYS) : from;
        Instant t = to == null ? Instant.now() : to;
        return analyticsService.overview(f, t);
    }

    @Operation(summary = "Issue counts by category, status, and top buildings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK."),
            @ApiResponse(responseCode = "400", description = "Validation failed, including an unknown resourceType.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/issues")
    public AnalyticsDtos.IssuesAnalyticsResponse issues() {
        currentUser.require();
        return analyticsService.issuesAnalytics();
    }

    @Operation(summary = "Resource usage ranking (estimates)", description = "Booked hours assume an average booking length and are documented as estimates.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK."),
            @ApiResponse(responseCode = "400", description = "Validation failed, including an unknown resourceType.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/resources")
    public List<AnalyticsDtos.ResourceUsageResponse> resources() {
        currentUser.require();
        return analyticsService.resourceUsage();
    }

    @Operation(summary = "Utilisation overview (same KPIs as /overview)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK."),
            @ApiResponse(responseCode = "400", description = "Validation failed, including an unknown resourceType.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/utilization")
    public AnalyticsDtos.OverviewResponse utilization(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return overview(from, to);
    }

    @Operation(summary = "Rank resources for a desired booking", description = "Returns up to 5 scored recommendations with explanations. Booking a recommendation re-validates availability through the booking service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK."),
            @ApiResponse(responseCode = "400", description = "Validation failed, including an unknown resourceType.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/optimization/recommendations")
    public List<AnalyticsDtos.RecommendationResponse> recommendations(@Valid @RequestBody AnalyticsDtos.OptimizationRequest req) {
        currentUser.require();
        return optimizationService.recommend(req);
    }
}
