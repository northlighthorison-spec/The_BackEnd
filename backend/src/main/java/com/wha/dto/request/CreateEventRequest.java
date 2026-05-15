package com.wha.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateEventRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank String description,
    @NotNull LocalDateTime eventDate,
    @NotBlank @Size(max = 300) String location,
    @Size(max = 500) String imageUrl,
    @Min(1) @Max(100000) int totalCapacity,
    @NotNull @DecimalMin("0.00") BigDecimal ticketPrice,
    @NotNull @DecimalMin("0.00") BigDecimal subscriberTicketPrice,
    boolean subscriberEarlyAccess,
    @Min(1) @Max(20) int maxTicketsPerUser
) {}
