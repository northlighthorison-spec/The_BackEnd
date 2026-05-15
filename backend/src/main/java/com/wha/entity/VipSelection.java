package com.wha.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "vip_selections", indexes = {
    @Index(name = "idx_vip_month", columnList = "selectionYear,selectionMonth", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VipSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User selectedUser;

    @Column(nullable = false)
    private int selectionYear;

    @Column(nullable = false)
    private int selectionMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal donationAmount = new BigDecimal("10000.00");

    @Column(nullable = false, length = 255)
    private String charityName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String charityDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VipStatus status = VipStatus.SELECTED;

    @Column(length = 255)
    private String donationReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_by")
    private User selectedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime donationConfirmedAt;

    public enum VipStatus {
        SELECTED, DONATION_PENDING, DONATION_SENT, COMPLETED
    }
}
