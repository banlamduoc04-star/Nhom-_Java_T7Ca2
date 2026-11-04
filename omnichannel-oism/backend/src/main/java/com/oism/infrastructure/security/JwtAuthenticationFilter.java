package com.oism.infrastructure.security;

import com.oism.application.auth.AuthenticatedUser;
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

/**
 * Doc "Authorization: Bearer ..." -> dat Authentication (principal = AuthenticatedUser, quyen ROLE_<role>)
 * va TenantContext. Token sai/het han: khong xac thuc, de RestAuthenticationEntryPoint tra 401 AU40101.
 * Khong khai bao la @Component de tranh bi dang ky them mot lan o servlet container.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith(BEARER)) {
                jwtService.parse(header.substring(BEARER.length()).trim()).ifPresent(this::authenticate);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(AuthenticatedUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContext.set(user.tenantId());
    }
}
