package com.campusos.booking;

import com.campusos.booking.dto.BookingDtos;
import com.campusos.booking.repository.BookingRepository;
import com.campusos.booking.service.BookingService;
import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import com.campusos.resource.repository.ResourceRepository;
import com.campusos.security.AppUserDetails;
import com.campusos.user.Role;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired BookingService bookingService;
    @Autowired UserRepository users;
    @Autowired ResourceRepository resources;
    @Autowired BookingRepository bookings;
    @Autowired PasswordEncoder encoder;

    private User owner;
    private CampusResource lab;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        resources.deleteAll();
        users.deleteAll();
        owner = users.save(new User("owner@test.edu", encoder.encode("Student@123"), "Owner", Role.STUDENT, "CS"));
        lab = resources.save(new CampusResource("Race Lab", ResourceEnums.Type.COMPUTER_LAB,
                "Engineering Block", "R9", 20, "lab", false, "projector"));
    }

    @Test
    void twoConcurrentOverlappingBookingsExactlyOneSucceeds() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        var req = new BookingDtos.CreateBookingRequest(lab.getId(), "Race booking",
                start, start.plus(2, ChronoUnit.HOURS));
        var actor = new AppUserDetails(owner);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            gate.await();
            try {
                bookingService.create(req, actor);
                return "OK";
            } catch (Exception ex) {
                return "FAIL:" + ex.getClass().getSimpleName();
            }
        };
        Future<String> f1 = pool.submit(attempt);
        Future<String> f2 = pool.submit(attempt);
        gate.countDown();
        List<String> results = List.of(f1.get(30, TimeUnit.SECONDS), f2.get(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, results.stream().filter("OK"::equals).count(),
                "exactly one concurrent booking must succeed, got " + results);
        assertEquals(1, bookings.count());
    }
}
