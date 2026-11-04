package com.oism.domain.repository;

import com.oism.domain.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    /** Luon truy van kem tenant_id: findById khong bi bo loc tenant chan (NFR-TENANT-01). */
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AppUser> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByPhoneAndIdNot(String phone, UUID id);
}
