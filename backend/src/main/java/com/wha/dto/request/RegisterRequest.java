package com.wha.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record RegisterRequest(
    @JsonProperty("email") @NotBlank @Email @Size(max = 255) String email,
    @JsonProperty("password") @NotBlank @Size(min = 8, max = 128) String password,
    @JsonProperty("firstName") @NotBlank @Size(min = 2, max = 100) String firstName,
    @JsonProperty("lastName") @NotBlank @Size(min = 2, max = 100) String lastName,
    @JsonProperty("phone") @Size(max = 20) String phone
) {}
