package com.ledgernest.user;

import com.ledgernest.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

     @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // UNIQUE across the entire platform — not just within a tenant
    // Matches your DB constraint: email VARCHAR(255) NOT NULL UNIQUE
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // Never store plain text passwords — always BCrypt hashed
    // BCrypt output is always 60 characters but 255 gives safe room
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    // @Enumerated(STRING) = stores "OWNER" not 0 in the DB
    // Matches your DB CHECK constraint: ('OWNER','ADMIN','ACCOUNTANT','VIEWER')
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.OWNER;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Nullable — only populated when user actually logs in
    // Useful for security auditing: "when did this user last access the system?"
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
