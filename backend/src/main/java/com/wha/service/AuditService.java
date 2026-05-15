package com.wha.service;

import com.wha.entity.AuditLog;
import com.wha.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String userId, String userEmail, String action,
                    String ipAddress, String userAgent, String details) {
        AuditLog entry = AuditLog.builder()
            .userId(userId)
            .userEmail(userEmail)
            .action(action)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .details(details)
            .suspicious(false)
            .build();
        auditLogRepository.save(entry);
    }

    @Async
    public void flagSuspicious(String userId, String userEmail, String action,
                                String ipAddress, String reason) {
        log.warn("SUSPICIOUS ACTIVITY — user={} ip={} reason={}", userEmail, ipAddress, reason);

        AuditLog entry = AuditLog.builder()
            .userId(userId)
            .userEmail(userEmail)
            .action(action)
            .ipAddress(ipAddress)
            .details(reason)
            .suspicious(true)
            .suspiciousReason(reason)
            .resolved(false)
            .build();
        auditLogRepository.save(entry);
    }

    public void checkBruteForce(String ipAddress, String userEmail) {
        long failedCount = auditLogRepository.countFailedLoginsFromIp(
            ipAddress, LocalDateTime.now().minusMinutes(15));

        if (failedCount >= 5) {
            flagSuspicious(null, userEmail, "BRUTE_FORCE_DETECTED", ipAddress,
                "5+ failed logins from IP in 15 minutes");
        }
    }
}
