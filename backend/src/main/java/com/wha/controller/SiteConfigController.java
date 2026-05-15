package com.wha.controller;

import com.wha.dto.response.ApiResponse;
import com.wha.entity.SiteConfig;
import com.wha.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/site-config")
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Site config", siteConfigService.getAllAsMap()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SiteConfig>>> getWithMeta() {
        return ResponseEntity.ok(ApiResponse.ok("Site config with metadata", siteConfigService.getAllWithMeta()));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody Map<String, String> updates) {
        siteConfigService.update(updates);
        return ResponseEntity.ok(ApiResponse.ok("Site config updated"));
    }
}
