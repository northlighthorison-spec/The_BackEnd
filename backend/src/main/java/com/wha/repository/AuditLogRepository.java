package com.wha.repository;

import com.wha.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<AuditLog> findBySuspiciousTrueAndResolvedFalseOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.ipAddress = :ip " +
           "AND a.action = 'LOGIN_FAILED' AND a.timestamp > :since")
    long countFailedLoginsFromIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.suspicious = true AND a.resolved = false")
    long countUnresolvedSuspicious();

    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ip ORDER BY a.timestamp DESC")
    List<AuditLog> findByIpAddress(@Param("ip") String ip, Pageable pageable);
}
