package com.wha.controller;

import com.wha.dto.request.CreateEventRequest;
import com.wha.dto.request.UpdateEventRequest;
import com.wha.dto.response.ApiResponse;
import com.wha.entity.Event;
import com.wha.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Event>>> listEvents(
            @PageableDefault(size = 12, sort = "eventDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved", eventService.getAllEvents(pageable)));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Event>>> upcomingEvents(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Upcoming events", eventService.getUpcomingEvents(limit)));
    }

    @GetMapping("/past")
    public ResponseEntity<ApiResponse<Page<Event>>> pastEvents(
            @PageableDefault(size = 12, sort = "eventDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Past events", eventService.getPastEvents(pageable)));
    }

    @GetMapping("/subscriber-early-access")
    public ResponseEntity<ApiResponse<List<Event>>> earlyAccessEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Early access events",
            eventService.getSubscriberEarlyAccessEvents()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Event>> getEvent(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Event retrieved", eventService.getEvent(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Event>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Event event = eventService.createEvent(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Event created", event));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Event>> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Event updated", eventService.updateEvent(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.ok("Event deleted", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Event>> updateStatus(
            @PathVariable String id,
            @RequestParam Event.EventStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", eventService.updateStatus(id, status)));
    }
}
