package com.wha.dto.response;

import com.wha.entity.Donation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DonationDetailResponse(
    String donationId,
    BigDecimal amount,
    String currency,
    String status,
    boolean flagged,
    String flagReason,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    String message,
    boolean anonymous,
    String paymentReference,
    String paymentMethod,
    String cardLast4,
    String donorIp,
    String billingName,
    String billingCountry,
    DonorInfo donor
) {
    public record DonorInfo(
        String id,
        String fullName,
        String email,
        String phone,
        LocalDateTime accountCreatedAt,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        boolean active
    ) {}

    public static DonationDetailResponse from(Donation d) {
        DonorInfo donor = null;
        if (d.getUser() != null) {
            var u = d.getUser();
            donor = new DonorInfo(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getCreatedAt(),
                u.getLastLoginAt(),
                u.getLastLoginIp(),
                u.isActive()
            );
        }
        return new DonationDetailResponse(
            d.getId(),
            d.getAmount(),
            d.getCurrency(),
            d.getStatus().name(),
            d.isFlagged(),
            d.getFlagReason(),
            d.getCreatedAt(),
            d.getCompletedAt(),
            d.getMessage(),
            d.isAnonymous(),
            d.getStripePaymentIntentId(),
            d.getPaymentMethod(),
            d.getCardLast4(),
            d.getDonorIp(),
            d.getBillingName(),
            d.getBillingCountry(),
            donor
        );
    }
}
