package com.ledgernest.auth;

import com.ledgernest.auth.dto.AuthResponse;
import com.ledgernest.auth.dto.LoginRequest;
import com.ledgernest.auth.dto.TenantRegisterRequest;
import com.ledgernest.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService;

    // POST /auth/register
    // Public route — no JWT needed (defined in SecurityConfig)
    // @Valid triggers validation on TenantRegisterRequest
    // including the nested @Valid on RegisterRequest inside it
    // If any field fails → GlobalExceptionHandler returns 400 automatically
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody TenantRegisterRequest request) {

        AuthResponse authResponse = authService.register(request);

        // 201 Created — correct HTTP status for resource creation
        // not 200 OK — that's for reads and updates
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    // POST /auth/login
    // Public route — no JWT needed (you're getting the JWT here)
    // Returns 200 OK — no new resource created, just authenticating
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success("Login successful", authResponse));
    }
}
