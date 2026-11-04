package com.oism.application.auth;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.AppUser;
import com.oism.domain.model.RefreshToken;
import com.oism.domain.model.Role;
import com.oism.domain.model.Tenant;
import com.oism.domain.repository.AppUserRepository;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.RefreshTokenRepository;
import com.oism.domain.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock BranchRepository branchRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AccessTokenIssuer tokenIssuer;

    AuthService service;
    UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuthService(tenantRepository, branchRepository, appUserRepository,
                refreshTokenRepository, passwordEncoder, tokenIssuer);
        when(tokenIssuer.issue(any(), any(), any(), any())).thenReturn("access-token");
        when(tokenIssuer.ttlSeconds()).thenReturn(3600L);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant("Shop")));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AppUser user(boolean active) {
        AppUser u = new AppUser(tenantId, "owner@shop.vn", "0901234567", "hash", "Chủ shop", Role.OWNER, null);
        u.setActive(active);
        return u;
    }

    @Test
    void registerRejectsDuplicateEmailWithAU40901() {
        when(appUserRepository.existsByEmailIgnoreCase("owner@shop.vn")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(
                new RegisterCommand("Shop", "Chủ shop", " Owner@Shop.vn ", null, "12345678")));

        assertEquals(ErrorCode.AU40901, ex.getErrorCode());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void registerCreatesTenantBranchAndOwner() {
        when(passwordEncoder.encode("12345678")).thenReturn("hashed");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));
        when(branchRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        RegisterResult result = service.register(
                new RegisterCommand("Shop", "Chủ shop", "Owner@Shop.vn", "0901 234 567", "12345678"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertEquals("owner@shop.vn", result.email());
        assertEquals(Role.OWNER, captor.getValue().getRole());
        assertEquals("hashed", captor.getValue().getPasswordHash());
        assertEquals("0901234567", captor.getValue().getPhone());
    }

    @Test
    void loginWithWrongPasswordFailsWithAU40101() {
        when(appUserRepository.findByEmailIgnoreCase("owner@shop.vn")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("sai", "hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new LoginCommand("owner@shop.vn", "sai")));

        assertEquals(ErrorCode.AU40101, ex.getErrorCode());
    }

    @Test
    void loginUnknownUserFailsWithSameCodeAsWrongPassword() {
        when(appUserRepository.findByEmailIgnoreCase("x@y.vn")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new LoginCommand("x@y.vn", "bat-ky")));

        assertEquals(ErrorCode.AU40101, ex.getErrorCode());
    }

    @Test
    void loginLockedAccountFailsWithAU40302() {
        when(appUserRepository.findByEmailIgnoreCase("owner@shop.vn")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("dung", "hash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new LoginCommand("owner@shop.vn", "dung")));

        assertEquals(ErrorCode.AU40302, ex.getErrorCode());
    }

    @Test
    void loginByPhoneIssuesTokensAndStoresOnlyRefreshHash() {
        when(appUserRepository.findByPhone("0901234567")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("dung", "hash")).thenReturn(true);

        TokenPair pair = service.login(new LoginCommand("0901234567", "dung"));

        assertEquals("access-token", pair.accessToken());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertNotEquals(pair.refreshToken(), captor.getValue().getTokenHash());
        assertEquals(AuthService.hash(pair.refreshToken()), captor.getValue().getTokenHash());
    }

    @Test
    void refreshRotatesTokenAndRevokesOldOne() {
        AppUser u = user(true);
        UUID userId = UUID.randomUUID();
        RefreshToken old = new RefreshToken(tenantId, userId, AuthService.hash("old"), Instant.now().plusSeconds(600));
        when(refreshTokenRepository.findByTokenHash(AuthService.hash("old"))).thenReturn(Optional.of(old));
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(u));

        TokenPair pair = service.refresh("old");

        assertTrue(old.isRevoked());
        assertNotEquals("old", pair.refreshToken());
    }

    @Test
    void reusingRevokedRefreshTokenRevokesAllTokensOfUser() {
        UUID userId = UUID.randomUUID();
        RefreshToken stolen = new RefreshToken(tenantId, userId, AuthService.hash("old"), Instant.now().plusSeconds(600));
        stolen.revoke();
        when(refreshTokenRepository.findByTokenHash(AuthService.hash("old"))).thenReturn(Optional.of(stolen));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refresh("old"));

        assertEquals(ErrorCode.AU40101, ex.getErrorCode());
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void refreshRejectsExpiredAndUnknownTokens() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = new RefreshToken(tenantId, userId, AuthService.hash("exp"), Instant.now().minusSeconds(5));
        when(refreshTokenRepository.findByTokenHash(AuthService.hash("exp"))).thenReturn(Optional.of(expired));
        when(refreshTokenRepository.findByTokenHash(AuthService.hash("none"))).thenReturn(Optional.empty());

        assertEquals(ErrorCode.AU40101, assertThrows(BusinessException.class, () -> service.refresh("exp")).getErrorCode());
        assertEquals(ErrorCode.AU40101, assertThrows(BusinessException.class, () -> service.refresh("none")).getErrorCode());
    }
}
