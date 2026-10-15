package com.campusos.analytics;

import com.campusos.analytics.dto.AnalyticsDtos;
import com.campusos.analytics.service.OptimizationService;
import com.campusos.booking.repository.BookingRepository;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OptimizationServiceTest {

    @Test
    void ranksFreeExactFitAboveBusyOversized() {
        ResourceRepository resources = mock(ResourceRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        CampusResource exact = new CampusResource("Lab Exact", ResourceEnums.Type.COMPUTER_LAB,
                "Engineering Block", "R1", 30, "desc", false, "projector");
        CampusResource big = new CampusResource("Hall Big", ResourceEnums.Type.COMPUTER_LAB,
                "Main Block", "H1", 120, "desc", false, "projector");
        // Resource ids are null in unit test (no JPA); ranking uses score only.
        when(resources.findByTypeAndStatus(ResourceEnums.Type.COMPUTER_LAB, ResourceEnums.Status.ACTIVE))
                .thenReturn(List.of(big, exact));
        when(bookings.findOverlapping(any(), anyList(), any(), any())).thenReturn(List.of());
        when(bookings.countByResource(anyList())).thenReturn(List.of());

        OptimizationService service = new OptimizationService(resources, bookings);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var req = new AnalyticsDtos.OptimizationRequest("COMPUTER_LAB", 30,
                List.of("projector"), start, start.plus(2, ChronoUnit.HOURS), "Engineering Block");
        var result = service.recommend(req);

        assertEquals(2, result.size());
        assertEquals("Lab Exact", result.get(0).resourceName());
        assertTrue(result.get(0).score() >= result.get(1).score());
        assertTrue(result.get(0).explanation().contains("Estimate only"), result.get(0).explanation());
    }

    @Test
    void filtersOutResourcesMissingEquipment() {
        ResourceRepository resources = mock(ResourceRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        CampusResource noGear = new CampusResource("Plain Room", ResourceEnums.Type.CLASSROOM,
                "Main Block", "R2", 50, "desc", false, "whiteboard");
        when(resources.findByTypeAndStatus(ResourceEnums.Type.CLASSROOM, ResourceEnums.Status.ACTIVE))
                .thenReturn(List.of(noGear));
        when(bookings.findOverlapping(any(), anyList(), any(), any())).thenReturn(List.of());
        when(bookings.countByResource(anyList())).thenReturn(List.of());

        OptimizationService service = new OptimizationService(resources, bookings);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var req = new AnalyticsDtos.OptimizationRequest("CLASSROOM", 10,
                List.of("projector"), start, start.plus(1, ChronoUnit.HOURS), null);
        assertTrue(service.recommend(req).isEmpty());
    }

    @Test
    void unknownResourceTypeIsValidationError() {
        OptimizationService service = new OptimizationService(
                mock(ResourceRepository.class), mock(BookingRepository.class));
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var req = new AnalyticsDtos.OptimizationRequest("HELIPAD", 10,
                List.of(), start, start.plus(1, ChronoUnit.HOURS), null);
        var ex = assertThrows(RuntimeException.class, () -> service.recommend(req));
        assertTrue(ex.getMessage().contains("HELIPAD") || ex.toString().contains("VALIDATION"),
                ex.getMessage());
    }
}
