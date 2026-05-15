package com.wha.service;

import com.wha.dto.request.CreateEventRequest;
import com.wha.dto.request.UpdateEventRequest;
import com.wha.entity.Event;
import com.wha.entity.User;
import com.wha.exception.AppException;
import com.wha.repository.EventRepository;
import com.wha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final com.wha.repository.TicketRepository ticketRepository;

    public Page<Event> getAllEvents(Pageable pageable) {
        return eventRepository.findByStatusNot(Event.EventStatus.CANCELLED, pageable);
    }

    public Event getEvent(String id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> AppException.notFound("Event not found"));
    }

    public List<Event> getUpcomingEvents(int limit) {
        return eventRepository.findUpcomingAvailable(
            Event.EventStatus.UPCOMING, Pageable.ofSize(limit));
    }

    public Page<Event> getPastEvents(Pageable pageable) {
        return eventRepository.findPastEvents(
            Event.EventStatus.COMPLETED, Event.EventStatus.CANCELLED, LocalDateTime.now(), pageable);
    }

    public List<Event> getSubscriberEarlyAccessEvents() {
        return eventRepository.findSubscriberEarlyAccessEvents(Event.EventStatus.UPCOMING);
    }

    @Transactional
    public Event createEvent(CreateEventRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));

        Event event = Event.builder()
            .title(request.title())
            .description(request.description())
            .eventDate(request.eventDate())
            .location(request.location())
            .imageUrl(request.imageUrl())
            .totalCapacity(request.totalCapacity())
            .ticketsAvailable(request.totalCapacity())
            .ticketPrice(request.ticketPrice())
            .subscriberTicketPrice(request.subscriberTicketPrice())
            .subscriberEarlyAccess(request.subscriberEarlyAccess())
            .maxTicketsPerUser(request.maxTicketsPerUser())
            .createdBy(creator)
            .build();

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(String id, UpdateEventRequest request) {
        Event event = getEvent(id);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setImageUrl(request.imageUrl());
        event.setTotalCapacity(request.totalCapacity());
        event.setTicketPrice(request.ticketPrice());
        event.setSubscriberTicketPrice(request.subscriberTicketPrice());
        event.setSubscriberEarlyAccess(request.subscriberEarlyAccess());
        event.setMaxTicketsPerUser(request.maxTicketsPerUser());
        if (request.status() != null) {
            event.setStatus(Event.EventStatus.valueOf(request.status()));
        }
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(String id) {
        if (!eventRepository.existsById(id)) {
            throw AppException.notFound("Event not found");
        }
        ticketRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
    }

    @Transactional
    public Event updateStatus(String id, Event.EventStatus status) {
        Event event = getEvent(id);
        event.setStatus(status);
        return eventRepository.save(event);
    }

    @Transactional
    public void decreaseAvailability(String eventId, int count) {
        Event event = getEvent(eventId);
        if (event.getTicketsAvailable() < count) {
            throw AppException.badRequest("Not enough tickets available");
        }
        event.setTicketsAvailable(event.getTicketsAvailable() - count);
        eventRepository.save(event);
    }
}
