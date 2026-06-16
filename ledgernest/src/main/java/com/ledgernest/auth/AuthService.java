package com.ledgernest.auth;

import com.ledgernest.auth.dto.*;
import com.ledgernest.common.ResourceNotFoundException;
import com.ledgernest.security.JwtUtil;
import com.ledgernest.tenant.*;
import com.ledgernest.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor

public class AuthService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(TenantRegisterRequest request) {

        // Step 1 — check company email isn't already registered
        if (tenantRepository.existsByEmail(request.getCompanyEmail())) {
            throw new RuntimeException("Company email already registered: "
                    + request.getCompanyEmail());
        }

        // Step 2 — check owner's personal email isn't already taken
        if (userRepository.existsByEmail(request.getOwner().getEmail())) {
            throw new RuntimeException("User email already registered: "
                    + request.getOwner().getEmail());
        }

        // Step 3 — create and save the Tenant (company) first
        // User needs tenant to exist because of the FK relationship
        Tenant tenant = Tenant.builder()
                .name(request.getCompanyName())
                .email(request.getCompanyEmail())
                .vatNumber(request.getVatNumber())
                .phone(request.getPhone())
                .address(request.getAddress())
                .countryCode(request.getCountryCode() != null
                        ? request.getCountryCode()
                        : "IE")
                .planType(Tenant.PlanType.FREE)
                .isActive(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // Step 4 — create the owner User linked to this tenant
        // passwordEncoder.encode() = BCrypt hashes the plain text password
        // NEVER store request.getOwner().getPassword() directly
        User owner = User.builder()
                .tenant(savedTenant)
                .email(request.getOwner().getEmail())
                .passwordHash(passwordEncoder.encode(request.getOwner().getPassword()))
                .firstName(request.getOwner().getFirstName())
                .lastName(request.getOwner().getLastName())
                .role(Role.OWNER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(owner);

        // Step 5 — generate JWT token immediately
        // User is registered and logged in at the same time
        // tenantId goes into the token so every future request carries it
        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedTenant.getId().toString());

        // Step 6 — build and return the response
        return buildAuthResponse(token, savedUser, savedTenant);
    }

    public AuthResponse login(LoginRequest request) {

        // Step 1 — AuthenticationManager verifies email + password
        // Internally calls UserDetailsServiceImpl.loadUserByUsername()
        // then BCrypt checks the password hash
        // If wrong credentials → throws BadCredentialsException → 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Step 2 — credentials verified, now load the full user from DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        // Step 3 — update last login timestamp
        // More efficient than saving the entire User object for one field change
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        // Step 4 — generate JWT token with email + tenantId
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getTenant().getId().toString());

        // Step 5 — return response with token + user info
        return buildAuthResponse(token, user, user.getTenant());
    }

    // Private helper — both register and login return the same shape
    // Extracted to avoid duplicating this block in both methods
    private AuthResponse buildAuthResponse(String token, User user, Tenant tenant) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .tenantId(tenant.getId())
                .companyName(tenant.getName())
                .build();
    }
}
