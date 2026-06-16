package com.ledgernest.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor

public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    // No @Size here intentionally
    // If someone has an existing account with a short password (legacy data)
    // you don't want login to reject them — only registration enforces the rule
    @NotBlank(message = "Password is required")
    private String password;
}
