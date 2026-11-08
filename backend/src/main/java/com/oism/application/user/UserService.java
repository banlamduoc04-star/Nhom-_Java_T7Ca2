package com.oism.application.user;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.AppUser;
import com.oism.domain.model.Role;
import com.oism.domain.repository.AppUserRepository;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** FR-AUTH-05: Owner quan ly tai khoan Staff/Cashier cua tenant (tao, sua, khoa/mo khoa, dat lai mat khau). */
@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final BranchRepository branchRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository appUserRepository,
                       BranchRepository branchRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserView> list(UUID tenantId) {
        return appUserRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(UserView::from).toList();
    }

    @Transactional
    public UserView create(UUID tenantId, String email, String phone, String password, String fullName,
                           Role role, UUID branchId) {
        assertStaffRole(role);
        assertBranchRule(tenantId, role, branchId);
        String cleanEmail = email.trim().toLowerCase();
        String cleanPhone = normalizePhone(phone);
        if (appUserRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessException(ErrorCode.AU40901, "Email đã được đăng ký");
        }
        if (cleanPhone != null && appUserRepository.existsByPhone(cleanPhone)) {
            throw new BusinessException(ErrorCode.AU40901, "Số điện thoại đã được đăng ký");
        }
        AppUser user = new AppUser(tenantId, cleanEmail, cleanPhone, passwordEncoder.encode(password),
                fullName.trim(), role, branchId);
        return UserView.from(appUserRepository.save(user));
    }

    @Transactional
    public UserView update(UUID tenantId, UUID id, String fullName, String phone, Role role, UUID branchId) {
        AppUser user = find(tenantId, id);
        if (user.getRole() == Role.OWNER) {
            throw new BusinessException(ErrorCode.AU40301, "Không sửa được tài khoản Owner qua API này");
        }
        assertStaffRole(role);
        assertBranchRule(tenantId, role, branchId);
        String cleanPhone = normalizePhone(phone);
        if (cleanPhone != null && appUserRepository.existsByPhoneAndIdNot(cleanPhone, id)) {
            throw new BusinessException(ErrorCode.AU40901, "Số điện thoại đã được đăng ký");
        }
        user.setFullName(fullName.trim());
        user.setPhone(cleanPhone);
        user.setRole(role);
        user.setBranchId(branchId);
        return UserView.from(appUserRepository.save(user));
    }

    /** Khoa/mo khoa. Owner khong tu khoa minh duoc. */
    @Transactional
    public UserView setActive(AuthenticatedUser current, UUID id, boolean active) {
        AppUser user = find(current.tenantId(), id);
        if (user.getId().equals(current.userId()) || user.getRole() == Role.OWNER) {
            throw new BusinessException(ErrorCode.AU40301, "Không khóa được tài khoản Owner");
        }
          if (!active) {
            refreshTokenRepository.revokeAllByUserId(user.getId());
        }
        user.setActive(active);
        return UserView.from(appUserRepository.save(user));
    }

    @Transactional
    public void resetPassword(UUID tenantId, UUID id, String newPassword) {
        AppUser user = find(tenantId, id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    private AppUser find(UUID tenantId, UUID id) {
        return appUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy người dùng"));
    }

    private static void assertStaffRole(Role role) {
        if (role != Role.STAFF && role != Role.CASHIER) {
            throw new BusinessException(ErrorCode.AU40001, "Chỉ tạo được tài khoản Staff hoặc Cashier");
        }
    }

    /** Cashier chi ban duoc tai chi nhanh duoc gan (FR-AUTH-05), nen bat buoc co chi nhanh hop le. */
    private void assertBranchRule(UUID tenantId, Role role, UUID branchId) {
        if (branchId == null) {
            if (role == Role.CASHIER) {
                throw new BusinessException(ErrorCode.AU40001, "Cashier phải được gán chi nhánh");
            }
            return;
        }
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy chi nhánh"));
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String cleaned = phone.trim().replaceAll("[\\s.\\-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }
}
