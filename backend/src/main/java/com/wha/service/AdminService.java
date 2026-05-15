package com.wha.service;

import com.wha.dto.request.SelectVipRequest;
import com.wha.dto.response.DonationDetailResponse;
import com.wha.entity.*;
import com.wha.exception.AppException;
import com.wha.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VipSelectionRepository vipSelectionRepository;
    private final AuditService auditService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDonations", donationRepository.countCompleted());
        stats.put("totalDonationAmount", donationRepository.sumCompletedDonations());
        stats.put("activeTickets", ticketRepository.countActiveTickets());
        stats.put("activeSubscriptions", subscriptionRepository.countActive(Subscription.SubscriptionStatus.ACTIVE));
        stats.put("unresolvedAlerts", auditLogRepository.countUnresolvedSuspicious());
        return stats;
    }

    public Page<User> getAllUsers(String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return userRepository.searchUsers(query.strip(), pageable);
        }
        return userRepository.findByRoleNot(User.Role.SUPER_ADMIN, pageable);
    }

    @Transactional
    public User toggleUserActive(String userId, boolean active, String adminEmail) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getRole() == User.Role.SUPER_ADMIN) {
            throw AppException.forbidden("Cannot modify a super admin account");
        }

        user.setActive(active);
        userRepository.save(user);
        auditService.log(null, adminEmail, active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
            null, null, "Target user: " + user.getEmail());
        return user;
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public Page<AuditLog> getUserAuditLogs(String userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    public Page<AuditLog> getSuspiciousActivity(Pageable pageable) {
        return auditLogRepository.findBySuspiciousTrueAndResolvedFalseOrderByTimestampDesc(pageable);
    }

    @Transactional
    public AuditLog resolveAlert(String alertId, String note, String resolvedByEmail) {
        AuditLog log = auditLogRepository.findById(alertId)
            .orElseThrow(() -> AppException.notFound("Alert not found"));

        User resolver = userRepository.findByEmail(resolvedByEmail)
            .orElseThrow(() -> AppException.notFound("Admin not found"));

        log.setResolved(true);
        log.setResolvedNote(note);
        log.setResolvedBy(resolver.getId());
        log.setResolvedAt(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    public Page<Donation> getAllDonations(Pageable pageable) {
        return donationRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Donation> getFlaggedDonations(Pageable pageable) {
        return donationRepository.findByFlaggedTrueOrderByCreatedAtDesc(pageable);
    }

    public DonationDetailResponse getDonationDetails(String donationId) {
        Donation donation = donationRepository.findById(donationId)
            .orElseThrow(() -> AppException.notFound("Donation not found"));
        return DonationDetailResponse.from(donation);
    }

    @Transactional
    public VipSelection selectVip(SelectVipRequest request, String adminEmail) {
        if (vipSelectionRepository.existsBySelectionYearAndSelectionMonth(
                request.year(), request.month())) {
            throw AppException.conflict("A VIP has already been selected for this month");
        }

        User selectedUser = userRepository.findById(request.userId())
            .orElseThrow(() -> AppException.notFound("User not found"));

        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> AppException.notFound("Admin not found"));

        VipSelection vip = VipSelection.builder()
            .selectedUser(selectedUser)
            .selectionYear(request.year())
            .selectionMonth(request.month())
            .charityName(request.charityName())
            .charityDescription(request.charityDescription())
            .donationAmount(new BigDecimal("10000.00"))
            .selectedBy(admin)
            .status(VipSelection.VipStatus.SELECTED)
            .build();

        vip = vipSelectionRepository.save(vip);

        auditService.log(admin.getId(), adminEmail, "VIP_SELECTED", null, null,
            "VIP: " + selectedUser.getEmail() + " for " + request.year() + "-" + request.month());

        return vip;
    }

    public Page<VipSelection> getVipHistory(Pageable pageable) {
        return vipSelectionRepository.findAllByOrderBySelectionYearDescSelectionMonthDesc(pageable);
    }

    @Transactional
    public VipSelection updateVipDonationStatus(String vipId, VipSelection.VipStatus status,
                                                 String reference) {
        VipSelection vip = vipSelectionRepository.findById(vipId)
            .orElseThrow(() -> AppException.notFound("VIP selection not found"));
        vip.setStatus(status);
        vip.setDonationReference(reference);
        if (status == VipSelection.VipStatus.DONATION_SENT) {
            vip.setDonationConfirmedAt(LocalDateTime.now());
        }
        return vipSelectionRepository.save(vip);
    }

}
