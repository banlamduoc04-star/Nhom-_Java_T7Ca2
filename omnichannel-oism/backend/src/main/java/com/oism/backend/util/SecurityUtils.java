package com.oism.backend.util;

import com.oism.backend.entity.Tenant;
import com.oism.backend.entity.User;
import com.oism.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper lay thong tin user va tenant hien tai tu SecurityContext
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    public Long getCurrentTenantId() {
        User user = getCurrentUser();
        if (user != null && user.getTenant() != null) {
            return user.getTenant().getId();
        }
        return 1L;
    }

    public Tenant getCurrentTenant() {
        User user = getCurrentUser();
        if (user != null && user.getTenant() != null) {
            return user.getTenant();
        }
        return Tenant.builder().id(1L).name("Default Store").slug("default-store").build();
    }
}
