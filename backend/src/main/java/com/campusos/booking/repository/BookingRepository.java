package com.campusos.booking.repository;

import com.campusos.booking.entity.Booking;
import com.campusos.booking.enums.BookingEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Overlap predicate: existing.start < newEnd AND existing.end > newStart (adjacent allowed). */
    @Query("""
        select b from Booking b
        where b.resource.id = :resourceId and b.status in :statuses
          and b.startAt < :endAt and b.endAt > :startAt
        order by b.startAt asc
        """)
    List<Booking> findOverlapping(@Param("resourceId") Long resourceId,
                                  @Param("statuses") List<BookingEnums.Status> statuses,
                                  @Param("startAt") Instant startAt,
                                  @Param("endAt") Instant endAt);

    @Query("""
        select b from Booking b
        where (:ownerId is null or b.owner.id = :ownerId)
          and (:resourceId is null or b.resource.id = :resourceId)
          and (:status is null or b.status = :status)
        """)
    Page<Booking> search(@Param("ownerId") Long ownerId,
                         @Param("resourceId") Long resourceId,
                         @Param("status") BookingEnums.Status status,
                         Pageable pageable);

    @Query("select b from Booking b where b.resource.id = :resourceId and b.status in :statuses and b.endAt > :from and b.startAt < :to order by b.startAt asc")
    List<Booking> findSchedule(@Param("resourceId") Long resourceId,
                               @Param("statuses") List<BookingEnums.Status> statuses,
                               @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(b) from Booking b where b.status in :statuses and b.startAt < :to and b.endAt > :from")
    long countActiveInWindow(@Param("statuses") List<BookingEnums.Status> statuses,
                             @Param("from") Instant from, @Param("to") Instant to);

    @Query("select b.resource.id, count(b) from Booking b where b.status in :statuses group by b.resource.id order by count(b) desc")
    List<Object[]> countByResource(@Param("statuses") List<BookingEnums.Status> statuses);
}
