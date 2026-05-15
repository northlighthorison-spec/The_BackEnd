package com.wha.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_ip", columnList = "ipAddress"),
    @Index(name = "idx_audit_suspicious", columnList = "suspicious"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    private String id;

    @Column(length = 36)
    private String userId;

    @Column(length = 255)
    private String userEmail;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private boolean suspicious = false;

    @Column(length = 300)
    private String suspiciousReason;

    @Column(nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @Column(length = 300)
    private String resolvedNote;

    @Column(length = 36)
    private String resolvedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private LocalDateTime resolvedAt;
}
