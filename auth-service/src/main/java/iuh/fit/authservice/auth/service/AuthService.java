package iuh.fit.authservice.auth.service;

import io.github.resilience4j.retry.annotation.Retry;
import iuh.fit.authservice.auth.dto.AuthTokenPair;
import iuh.fit.authservice.auth.dto.LoginRequest;
import iuh.fit.authservice.auth.dto.RegisterRequest;
import iuh.fit.authservice.auth.dto.RegisterResponse;
import iuh.fit.authservice.client.UserServiceClient;
import iuh.fit.authservice.client.dto.UserAuthProfileResponse;
import iuh.fit.authservice.client.dto.UserRegisterRequest;
import iuh.fit.authservice.security.JwtTokenProvider;
import iuh.fit.authservice.token.model.RefreshToken;
import iuh.fit.authservice.token.service.RefreshTokenService;
import iuh.fit.shared.api.ApiResponse;
import iuh.fit.shared.error.BusinessException;
import iuh.fit.shared.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserServiceClient userServiceClient,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.userServiceClient = userServiceClient;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Retry(name = "userServiceRetry")
    public RegisterResponse register(RegisterRequest request) {
        ApiResponse<UserAuthProfileResponse> response = userServiceClient.register(
                new UserRegisterRequest(
                        request.email(),
                        request.password(),
                        request.fullName(),
                        request.phoneNumber()
                )
        );

        UserAuthProfileResponse user = extractResponseData(response, "Register failed");
        return new RegisterResponse(user.accountId(), user.email(), user.role());
    }

    @Retry(name = "userServiceRetry")
    public AuthTokenPair login(LoginRequest request, String deviceInfo, String ipAddress) {
        UserAuthProfileResponse user = getUserByEmail(request.email());

        if (user.passwordHash() == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        if (Boolean.FALSE.equals(user.active())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User account is disabled");
        }

        return issueTokenPair(user, deviceInfo, ipAddress);
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

    private AuthTokenPair issueTokenPair(UserAuthProfileResponse user, String deviceInfo, String ipAddress) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.accountId(), user.email(), user.role());

        String refreshTokenId = jwtTokenProvider.generateRefreshTokenId();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(refreshTokenId)
                .tokenValue(refreshTokenId)
                .accountId(user.accountId())
                .email(user.email())
                .role(user.role())
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

    private UserAuthProfileResponse getUserByEmail(String email) {
        ApiResponse<UserAuthProfileResponse> response = userServiceClient.getByEmail(email);
        return extractResponseData(response, "User lookup failed");
    }

    private static UserAuthProfileResponse extractResponseData(ApiResponse<UserAuthProfileResponse> response, String fallbackMessage) {
        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fallbackMessage);
        }
        return response.data();
    }
}
