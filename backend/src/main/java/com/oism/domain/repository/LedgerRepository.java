package com.oism.domain.repository;

import com.oism.domain.model.LedgerEntryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntryEntity, Long> {

    @Query("""
        SELECT l FROM LedgerEntryEntity l
        WHERE l.tenantId = :tenantId AND l.accountId = :accountId
        ORDER BY l.createdAt DESC, l.id DESC
    """)
    List<LedgerEntryEntity> findLedgerByTenantAndAccount(
            @Param("tenantId") String tenantId,
            @Param("accountId") String accountId,
            Pageable pageable);

    Optional<LedgerEntryEntity> findTopByTenantIdAndAccountIdOrderByCreatedAtDescIdDesc(
            String tenantId, String accountId);

    Optional<LedgerEntryEntity> findTopByTenantIdAndAccountIdOrderByIdDesc(
            String tenantId, String accountId);
}
