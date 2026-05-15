package com.wha.service;

import com.wha.entity.Subscription;
import com.wha.entity.User;
import com.wha.exception.AppException;
import com.wha.repository.SubscriptionRepository;
import com.wha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.subscription.monthly-price-usd}")
    private BigDecimal monthlyPrice;

    public Optional<Subscription> getUserSubscription(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));
        return subscriptionRepository.findByUserId(user.getId());
    }

    public boolean isSubscribed(String userId) {
        return subscriptionRepository.findByUserId(userId)
            .map(Subscription::isActive)
            .orElse(false);
    }

    @Transactional
    public Subscription subscribe(String userEmail, String ip) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));

        subscriptionRepository.findByUserId(user.getId()).ifPresent(existing -> {
            if (existing.isActive()) {
                throw AppException.conflict("You already have an active subscription");
            }
        });

        Subscription subscription = Subscription.builder()
            .user(user)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .monthlyAmount(monthlyPrice)
            .startedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMonths(1))
            .build();

        subscription = subscriptionRepository.save(subscription);
        auditService.log(user.getId(), user.getEmail(), "SUBSCRIPTION_STARTED", ip, null,
            "Monthly: $" + monthlyPrice);

        return subscription;
    }

    @Transactional
    public void cancel(String userEmail, String ip) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> AppException.notFound("User not found"));

        Subscription sub = subscriptionRepository.findByUserId(user.getId())
            .orElseThrow(() -> AppException.notFound("No active subscription found"));

        sub.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(LocalDateTime.now());
        subscriptionRepository.save(sub);

        auditService.log(user.getId(), user.getEmail(), "SUBSCRIPTION_CANCELLED", ip, null, null);
    }
}
