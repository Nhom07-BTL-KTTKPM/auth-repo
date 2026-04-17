package iuh.fit.authservice.auth.dto;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}
