package com.oism.infrastructure.websocket;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.infrastructure.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Cau hinh WebSocket STOMP (JC-28, FR-SIM-02).
 * Xac thuc JWT khi STOMP CONNECT, dinh tuyen tin theo kenh /topic/tenants/{tenantId}/...
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");
                    String authHeader = (authHeaders != null && !authHeaders.isEmpty()) ? authHeaders.get(0) : null;
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        authHeader = accessor.getFirstNativeHeader("token");
                        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
                            authHeader = "Bearer " + authHeader;
                        }
                    }
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new AccessDeniedException("Missing WebSocket bearer token");
                    }
                    try {
                        var user = jwtService.parse(authHeader.substring(7))
                                .orElseThrow(() -> new AccessDeniedException("Invalid WebSocket token"));
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                        accessor.setUser(authentication);
                    } catch (AccessDeniedException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new AccessDeniedException("Invalid WebSocket token", e);
                    }
                }
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (!(accessor.getUser() instanceof Authentication authentication)
                            || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
                        throw new AccessDeniedException("WebSocket subscription requires authentication");
                    }
                    String expectedPrefix = "/topic/tenants/" + user.tenantId() + "/";
                    String destination = accessor.getDestination();
                    if (destination == null || !destination.startsWith(expectedPrefix)) {
                        throw new AccessDeniedException("Cross-tenant WebSocket subscription is forbidden");
                    }
                }
                return message;
            }
        });
    }
}
