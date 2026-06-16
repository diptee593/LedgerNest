package com.ledgernest.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor

public class TenantRegisterRequest {
    // The company name — required, max 200 matches DB column length
    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    // Company email — can be different from the owner's personal email
    // Example: owner email = john@gmail.com, company email = info@abcltd.ie
    @NotBlank(message = "Company email is required")
    @Email(message = "Company email must be valid")
    private String companyEmail;

    // Optional fields — company may not have these at registration time
    private String vatNumber;
    private String phone;
    private String address;

    // Defaults to IE in the Tenant entity but can be overridden here
    // CHAR(2) country codes: IE, GB, US etc
    private String countryCode = "IE";

    // The owner's personal details — nested inside this request
    // So the full registration body looks like:
    // {
    // "companyName": "ABC Ltd",
    // "companyEmail": "info@abc.ie",
    // "owner": {
    // "firstName": "John",
    // "lastName": "Doe",
    // "email": "john@abc.ie",
    // "password": "secret123"
    // }
    // }
    // @Valid here means Spring also validates the nested RegisterRequest object
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotNull(message = "Owner details are required")
    private RegisterRequest owner;

}
