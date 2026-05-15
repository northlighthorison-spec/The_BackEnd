package com.wha.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DonationRequest(
    @NotNull @DecimalMin("1.00") @DecimalMax("100000.00") BigDecimal amount,
    @Size(max = 500) String message,
    boolean anonymous
) {}
