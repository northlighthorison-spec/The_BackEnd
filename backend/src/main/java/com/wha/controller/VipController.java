package com.wha.controller;

import com.wha.dto.response.ApiResponse;
import com.wha.entity.VipSelection;
import com.wha.exception.AppException;
import com.wha.repository.VipSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipSelectionRepository vipSelectionRepository;

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<VipSelection>> latest() {
        LocalDateTime now = LocalDateTime.now();
        VipSelection vip = vipSelectionRepository
            .findBySelectionYearAndSelectionMonth(now.getYear(), now.getMonthValue())
            .orElseGet(() -> {
                int prevMonth = now.getMonthValue() == 1 ? 12 : now.getMonthValue() - 1;
                int prevYear = now.getMonthValue() == 1 ? now.getYear() - 1 : now.getYear();
                return vipSelectionRepository
                    .findBySelectionYearAndSelectionMonth(prevYear, prevMonth)
                    .orElseThrow(() -> AppException.notFound("No VIP selected yet"));
            });
        return ResponseEntity.ok(ApiResponse.ok("Latest VIP", vip));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<VipSelection>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<VipSelection> result = vipSelectionRepository
            .findAllByOrderBySelectionYearDescSelectionMonthDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("VIP history", result));
    }
}
