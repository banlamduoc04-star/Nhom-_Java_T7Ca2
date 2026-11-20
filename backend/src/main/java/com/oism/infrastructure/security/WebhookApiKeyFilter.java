package com.oism.infrastructure.security;

import com.oism.application.order.ChannelCredentialService;
import com.oism.application.order.WebhookCaller;
import com.oism.domain.model.OrderChannel;
import com.oism.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Xac thuc webhook san bang header X-Api-Key (FR-SIM-01; Ke hoach muc 4 "Webhook lay tenantId tu API key").
 * Chay sau JwtAuthenticationFilter: tren duong dan webhook bo moi xac thuc JWT, chi chap nhan API key cua dung san.
 * Key thieu/sai -> khong xac thuc -> RestAuthenticationEntryPoint tra 401 OD40101 (truoc ca buoc validate body).
 */
public class WebhookApiKeyFilter extends OncePerRequestFilter {

    public static final String WEBHOOK_PATH_PREFIX = "/api/v1/webhooks/";
    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String ROLE_WEBHOOK = "WEBHOOK";

    private final ChannelCredentialService channelCredentialService;

    public WebhookApiKeyFilter(ChannelCredentialService channelCredentialService) {
        this.channelCredentialService = channelCredentialService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + WEBHOOK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        channelCredentialService.authenticate(channelOf(request), request.getHeader(API_KEY_HEADER))
                .ifPresent(this::authenticate);
        chain.doFilter(request, response);
    }

    private void authenticate(WebhookCaller caller) {
        var authentication = new UsernamePasswordAuthenticationToken(
                caller, null, List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_WEBHOOK)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContext.set(caller.tenantId());
    }

    private static OrderChannel channelOf(HttpServletRequest request) {
        String rest = request.getRequestURI().substring(request.getContextPath().length() + WEBHOOK_PATH_PREFIX.length());
        int slash = rest.indexOf('/');
        String segment = slash >= 0 ? rest.substring(0, slash) : rest;
        try {
            return OrderChannel.valueOf(segment.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
