package com.wha.controller;

import com.wha.dto.response.ApiResponse;
import com.wha.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw AppException.badRequest("No file provided");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw AppException.badRequest("Only JPEG, PNG, and WebP images are allowed");
        }
        if (file.getSize() > MAX_BYTES) {
            throw AppException.badRequest("File size must not exceed 5 MB");
        }

        // Strip any path components from the original filename to prevent traversal
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String safeName = Paths.get(original).getFileName().toString();
        String ext = safeName.contains(".")
                ? safeName.substring(safeName.lastIndexOf('.')).toLowerCase()
                : ".jpg";
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) ext = ".jpg";

        String fileName = UUID.randomUUID() + ext;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw AppException.badRequest("Could not save image: " + e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.ok("Image uploaded",
                Map.of("url", baseUrl + "/uploads/" + fileName)));
    }
}
