package com.oism.application.auth;

import com.oism.application.common.Sha256;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.AppUser;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.RefreshToken;
import com.oism.domain.model.Role;
import com.oism.domain.model.Tenant;
import com.oism.domain.repository.AppUserRepository;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.RefreshTokenRepository;
import com.oism.domain.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case AUTH: dang ky cua hang (FR-AUTH-01), dang nhap / lam moi / dang xuat (FR-AUTH-02),
 * thong tin nguoi dang nhap. Refresh token: 7 ngay, xoay vong moi lan dung, luu dang bam (NFR-SEC-01).
 */
@Service
public class AuthService {

    static final Duration REFRESH_TTL = Duration.ofDays(7);
    static final String DEFAULT_BRANCH_NAME = "Chi nhánh chính";

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(TenantRepository tenantRepository,
                       BranchRepository branchRepository,
                       AppUserRepository appUserRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AccessTokenIssuer accessTokenIssuer) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    /** FR-AUTH-01: tao tenant + Owner dau tien + chi nhanh mac dinh trong mot giao dich. */
    @Transactional
    public RegisterResult register(RegisterCommand command) {
        String email = normalizeEmail(command.email());
        String phone = normalizePhone(command.phone());

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.AU40901, "Email đã được đăng ký");
        }
        if (phone != null && appUserRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.AU40901, "Số điện thoại đã được đăng ký");
        }

        Tenant tenant = tenantRepository.save(new Tenant(command.storeName().trim()));
        Branch branch = branchRepository.save(new Branch(tenant.getId(), DEFAULT_BRANCH_NAME, BranchType.STORE));
        AppUser owner = appUserRepository.save(new AppUser(
                tenant.getId(),
                email,
                phone,
                passwordEncoder.encode(command.password()),
                command.fullName().trim(),
                Role.OWNER,
                branch.getId()));

        return new RegisterResult(tenant.getId(), owner.getId(), branch.getId(), owner.getEmail());
    }

    /** FR-AUTH-02: dang nhap bang email hoac so dien thoai. Sai thong tin -> AU40101, bi khoa -> AU40302. */
    @Transactional
    public TokenPair login(LoginCommand command) {
        String identifier = command.identifier().trim();
        Optional<AppUser> found = identifier.contains("@")
                ? appUserRepository.findByEmailIgnoreCase(identifier)
                : appUserRepository.findByPhone(normalizePhone(identifier));

        AppUser user = found
                .filter(u -> passwordEncoder.matches(command.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40101, "Sai thông tin đăng nhập"));

        assertCanSignIn(user);
        return issueTokens(user);
    }

    /**
     * Cap lai access token, dong thoi xoay vong refresh token (token cu bi thu hoi).
     * Neu token da bi thu hoi ma van duoc gui lai -> coi la bi danh cap, thu hoi moi token cua nguoi dung do.
     * noRollbackFor: viec thu hoi phai duoc ghi lai du da nem loi.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(this::invalidToken);

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw invalidToken();
        }
        if (stored.isExpired(Instant.now())) {
            throw invalidToken();
        }

        AppUser user = appUserRepository.findById(stored.getUserId()).orElseThrow(this::invalidToken);
        assertCanSignIn(user);

        stored.revoke();
        refreshTokenRepository.save(stored);
        return issueTokens(user);
    }
        /** Đăng xuất: thu hồi toàn bộ refresh token của người dùng. */
    @Transactional
    public void logout(AuthenticatedUser current, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(hash(refreshToken.trim()))
                    .ifPresent(token -> {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    });
        }
        // Hoặc thu hồi tất cả các token đang hoạt động của user này
        refreshTokenRepository.revokeAllByUserId(current.userId());
    }
       /** Nguoi dung tu doi mat khau. Sai mat khau cu -> AU40001 (khong dung 401 de client khong tu refresh). */
    @Transactional
    public void changePassword(AuthenticatedUser current, String currentPassword, String newPassword) {
        AppUser user = appUserRepository.findByIdAndTenantId(current.userId(), current.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AU40001, "Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }
    @Transactional(readOnly = true)
    public UserProfile me(AuthenticatedUser current) {
        AppUser user = appUserRepository.findByIdAndTenantId(current.userId(), current.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy người dùng"));
        String tenantName = tenantRepository.findById(user.getTenantId()).map(Tenant::getName).orElse(null);
        return new UserProfile(user.getId(), user.getTenantId(), tenantName, user.getEmail(), user.getPhone(),
                user.getFullName(), user.getRole(), user.getBranchId());
    }

    private void assertCanSignIn(AppUser user) {
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.AU40302);
        }
        boolean tenantActive = tenantRepository.findById(user.getTenantId()).map(Tenant::isActive).orElse(false);
        if (!tenantActive) {
            throw new BusinessException(ErrorCode.AU40302, "Cửa hàng đã bị tạm ngưng");
        }
    }

    private TokenPair issueTokens(AppUser user) {
        String accessToken = accessTokenIssuer.issue(user.getId(), user.getTenantId(), user.getRole(), user.getEmail());

        String rawRefresh = newRefreshTokenValue();
        refreshTokenRepository.save(new RefreshToken(
                user.getTenantId(), user.getId(), hash(rawRefresh), Instant.now().plus(REFRESH_TTL)));

        return new TokenPair(accessToken, accessTokenIssuer.ttlSeconds(), rawRefresh, REFRESH_TTL.toSeconds());
    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorCode.AU40101, "Refresh token không hợp lệ hoặc đã hết hạn");
    }

    private String newRefreshTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        return Sha256.hex(rawToken);
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String cleaned = phone.trim().replaceAll("[\\s.\\-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** Dung cho test: tao UUID khong lien quan toi nghiep vu. */
    static UUID newId() {
        return UUID.randomUUID();
    }
}
