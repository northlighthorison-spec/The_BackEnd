package com.wha.controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.wha.dto.request.TicketPurchaseRequest;
import com.wha.dto.response.ApiResponse;
import com.wha.entity.Donation;
import com.wha.entity.Event;
import com.wha.entity.Ticket;
import com.wha.exception.AppException;
import com.wha.security.RateLimitingFilter;
import com.wha.service.DonationService;
import com.wha.service.EventService;
import com.wha.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final DonationService donationService;
    private final TicketService ticketService;
    private final EventService eventService;

    @Value("${app.stripe.secret-key}")
    private String stripeSecretKey;

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<Map<String, String>>> createIntent(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) throws StripeException {

        Stripe.apiKey = stripeSecretKey;

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String message = body.containsKey("message") ? (String) body.get("message") : null;
        boolean anonymous = body.containsKey("anonymous") && Boolean.TRUE.equals(body.get("anonymous"));
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);

        Donation donation = donationService.createPendingDonation(amount, message, anonymous,
                userDetails.getUsername(), ip);

        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
        PaymentIntent intent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency("usd")
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build())
                        .putMetadata("donationId", donation.getId())
                        .putMetadata("userEmail", userDetails.getUsername())
                        .build());

        donationService.attachPaymentIntent(donation.getId(), intent.getId());

        return ResponseEntity.ok(ApiResponse.ok("Payment intent created", Map.of(
                "clientSecret", intent.getClientSecret(),
                "donationId", donation.getId())));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Donation>> confirm(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) throws StripeException {

        Stripe.apiKey = stripeSecretKey;

        String donationId = body.get("donationId");
        String paymentIntentId = body.get("paymentIntentId");

        PaymentIntent intent = PaymentIntent.retrieve(
                paymentIntentId,
                PaymentIntentRetrieveParams.builder().addExpand("latest_charge").build(),
                null);

        if (!"succeeded".equals(intent.getStatus())) {
            throw AppException.badRequest("Payment has not succeeded");
        }

        String billingName = null;
        String billingCountry = null;
        String cardLast4 = null;
        String paymentMethod = null;

        if (intent.getLatestChargeObject() != null &&
                intent.getLatestChargeObject().getPaymentMethodDetails() != null) {
            var details = intent.getLatestChargeObject().getPaymentMethodDetails();
            paymentMethod = details.getType();
            if (details.getCard() != null) {
                cardLast4 = details.getCard().getLast4();
            }
            if (intent.getLatestChargeObject().getBillingDetails() != null) {
                var billing = intent.getLatestChargeObject().getBillingDetails();
                billingName = billing.getName();
                if (billing.getAddress() != null) {
                    billingCountry = billing.getAddress().getCountry();
                }
            }
        }

        Donation donation = donationService.completeDonation(
                donationId, paymentIntentId, billingName, billingCountry, cardLast4, paymentMethod);

        return ResponseEntity.ok(ApiResponse.ok("Donation confirmed. Thank you.", donation));
    }

    @PostMapping("/create-ticket-intent")
    public ResponseEntity<ApiResponse<Map<String, String>>> createTicketIntent(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) throws StripeException {

        Stripe.apiKey = stripeSecretKey;

        String eventId = (String) body.get("eventId");
        int quantity = ((Number) body.get("quantity")).intValue();

        Event event = eventService.getEvent(eventId);

        BigDecimal pricePerTicket = event.getTicketPrice();
        BigDecimal total = pricePerTicket.multiply(BigDecimal.valueOf(quantity));

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw AppException.badRequest("This event has free tickets — use the direct purchase endpoint.");
        }

        long amountCents = total.multiply(BigDecimal.valueOf(100)).longValue();
        PaymentIntent intent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency("usd")
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build())
                        .putMetadata("type", "ticket")
                        .putMetadata("eventId", eventId)
                        .putMetadata("quantity", String.valueOf(quantity))
                        .putMetadata("userEmail", userDetails.getUsername())
                        .build());

        return ResponseEntity.ok(ApiResponse.ok("Ticket payment intent created",
                Map.of("clientSecret", intent.getClientSecret())));
    }

    @PostMapping("/confirm-ticket")
    public ResponseEntity<ApiResponse<List<Ticket>>> confirmTicket(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) throws StripeException {

        Stripe.apiKey = stripeSecretKey;

        String paymentIntentId = (String) body.get("paymentIntentId");
        String eventId = (String) body.get("eventId");
        int quantity = ((Number) body.get("quantity")).intValue();
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);

        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        if (!"succeeded".equals(intent.getStatus())) {
            throw AppException.badRequest("Payment has not succeeded");
        }

        List<Ticket> tickets = ticketService.purchaseTickets(
                new TicketPurchaseRequest(eventId, quantity), userDetails.getUsername(), ip);

        return ResponseEntity.ok(ApiResponse.ok("Tickets confirmed", tickets));
    }
}
