package iuh.fit.authservice.auth.dto;

public record AuthTokenPair(
        String accessToken,
        long accessTokenExpiresIn,
        String refreshTokenId,
        long refreshTokenExpiresIn
) {
}
