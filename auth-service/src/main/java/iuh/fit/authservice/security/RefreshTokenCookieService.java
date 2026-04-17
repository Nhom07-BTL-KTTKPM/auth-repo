package iuh.fit.authservice.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenCookieService {

    private final AuthCookieProperties cookieProperties;

    public RefreshTokenCookieService(AuthCookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshTokenId, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getRefreshTokenName(), refreshTokenId)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .sameSite(cookieProperties.getSameSite())
                .maxAge(Duration.ofSeconds(maxAgeSeconds));

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getRefreshTokenName(), "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .sameSite(cookieProperties.getSameSite())
                .maxAge(Duration.ZERO);

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    public Optional<String> extractRefreshTokenId(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieProperties.getRefreshTokenName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
