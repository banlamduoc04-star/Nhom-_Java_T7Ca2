package com.oism.infrastructure.websocket;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.domain.model.Role;
import com.oism.infrastructure.security.JwtProperties;
import com.oism.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** FR-SIM-02, NFR-TENANT-01: WebSocket bat buoc JWT va chi nghe kenh cua dung tenant. */
class TenantChannelInterceptorTest {

    private final JwtService jwtService = new JwtService(new JwtProperties("unit-test-secret-0123456789-abcdefghij", 60));
    private final TenantChannelInterceptor interceptor = new TenantChannelInterceptor(jwtService);

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void connectWithoutTokenIsRejected() {
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), null));
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer abc.def.ghi", null, null), null));
    }

    @Test
    void connectWithValidTokenSetsUser() {
        String token = jwtService.issue(UUID.randomUUID(), tenantId, Role.OWNER, "o@test.vn");
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + token, null, null), null);

        Principal user = StompHeaderAccessor.wrap(result).getUser();
        AuthenticatedUser principal = (AuthenticatedUser) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
        assertEquals(tenantId, principal.tenantId());
    }

    @Test
    void subscribeOwnTenantTopicsIsAllowed() {
        Principal user = principal(tenantId);
        assertDoesNotThrow(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, null, user, "/topic/tenants/" + tenantId + "/orders"), null));
        assertDoesNotThrow(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, null, user, "/topic/tenants/" + tenantId + "/alerts"), null));
    }

    @Test
    void subscribeOtherTenantOrUnauthenticatedIsRejected() {
        String otherTopic = "/topic/tenants/" + UUID.randomUUID() + "/orders";
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, principal(tenantId), otherTopic), null));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, null, otherTopic), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, null, principal(tenantId), "/topic/tenants/" + tenantId), null));
    }

    @Test
    void clientSendIsRejected() {
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, null, principal(tenantId), "/topic/tenants/" + tenantId + "/orders"), null));
    }

    private Principal principal(UUID tenant) {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), tenant, Role.STAFF, "s@test.vn");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private static Message<byte[]> frame(StompCommand command, String authorization, Principal user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
