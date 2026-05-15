package com.wha.controller;

import com.wha.dto.response.ApiResponse;
import com.wha.entity.Subscription;
import com.wha.security.RateLimitingFilter;
import com.wha.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Optional<Subscription>>> mySubscription(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Subscription status",
            subscriptionService.getUserSubscription(userDetails.getUsername())));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Subscription>> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        Subscription sub = subscriptionService.subscribe(userDetails.getUsername(), ip);
        return ResponseEntity.ok(ApiResponse.ok("Subscription activated!", sub));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        subscriptionService.cancel(userDetails.getUsername(), ip);
        return ResponseEntity.ok(ApiResponse.ok("Subscription cancelled"));
    }
}
