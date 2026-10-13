package com.oism.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

/**
 * Cấu hình JPA Auditing
 * - Tự động ghi thời gian tạo/sửa và tên người thực hiện vào mỗi Entity
 * - Nếu chưa đăng nhập thì ghi "system"
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Lấy thông tin user đang đăng nhập từ Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Nếu chưa đăng nhập hoặc là anonymous thì ghi "system"
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of("system");
            }

            return Optional.of(authentication.getName());
        };
    }
}
