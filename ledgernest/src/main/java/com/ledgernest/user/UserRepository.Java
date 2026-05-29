package com.ledgernest.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    //  SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    //  SELECT COUNT(*) FROM users WHERE email = ? → true/false
    boolean existsByEmail(String email);

    //  SELECT * FROM users WHERE tenant_id = ?
    List<User> findByTenantId(UUID tenantId);

    //  SELECT * FROM users WHERE id = ? AND tenant_id = ?
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    // SELECT COUNT(*) FROM users WHERE email = ? AND tenant_id = ?
    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    // JPQL (Java Persistence Query Language) — looks like SQL but uses class/field names
    // "u.tenant.id" not "tenant_id" because JPQL navigates Java relationships
    // Counts how many users a tenant has — used to enforce plan limits later
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.isActive = true")
    long countActiveUsersByTenantId(@Param("tenantId") UUID tenantId);

    // @Modifying = this query changes data (not a SELECT)
    // @Transactional = this operation needs a DB transaction — required for writes
    // Updates last_login timestamp when user successfully logs in
    // More efficient than loading the full User object just to update one field
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);

}