package com.oism.infrastructure.websocket;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.infrastructure.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

/**
 * Chot chan STOMP (FR-SIM-02, NFR-TENANT-01):
 * - CONNECT phai co "Authorization: Bearer <JWT>" hop le, neu khong bi tu choi (client nhan frame ERROR).
 * - SUBSCRIBE chi duoc vao /topic/tenants/{tenantId cua token}/... ; nghe kenh tenant khac bi tu choi.
 * - SEND tu client bi chan: kenh chi dung de server day thong bao.
 */
public class TenantChannelInterceptor implements ChannelInterceptor {

    static final String TENANT_TOPIC_PREFIX = "/topic/tenants/";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public TenantChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor));
            case SUBSCRIBE -> assertOwnTenant(accessor.getUser(), accessor.getDestination());
            case SEND -> throw new MessageDeliveryException("Không được gửi tin lên kênh thông báo");
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK... khong can kiem tra
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            throw new MessageDeliveryException("Thiếu access token khi kết nối WebSocket");
        }
        AuthenticatedUser user = jwtService.parse(header.substring(BEARER.length()).trim())
                .orElseThrow(() -> new MessageDeliveryException("Access token không hợp lệ hoặc đã hết hạn"));
        return new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
    }

    static void assertOwnTenant(Principal principal, String destination) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new MessageDeliveryException("Chưa xác thực WebSocket");
        }
        String allowedPrefix = TENANT_TOPIC_PREFIX + user.tenantId() + "/";
        if (destination == null || !destination.startsWith(allowedPrefix) || destination.contains("..")) {
            throw new MessageDeliveryException("Không được nghe kênh của cửa hàng khác");
        }
    }
}
