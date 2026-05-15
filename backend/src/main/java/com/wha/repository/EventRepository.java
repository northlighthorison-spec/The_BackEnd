package com.wha.repository;

import com.wha.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    Page<Event> findByStatusNot(Event.EventStatus status, Pageable pageable);

    List<Event> findByStatusAndEventDateAfterOrderByEventDateAsc(
        Event.EventStatus status, LocalDateTime after);

    @Query("SELECT e FROM Event e WHERE e.status = :status ORDER BY e.eventDate ASC")
    List<Event> findUpcomingAvailable(@Param("status") Event.EventStatus status, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.subscriberEarlyAccess = true " +
           "AND e.status = :status ORDER BY e.eventDate ASC")
    List<Event> findSubscriberEarlyAccessEvents(@Param("status") Event.EventStatus status);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countUpcoming(@Param("status") Event.EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = :completed OR e.status = :cancelled OR e.eventDate < :now ORDER BY e.eventDate DESC")
    Page<Event> findPastEvents(@Param("completed") Event.EventStatus completed,
                               @Param("cancelled") Event.EventStatus cancelled,
                               @Param("now") java.time.LocalDateTime now, Pageable pageable);
}
