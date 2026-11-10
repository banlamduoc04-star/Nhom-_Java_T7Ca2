package com.oism.application.order;

import com.oism.application.common.Sha256;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.ChannelCredential;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.repository.ChannelCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * API key webhook theo tenant x san (FR-SIM-01, Ke hoach muc 4 bang channel_credential).
 * Key tho chi tra ve mot lan luc sinh; DB chi giu ban bam SHA-256 (giong refresh token, NFR-SEC-01).
 */
@Service
public class ChannelCredentialService {

    /** Ke hoach: channel_credential.channel thuoc SHOPEE/TIKTOK. */
    public static final Set<OrderChannel> WEBHOOK_CHANNELS = Set.of(OrderChannel.SHOPEE, OrderChannel.TIKTOK);

    private static final String KEY_PREFIX = "oism_";

    private final ChannelCredentialRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ChannelCredentialService(ChannelCredentialRepository repository) {
        this.repository = repository;
    }

    /** Key tho vua sinh (chi tra ve mot lan). */
    public record IssuedKey(OrderChannel channel, String apiKey, Instant createdAt) {
    }

    /** Trang thai key cua tung san, khong bao gio chua key. */
    public record ChannelKeyView(OrderChannel channel, boolean configured, Instant createdAt) {
    }

    /** Sinh key moi cho san; key cu cua san do (neu co) bi vo hieu ngay. */
    @Transactional
    public IssuedKey issue(UUID tenantId, OrderChannel channel) {
        assertWebhookChannel(channel);
        repository.findByTenantIdAndChannelAndActiveTrue(tenantId, channel).ifPresent(old -> {
            old.deactivate();
            // flush truoc khi them key moi: index duy nhat (tenant, channel) WHERE active
            repository.saveAndFlush(old);
        });
        String rawKey = newRawKey();
        ChannelCredential saved = repository.save(new ChannelCredential(tenantId, channel, Sha256.hex(rawKey)));
        repository.flush();
        return new IssuedKey(channel, rawKey, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ChannelKeyView> list(UUID tenantId) {
        List<ChannelCredential> active = repository.findByTenantIdAndActiveTrue(tenantId);
        return WEBHOOK_CHANNELS.stream()
                .sorted()
                .map(ch -> active.stream().filter(c -> c.getChannel() == ch).findFirst()
                        .map(c -> new ChannelKeyView(ch, true, c.getCreatedAt()))
                        .orElse(new ChannelKeyView(ch, false, null)))
                .toList();
    }

    /** Xac thuc key cua mot san. Rong khi key thieu/sai, sai san, hoac san khong nhan webhook. */
    @Transactional(readOnly = true)
    public Optional<WebhookCaller> authenticate(OrderChannel channel, String rawKey) {
        if (channel == null || !WEBHOOK_CHANNELS.contains(channel) || rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByApiKeyHashAndChannelAndActiveTrue(Sha256.hex(rawKey.trim()), channel)
                .map(c -> new WebhookCaller(c.getTenantId(), c.getChannel()));
    }

    private static void assertWebhookChannel(OrderChannel channel) {
        if (!WEBHOOK_CHANNELS.contains(channel)) {
            throw new BusinessException(ErrorCode.OD40001, "Kênh " + channel + " không nhận webhook (chỉ SHOPEE, TIKTOK)");
        }
    }

    private String newRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
