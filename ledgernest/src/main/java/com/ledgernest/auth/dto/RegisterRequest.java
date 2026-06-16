package com.ledgernest.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;
// It's just the shape of data coming IN from the HTTP request body

@Getter
@NoArgsConstructor

public class RegisterRequest {
    @NotBlank(message = "First Name is Required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    // @Email checks format — must contain @ and valid domain
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    // Minimum 8 characters — basic security requirement
    // Maximum 100 to prevent abuse
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

}
