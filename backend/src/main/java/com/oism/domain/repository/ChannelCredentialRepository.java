package com.oism.domain.repository;

import com.oism.domain.model.ChannelCredential;
import com.oism.domain.model.OrderChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelCredentialRepository extends JpaRepository<ChannelCredential, UUID> {

    /**
     * Ngoai le co chu dich cua quy tac "moi truy van co tenantId" (Chuan muc 7.1): webhook chua biet tenant,
     * tenant duoc suy ra tu chinh API key (Ke hoach muc 4 - "Webhook lay tenantId tu API key").
     */
    Optional<ChannelCredential> findByApiKeyHashAndChannelAndActiveTrue(String apiKeyHash, OrderChannel channel);

    Optional<ChannelCredential> findByTenantIdAndChannelAndActiveTrue(UUID tenantId, OrderChannel channel);

    List<ChannelCredential> findByTenantIdAndActiveTrue(UUID tenantId);
}
