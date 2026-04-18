package iuh.fit.authservice.auth.service;

import io.github.resilience4j.retry.annotation.Retry;
import feign.FeignException;
import iuh.fit.authservice.auth.dto.AuthTokenPair;
import iuh.fit.authservice.auth.dto.LoginRequest;
import iuh.fit.authservice.auth.dto.RegisterRequest;
import iuh.fit.authservice.auth.dto.RegisterResponse;
import iuh.fit.authservice.client.UserServiceErrorMapper;
import iuh.fit.authservice.client.UserServiceClient;
import iuh.fit.authservice.client.dto.UserAuthProfileResponse;
import iuh.fit.authservice.client.dto.UserRegisterRequest;
import iuh.fit.authservice.security.JwtTokenProvider;
import iuh.fit.authservice.token.model.RefreshToken;
import iuh.fit.authservice.token.service.RefreshTokenService;
import iuh.fit.shared.api.ApiResponse;
import iuh.fit.shared.error.BusinessException;
import iuh.fit.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserServiceClient userServiceClient;
    private final UserServiceErrorMapper userServiceErrorMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserServiceClient userServiceClient,
            UserServiceErrorMapper userServiceErrorMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.userServiceClient = userServiceClient;
        this.userServiceErrorMapper = userServiceErrorMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Retry(name = "userServiceRetry", fallbackMethod = "registerFallback")
    public RegisterResponse register(RegisterRequest request) {
        ApiResponse<UserAuthProfileResponse> response = userServiceClient.register(
            new UserRegisterRequest(
                request.email(),
                request.password(),
                request.fullName(),
                request.phoneNumber()
            )
        );

        UserAuthProfileResponse user = extractResponseData(
                response,
                ErrorCode.BAD_REQUEST,
                "Register failed"
        );
        return new RegisterResponse(user.accountId(), user.email(), user.role());
    }

    @Retry(name = "userServiceRetry", fallbackMethod = "loginFallback")
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

    private RegisterResponse registerFallback(RegisterRequest request, Throwable throwable) {
        BusinessException mapped = userServiceErrorMapper.mapRetryExhausted("register-user", request.email(), throwable);
        if (mapped.getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                && !userServiceErrorMapper.isUserServiceFailure(throwable)) {
            log.error("Register failed due to internal auth-service error for email={}", request.email(), throwable);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Registration is temporarily unavailable. Please try again later."
            );
        }

        if (mapped.getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("User-service call failed during register-user for email={}", request.email(), throwable);
        } else {
            log.debug("Register fallback mapped to business error for email={}, code={}", request.email(), mapped.getErrorCode().code());
        }
        throw mapped;
    }

    private AuthTokenPair loginFallback(LoginRequest request, String deviceInfo, String ipAddress, Throwable throwable) {
        BusinessException mapped = userServiceErrorMapper.mapRetryExhausted("get-user-by-email", request.email(), throwable);
        if (mapped.getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                && !userServiceErrorMapper.isUserServiceFailure(throwable)) {
            log.error("Login failed due to internal auth-service error for email={}", request.email(), throwable);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Authentication is temporarily unavailable. Please try again later."
            );
        }

        if (mapped.getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("User-service call failed during get-user-by-email for email={}", request.email(), throwable);
        } else {
            log.debug("Login fallback mapped to business error for email={}, code={}", request.email(), mapped.getErrorCode().code());
        }
        throw mapped;
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
        ApiResponse<UserAuthProfileResponse> response = executeUserServiceCall(
                "get-user-by-email",
                email,
                () -> userServiceClient.getByEmail(email)
        );

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        return response.data();
    }

    private <T> T executeUserServiceCall(String operation, String email, Supplier<T> action) {
        try {
            return action.get();
        } catch (FeignException.FeignClientException ex) {
            throw userServiceErrorMapper.mapClientException(operation, email, ex);
        }
    }

    private static UserAuthProfileResponse extractResponseData(
            ApiResponse<UserAuthProfileResponse> response,
            ErrorCode defaultCode,
            String fallbackMessage
    ) {
        if (response == null || !response.success() || response.data() == null) {
            throw toBusinessException(response, defaultCode, fallbackMessage);
        }
        return response.data();
    }

    private static BusinessException toBusinessException(ApiResponse<?> response, ErrorCode defaultCode, String defaultMessage) {
        if (response == null) {
            return new BusinessException(defaultCode, defaultMessage);
        }

        if (response.error() == null) {
            String message = (response.message() == null || response.message().isBlank())
                    ? defaultMessage
                    : response.message();
            return new BusinessException(defaultCode, message);
        }

        ErrorCode mappedCode = mapErrorCode(response.error().code(), defaultCode);
        String message = (response.error().detail() == null || response.error().detail().isBlank())
                ? defaultMessage
                : response.error().detail();
        return new BusinessException(mappedCode, message, response.error().metadata());
    }

    private static ErrorCode mapErrorCode(String code, ErrorCode defaultCode) {
        if (code == null || code.isBlank()) {
            return defaultCode;
        }

        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.code().equalsIgnoreCase(code)) {
                return errorCode;
            }
        }

        return defaultCode;
    }

}
