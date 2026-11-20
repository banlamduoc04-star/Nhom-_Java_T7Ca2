package com.oism.infrastructure.websocket;

import com.oism.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket STOMP (FR-SIM-02): endpoint /ws, kenh /topic/tenants/{tenantId}/orders va /alerts.
 * Xac thuc JWT luc CONNECT va chi cho nghe kenh cua dung tenant (TenantChannelInterceptor).
 * Origin lay tu OISM_CORS_ORIGINS nhu REST, khong mo "*".
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final String[] allowedOrigins;

    public WebSocketConfig(JwtService jwtService,
                           @Value("${oism.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.jwtService = jwtService;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new TenantChannelInterceptor(jwtService));
    }
}
