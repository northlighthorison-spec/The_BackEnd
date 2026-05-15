package com.wha.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations", indexes = {
    @Index(name = "idx_donation_user", columnList = "user_id"),
    @Index(name = "idx_donation_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DonationStatus status = DonationStatus.PENDING;

    @Column(length = 255)
    private String stripePaymentIntentId;

    @Column(length = 45)
    private String donorIp;

    // AML compliance fields — admin-only
    @Column(length = 255)
    private String billingName;

    @Column(length = 100)
    private String billingCountry;

    @Column(length = 10)
    private String cardLast4;

    @Column(length = 50)
    private String paymentMethod;

    @Column(nullable = false)
    @Builder.Default
    private boolean flagged = false;

    @Column(length = 300)
    private String flagReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public enum DonationStatus {
        PENDING, COMPLETED, FAILED, REFUNDED, FLAGGED
    }
}
