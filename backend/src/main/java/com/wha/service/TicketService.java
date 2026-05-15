package com.wha.service;

import com.wha.dto.request.TicketPurchaseRequest;
import com.wha.entity.Event;
import com.wha.entity.Subscription;
import com.wha.entity.Ticket;
import com.wha.entity.User;
import com.wha.exception.AppException;
import com.wha.repository.SubscriptionRepository;
import com.wha.repository.TicketRepository;
import com.wha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventService eventService;
    private final AuditService auditService;

    @Transactional
    public List<Ticket> purchaseTickets(TicketPurchaseRequest request,
                                         String userEmail, String ip) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));

        Event event = eventService.getEvent(request.eventId());

        if (event.getStatus() != Event.EventStatus.UPCOMING) {
            throw AppException.badRequest("Tickets are not available for this event");
        }

        boolean isSubscriber = subscriptionRepository.findByUserId(user.getId())
            .map(Subscription::isActive).orElse(false);

        if (event.isSubscriberEarlyAccess() && !isSubscriber) {
            if (event.getEventDate().minusDays(3).isAfter(LocalDateTime.now())) {
                throw AppException.forbidden(
                    "Early access tickets are only available to subscribers until 3 days before the event");
            }
        }

        long alreadyOwned = ticketRepository.countByUserIdAndEventId(user.getId(), event.getId());
        if (alreadyOwned + request.quantity() > event.getMaxTicketsPerUser()) {
            throw AppException.badRequest(
                "You can only buy " + event.getMaxTicketsPerUser() + " tickets per event");
        }

        long bulkCheck = ticketRepository.countPurchasesFromIp(ip, LocalDateTime.now().minusHours(1));
        if (bulkCheck >= 20) {
            auditService.flagSuspicious(user.getId(), user.getEmail(),
                "BULK_TICKET_PURCHASE", ip, "20+ ticket purchases from one IP in 1 hour");
        }

        BigDecimal price = isSubscriber && event.getSubscriberTicketPrice().compareTo(BigDecimal.ZERO) > 0
            ? event.getSubscriberTicketPrice()
            : event.getTicketPrice();

        eventService.decreaseAvailability(event.getId(), request.quantity());

        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < request.quantity(); i++) {
            Ticket ticket = Ticket.builder()
                .ticketCode(UUID.randomUUID().toString().toUpperCase())
                .user(user)
                .event(event)
                .pricePaid(price)
                .subscriberDiscount(isSubscriber)
                .purchaseIp(ip)
                .build();
            tickets.add(ticketRepository.save(ticket));
        }

        auditService.log(user.getId(), user.getEmail(), "TICKET_PURCHASE", ip, null,
            "Event: " + event.getTitle() + ", Qty: " + request.quantity());

        return tickets;
    }

    public List<Ticket> getUserTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));
        return ticketRepository.findByUserIdOrderByPurchasedAtDesc(user.getId());
    }
}
