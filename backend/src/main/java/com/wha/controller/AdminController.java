package com.wha.controller;

import com.wha.dto.request.SelectVipRequest;
import com.wha.dto.response.ApiResponse;
import com.wha.dto.response.DonationDetailResponse;
import com.wha.entity.*;
import com.wha.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats", adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> users(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Users", adminService.getAllUsers(search, pageable)));
    }

    @PatchMapping("/users/{id}/active")
    public ResponseEntity<ApiResponse<User>> toggleActive(
            @PathVariable String id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = adminService.toggleUserActive(id, active, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User status updated", user));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> auditLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Audit logs", adminService.getAuditLogs(pageable)));
    }

    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> userAuditLogs(
            @PathVariable String userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("User audit logs",
            adminService.getUserAuditLogs(userId, pageable)));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> alerts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Suspicious activity alerts",
            adminService.getSuspiciousActivity(pageable)));
    }

    @PatchMapping("/alerts/{id}/resolve")
    public ResponseEntity<ApiResponse<AuditLog>> resolveAlert(
            @PathVariable String id,
            @RequestParam String note,
            @AuthenticationPrincipal UserDetails userDetails) {
        AuditLog log = adminService.resolveAlert(id, note, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Alert resolved", log));
    }

    @GetMapping("/donations")
    public ResponseEntity<ApiResponse<Page<Donation>>> donations(
            @RequestParam(defaultValue = "false") boolean flaggedOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Donation> result = flaggedOnly
            ? adminService.getFlaggedDonations(pageable)
            : adminService.getAllDonations(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Donations", result));
    }

    @GetMapping("/donations/{id}/details")
    public ResponseEntity<ApiResponse<DonationDetailResponse>> donationDetails(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Donation details",
            adminService.getDonationDetails(id)));
    }

    @PostMapping("/vip/select")
    public ResponseEntity<ApiResponse<VipSelection>> selectVip(
            @Valid @RequestBody SelectVipRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        VipSelection vip = adminService.selectVip(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("VIP selected successfully", vip));
    }

    @GetMapping("/vip/history")
    public ResponseEntity<ApiResponse<Page<VipSelection>>> vipHistory(
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("VIP history", adminService.getVipHistory(pageable)));
    }

    @PatchMapping("/vip/{id}/status")
    public ResponseEntity<ApiResponse<VipSelection>> updateVipStatus(
            @PathVariable String id,
            @RequestParam VipSelection.VipStatus status,
            @RequestParam(required = false) String reference) {
        VipSelection vip = adminService.updateVipDonationStatus(id, status, reference);
        return ResponseEntity.ok(ApiResponse.ok("VIP donation status updated", vip));
    }
}
