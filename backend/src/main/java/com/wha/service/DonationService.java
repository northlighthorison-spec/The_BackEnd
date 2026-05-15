package com.wha.service;

import com.wha.dto.request.DonationRequest;
import com.wha.entity.Donation;
import com.wha.entity.User;
import com.wha.exception.AppException;
import com.wha.repository.DonationRepository;
import com.wha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Donation createDonation(DonationRequest request, String userEmail, String ip) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));

        long donationsFromIp = donationRepository.countDonationsFromIp(
            ip, LocalDateTime.now().minusHours(1));
        if (donationsFromIp >= 10) {
            auditService.flagSuspicious(user.getId(), user.getEmail(),
                "SUSPICIOUS_DONATIONS", ip, "10+ donations from one IP in 1 hour");
        }

        BigDecimal recentTotal = donationRepository.sumRecentDonationsByUser(
            user.getId(), LocalDateTime.now().minusHours(24));
        if (recentTotal != null) {
            BigDecimal projected = recentTotal.add(request.amount());
            if (projected.compareTo(new BigDecimal("50000")) > 0) {
                auditService.flagSuspicious(user.getId(), user.getEmail(),
                    "HIGH_DONATION_VOLUME", ip,
                    "Single user donated $" + projected + " in 24h");
            }
        }

        Donation donation = Donation.builder()
            .user(user)
            .amount(request.amount())
            .message(request.message())
            .anonymous(request.anonymous())
            .donorIp(ip)
            .status(Donation.DonationStatus.PENDING)
            .build();

        donation = donationRepository.save(donation);

        auditService.log(user.getId(), user.getEmail(), "DONATION_INITIATED", ip, null,
            "Amount: $" + request.amount());

        return donation;
    }

    @Transactional
    public Donation createPendingDonation(BigDecimal amount, String message, boolean anonymous,
                                          String userEmail, String ip) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));

        long donationsFromIp = donationRepository.countDonationsFromIp(
            ip, LocalDateTime.now().minusHours(1));
        if (donationsFromIp >= 10) {
            auditService.flagSuspicious(user.getId(), user.getEmail(),
                "SUSPICIOUS_DONATIONS", ip, "10+ donations from one IP in 1 hour");
        }

        BigDecimal recentTotal = donationRepository.sumRecentDonationsByUser(
            user.getId(), LocalDateTime.now().minusHours(24));
        if (recentTotal != null && recentTotal.add(amount).compareTo(new BigDecimal("50000")) > 0) {
            auditService.flagSuspicious(user.getId(), user.getEmail(),
                "HIGH_DONATION_VOLUME", ip,
                "Single user donated $" + recentTotal.add(amount) + " in 24h");
        }

        Donation donation = Donation.builder()
            .user(user)
            .amount(amount)
            .message(message)
            .anonymous(anonymous)
            .donorIp(ip)
            .status(Donation.DonationStatus.PENDING)
            .build();

        donation = donationRepository.save(donation);
        auditService.log(user.getId(), user.getEmail(), "DONATION_INITIATED", ip, null,
            "Amount: $" + amount);
        return donation;
    }

    @Transactional
    public void attachPaymentIntent(String donationId, String intentId) {
        Donation donation = donationRepository.findById(donationId)
            .orElseThrow(() -> AppException.notFound("Donation not found"));
        donation.setStripePaymentIntentId(intentId);
        donationRepository.save(donation);
    }

    @Transactional
    public Donation completeDonation(String donationId, String paymentIntentId,
                                     String billingName, String billingCountry,
                                     String cardLast4, String paymentMethod) {
        Donation donation = donationRepository.findById(donationId)
            .orElseThrow(() -> AppException.notFound("Donation not found"));
        donation.setStatus(Donation.DonationStatus.COMPLETED);
        donation.setStripePaymentIntentId(paymentIntentId);
        donation.setCompletedAt(LocalDateTime.now());
        donation.setBillingName(billingName);
        donation.setBillingCountry(billingCountry);
        donation.setCardLast4(cardLast4);
        donation.setPaymentMethod(paymentMethod);
        donation = donationRepository.save(donation);
        auditService.log(donation.getUser().getId(), donation.getUser().getEmail(),
            "DONATION_COMPLETED", null, null, "Amount: $" + donation.getAmount());
        return donation;
    }

    @Transactional
    public Donation confirmDonation(String donationId, String paymentIntentId) {
        Donation donation = donationRepository.findById(donationId)
            .orElseThrow(() -> AppException.notFound("Donation not found"));
        donation.setStatus(Donation.DonationStatus.COMPLETED);
        donation.setStripePaymentIntentId(paymentIntentId);
        donation.setCompletedAt(LocalDateTime.now());
        return donationRepository.save(donation);
    }

    public List<Donation> getUserDonations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));
        return donationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
