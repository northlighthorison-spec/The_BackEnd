package com.wha.dto.request;

import jakarta.validation.constraints.*;

public record TicketPurchaseRequest(
    @NotBlank String eventId,
    @Min(1) @Max(4) int quantity
) {}
