package iuh.fit.authservice.auth.controller;

import iuh.fit.authservice.account.enums.AccountRole;
import iuh.fit.authservice.auth.dto.AuthTokenPair;
import iuh.fit.authservice.auth.dto.AuthTokenResponse;
import iuh.fit.authservice.auth.dto.ChangePasswordRequest;
import iuh.fit.authservice.auth.dto.ForgotPasswordRequest;
import iuh.fit.authservice.auth.dto.LoginRequest;
import iuh.fit.authservice.auth.dto.GoogleLoginRequest;
import iuh.fit.authservice.auth.dto.RefreshTokenRequest;
import iuh.fit.authservice.auth.dto.RegisterRequest;
import iuh.fit.authservice.auth.dto.RegisterResponse;
import iuh.fit.authservice.auth.dto.ResetPasswordRequest;
import iuh.fit.authservice.auth.service.AuthService;
import iuh.fit.authservice.security.RefreshTokenCookieService;
import iuh.fit.shared.api.ApiResponse;
import iuh.fit.shared.error.BusinessException;
import iuh.fit.shared.error.ErrorCode;
import iuh.fit.shared.trace.TraceIdConstants;
import iuh.fit.shared.trace.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Register successful", resolveTraceId(servletRequest)));
    }

    @PostMapping("internal/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> internalRegister(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        RegisterResponse response = authService.createInternalAccount(request, AccountRole.EMPLOYEE);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Internal account created successfully", resolveTraceId(servletRequest)));
    }
    
    

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthTokenPair tokenPair = authService.login(
                request,
                servletRequest.getHeader("User-Agent"),
                resolveClientIp(servletRequest)
        );

        AuthTokenResponse response = toPublicTokenResponse(tokenPair);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService
                    .buildRefreshTokenCookie(tokenPair.refreshTokenId(), tokenPair.refreshTokenExpiresIn())
                    .toString())
            .body(ApiResponse.success(response, "Login successful", resolveTraceId(servletRequest)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthTokenPair tokenPair = authService.loginWithGoogle(
                request.idToken(),
                servletRequest.getHeader("User-Agent"),
                resolveClientIp(servletRequest)
        );

        AuthTokenResponse response = toPublicTokenResponse(tokenPair);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService
                    .buildRefreshTokenCookie(tokenPair.refreshTokenId(), tokenPair.refreshTokenExpiresIn())
                    .toString())
            .body(ApiResponse.success(response, "Google Login successful", resolveTraceId(servletRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        String refreshTokenId = resolveRefreshTokenId(request, servletRequest);
        AuthTokenPair tokenPair = authService.refresh(
            refreshTokenId,
                servletRequest.getHeader("User-Agent"),
                resolveClientIp(servletRequest)
        );

        AuthTokenResponse response = toPublicTokenResponse(tokenPair);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService
                    .buildRefreshTokenCookie(tokenPair.refreshTokenId(), tokenPair.refreshTokenExpiresIn())
                    .toString())
            .body(ApiResponse.success(response, "Token refreshed", resolveTraceId(servletRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        String refreshTokenId = resolveRefreshTokenId(request, servletRequest);
        authService.logout(refreshTokenId);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearRefreshTokenCookie().toString())
            .body(ApiResponse.success(null, "Logout successful", resolveTraceId(servletRequest)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest servletRequest
    ) {
        if (jwt == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing authentication principal");
        }

        Map<String, Object> data = authService.getProfile(jwt.getSubject());
        data.put("issuedAt", jwt.getIssuedAt());
        data.put("expiresAt", jwt.getExpiresAt());

        return ResponseEntity.ok(ApiResponse.success(data, "Current user profile", resolveTraceId(servletRequest)));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<Map<String, Object>>> claims(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest servletRequest
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing bearer token");
        }

        String token = authorizationHeader.substring(7);
        Map<String, Object> claims = authService.parseAndValidateClaims(token);
        return ResponseEntity.ok(ApiResponse.success(claims, "Token claims", resolveTraceId(servletRequest)));
    }

    // ── Verify Email ─────────────────────────────────────────────────────

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest servletRequest
    ) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully", resolveTraceId(servletRequest)));
    }

    // ── Forgot Password ───────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null,
                "OTP sent to your email if it exists", resolveTraceId(servletRequest)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully", resolveTraceId(servletRequest)));
    }

    // ── Change Password (JWT required) ─────────────────────────────────────

    @PostMapping("/change-password/request")
    public ResponseEntity<ApiResponse<Void>> requestChangePassword(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest servletRequest
    ) {
        if (jwt == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication required");
        authService.requestChangePassword(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(null,
                "OTP sent to your registered email", resolveTraceId(servletRequest)));
    }

    @PostMapping("/change-password/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmChangePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        if (jwt == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication required");
        authService.confirmChangePassword(jwt.getSubject(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully", resolveTraceId(servletRequest)));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveRefreshTokenId(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return refreshTokenCookieService.extractRefreshTokenId(servletRequest)
                .orElseGet(() -> {
                    if (request != null && request.refreshTokenId() != null && !request.refreshTokenId().isBlank()) {
                        return request.refreshTokenId();
                    }
                    throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing refresh token");
                });
    }

    private static AuthTokenResponse toPublicTokenResponse(AuthTokenPair tokenPair) {
        return new AuthTokenResponse(
                "Bearer",
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresIn(),
                tokenPair.refreshTokenExpiresIn()
        );
    }

    private static String resolveTraceId(HttpServletRequest request) {
        if (request != null) {
            Object attr = request.getAttribute(TraceIdConstants.REQUEST_ATTRIBUTE);
            if (attr instanceof String traceId && !traceId.isBlank()) {
                return traceId;
            }

            String headerTraceId = request.getHeader(TraceIdConstants.HEADER_NAME);
            if (headerTraceId != null && !headerTraceId.isBlank()) {
                return headerTraceId;
            }
        }

        String contextTraceId = TraceIdContext.get();
        return (contextTraceId == null || contextTraceId.isBlank()) ? null : contextTraceId;
    }
}
