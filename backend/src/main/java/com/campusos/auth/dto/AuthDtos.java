package com.campusos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @Schema(example = "student@campus.edu") @NotBlank(message = "Email is required.") @Email(message = "Email must be valid.") String email,
            @NotBlank(message = "Password is required.") String password) {}

    public record MeResponse(Long id, String email, String displayName, String role, String department) {}
}
