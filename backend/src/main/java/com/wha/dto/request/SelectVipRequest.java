package com.wha.dto.request;

import jakarta.validation.constraints.*;

public record SelectVipRequest(
    @NotBlank String userId,
    @NotBlank @Size(max = 255) String charityName,
    @NotBlank @Size(max = 1000) String charityDescription,
    @Min(2024) @Max(2100) int year,
    @Min(1) @Max(12) int month
) {}
