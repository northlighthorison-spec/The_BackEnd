package com.wha.controller;

import com.wha.dto.request.DonationRequest;
import com.wha.dto.response.ApiResponse;
import com.wha.entity.Donation;
import com.wha.security.RateLimitingFilter;
import com.wha.service.DonationService;
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
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Donation>> donate(
            @Valid @RequestBody DonationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        Donation donation = donationService.createDonation(request, userDetails.getUsername(), ip);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Donation recorded. Thank you.", donation));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Donation>>> myDonations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Your donation history",
            donationService.getUserDonations(userDetails.getUsername())));
    }
}
