package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/**
 * API key cua tenant cho webhook mot san TMDT (FR-SIM-01; Ke hoach muc 4, bang AUTH channel_credential).
 * Chi luu ban bam SHA-256 cua key.
 */
@Entity
@Table(name = "channel_credential")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class ChannelCredential extends TenantScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private OrderChannel channel;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ChannelCredential() {
    }

    public ChannelCredential(UUID tenantId, OrderChannel channel, String apiKeyHash) {
        super(tenantId);
        this.channel = channel;
        this.apiKeyHash = apiKeyHash;
    }

    public OrderChannel getChannel() {
        return channel;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }
}
