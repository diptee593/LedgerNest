package com.ledgernest.auth.dto;

import lombok.*;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;

    private UUID tenantId;
    private String companyName;

}
