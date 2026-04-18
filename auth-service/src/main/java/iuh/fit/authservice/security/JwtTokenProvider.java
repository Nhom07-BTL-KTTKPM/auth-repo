package iuh.fit.authservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthJwtProperties jwtProperties;

    public JwtTokenProvider(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, AuthJwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UUID accountId, String email, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenExpiration());
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(accountId))
                .claims(existing -> {
                    existing.put("email", email);
                    existing.put("role", role);
                })
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    public String generateRefreshTokenId() {
        return UUID.randomUUID().toString();
    }

    public Jwt parseClaims(String accessToken) {
        return jwtDecoder.decode(accessToken);
    }

    public Map<String, Object> parseClaimsMap(String accessToken) {
        return parseClaims(accessToken).getClaims();
    }

    public boolean validateAccessToken(String accessToken) {
        try {
            jwtDecoder.decode(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Instant calculateRefreshTokenExpiresAt() {
        return Instant.now().plus(jwtProperties.getRefreshTokenExpiration());
    }

    public long accessTokenExpiresInSeconds() {
        return jwtProperties.getAccessTokenExpiration().getSeconds();
    }

    public long refreshTokenExpiresInSeconds() {
        return jwtProperties.getRefreshTokenExpiration().getSeconds();
    }
}
