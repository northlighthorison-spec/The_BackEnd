package com.wha.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @JsonProperty("email") @NotBlank @Email @Size(max = 255) String email,
    @JsonProperty("password") @NotBlank @Size(min = 8, max = 128) String password
) {}
