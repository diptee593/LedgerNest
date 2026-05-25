package com.ledgernest.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

// @Repository = marks this as a DB access layer

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findbyEmail(String email);

    boolean existsByEmail(String email);

}
