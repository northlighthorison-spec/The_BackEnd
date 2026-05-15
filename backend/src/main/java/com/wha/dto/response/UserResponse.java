package com.wha.dto.response;

import com.wha.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    String fullName,
    String role,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt,
    boolean hasActiveSubscription
) {
    public static UserResponse from(User user, boolean hasSubscription) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.getRole().name(),
            user.isActive(),
            user.getCreatedAt(),
            user.getLastLoginAt(),
            hasSubscription
        );
    }
}
