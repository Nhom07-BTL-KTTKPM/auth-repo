package iuh.fit.authservice.auth.service;

import iuh.fit.authservice.account.entity.Account;
import iuh.fit.authservice.account.repository.AccountRepository;
import iuh.fit.authservice.account.enums.AccountRole;
import iuh.fit.authservice.account.enums.AccountStatus;
import iuh.fit.authservice.account.enums.AuthProvider;
import iuh.fit.authservice.auth.dto.AuthTokenPair;
import iuh.fit.authservice.auth.dto.LoginRequest;
import iuh.fit.authservice.auth.dto.RegisterRequest;
import iuh.fit.authservice.auth.dto.RegisterResponse;
import iuh.fit.authservice.security.JwtTokenProvider;
import iuh.fit.authservice.token.model.RefreshToken;
import iuh.fit.authservice.token.service.RefreshTokenService;
import iuh.fit.shared.error.BusinessException;
import iuh.fit.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Email already exists",
                    Map.of("email", email)
            );
        }

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(AccountRole.CUSTOMER);
        account.setStatus(AccountStatus.ACTIVE);
        account.setProvider(AuthProvider.LOCAL);
        account.setEmailVerified(true);
        account.setFullName(request.fullName().trim());
        account.setPhoneNumber(request.phoneNumber().trim());

        try {
            Account saved = accountRepository.save(account);
            return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getRole().name());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Email already exists",
                    Map.of("email", email)
            );
        }
    }

    @Transactional(readOnly = true)
    public AuthTokenPair login(LoginRequest request, String deviceInfo, String ipAddress) {
        String email = normalizeEmail(request.email());
        Account account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));

        if (account.getPasswordHash() == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User account is disabled or pending verification");
        }
        
        account.setLastLoginAt(java.time.Instant.now());
        accountRepository.save(account);

        return issueTokenPair(account, deviceInfo, ipAddress);
    }

    public AuthTokenPair refresh(String refreshTokenId, String deviceInfo, String ipAddress) {
        RefreshToken current = refreshTokenService.getByTokenId(refreshTokenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (current.isRevoked() || current.isExpired()) {
            refreshTokenService.logout(current.getTokenId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is expired or revoked");
        }

        String newRefreshTokenId = jwtTokenProvider.generateRefreshTokenId();
        RefreshToken rotated = RefreshToken.builder()
                .tokenId(newRefreshTokenId)
                .tokenValue(newRefreshTokenId)
                .accountId(current.getAccountId())
                .email(current.getEmail())
                .role(current.getRole())
                .createdAt(current.getCreatedAt())
                .expiresAt(jwtTokenProvider.calculateRefreshTokenExpiresAt())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .revoked(false)
                .build();

        refreshTokenService.rotate(current.getTokenId(), rotated);

        String accessToken = jwtTokenProvider.generateAccessToken(
                rotated.getAccountId(),
                rotated.getEmail(),
                rotated.getRole()
        );

        return new AuthTokenPair(
                accessToken,
                jwtTokenProvider.accessTokenExpiresInSeconds(),
                rotated.getTokenId(),
                jwtTokenProvider.refreshTokenExpiresInSeconds()
        );
    }

    public void logout(String refreshTokenId) {
        boolean removed = refreshTokenService.revoke(refreshTokenId);
        if (!removed) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Refresh token not found");
        }
    }

    public Map<String, Object> parseAndValidateClaims(String accessToken) {
        if (!jwtTokenProvider.validateAccessToken(accessToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid access token");
        }
        return jwtTokenProvider.parseClaimsMap(accessToken);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(String accountIdStr) {
        java.util.UUID accountId = java.util.UUID.fromString(accountIdStr);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("accountId", account.getId().toString());
        data.put("email", account.getEmail());
        data.put("role", account.getRole().name());
        data.put("fullName", account.getFullName());
        data.put("phoneNumber", account.getPhoneNumber());
        data.put("status", account.getStatus().name());
        data.put("provider", account.getProvider().name());
        data.put("avatarUrl", account.getAvatarUrl());
        data.put("emailVerified", account.getEmailVerified());
        data.put("lastLoginAt", account.getLastLoginAt());
        data.put("createdAt", account.getCreatedAt());
        
        return data;
    }

    private AuthTokenPair issueTokenPair(Account account, String deviceInfo, String ipAddress) {
        String accessToken = jwtTokenProvider.generateAccessToken(account.getId(), account.getEmail(), account.getRole().name());

        String refreshTokenId = jwtTokenProvider.generateRefreshTokenId();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(refreshTokenId)
                .tokenValue(refreshTokenId)
                .accountId(account.getId())
                .email(account.getEmail())
                .role(account.getRole().name())
                .expiresAt(jwtTokenProvider.calculateRefreshTokenExpiresAt())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .revoked(false)
                .build();

        refreshTokenService.create(refreshToken);

        return new AuthTokenPair(
                accessToken,
                jwtTokenProvider.accessTokenExpiresInSeconds(),
                refreshTokenId,
                jwtTokenProvider.refreshTokenExpiresInSeconds()
        );
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email must not be null");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
