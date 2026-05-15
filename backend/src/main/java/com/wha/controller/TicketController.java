package com.wha.controller;

import com.wha.dto.request.TicketPurchaseRequest;
import com.wha.dto.response.ApiResponse;
import com.wha.entity.Ticket;
import com.wha.security.RateLimitingFilter;
import com.wha.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<List<Ticket>>> purchase(
            @Valid @RequestBody TicketPurchaseRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        List<Ticket> tickets = ticketService.purchaseTickets(request, userDetails.getUsername(), ip);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tickets purchased successfully", tickets));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Ticket>>> myTickets(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Your tickets",
            ticketService.getUserTickets(userDetails.getUsername())));
    }
}
