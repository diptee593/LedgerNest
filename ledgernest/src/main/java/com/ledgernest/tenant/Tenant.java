package com.ledgernest.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // nullable = false mirrors your DB constraint: name VARCHAR(200) NOT NULL
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "vat_number", length = 20)
    private String vatNumber;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "Text")
    private String address;

    // CHAR(2) in DB — stores country code like "IE", "GB", "US"
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode = "IE";

    // @Enumerated(STRING) = stores "FREE" not 0 in the DB
    // columnDefinition matches your CHECK constraint values
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType = PlanType.FREE;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Hibernate sets this automatically when the record is first saved
    // updatable = false means it never changes after creation
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Hibernate updates this automatically every time the record is saved
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enum lives inside Tenant because it only belongs to Tenant
    // Matches your DB CHECK constraint: ('FREE', 'PRO', 'ENTERPRISE')
    public enum PlanType {
        FREE, PRO, ENTERPRISE
    }

}
