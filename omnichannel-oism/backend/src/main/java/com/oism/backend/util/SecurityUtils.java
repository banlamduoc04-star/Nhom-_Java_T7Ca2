package com.oism.backend.util;

import com.oism.backend.entity.Tenant;
import com.oism.backend.entity.User;
import com.oism.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Tiện ích hỗ trợ lấy thông tin user và tenant (cửa hàng) hiện tại
 * Dễ hiểu, thân thiện với bài làm sinh viên: nếu chưa có security đăng nhập thì tự động fallback về tenant mặc định (id = 1)
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Lấy thông tin user đang đăng nhập
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    // Lấy tenant ID hiện tại (mặc định 1L nếu đang test hoặc chưa có JWT)
    public Long getCurrentTenantId() {
        User user = getCurrentUser();
        if (user != null && user.getTenant() != null) {
            return user.getTenant().getId();
        }
        // Giá trị mặc định cho bài tập sinh viên khi chạy test/dev
        return 1L;
    }

    // Lấy tenant entity hiện tại
    public Tenant getCurrentTenant() {
        User user = getCurrentUser();
        if (user != null && user.getTenant() != null) {
            return user.getTenant();
        }
        return Tenant.builder().id(1L).name("Cửa hàng mặc định").slug("default-store").build();
    }
}
