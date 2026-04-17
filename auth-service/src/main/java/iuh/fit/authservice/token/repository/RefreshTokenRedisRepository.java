package iuh.fit.authservice.token.repository;

import iuh.fit.authservice.token.model.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RefreshTokenRedisRepository {

    private final RedisTemplate<String, RefreshToken> refreshTokenRedisTemplate;

    @Value("${auth.redis.refresh-token-key-prefix:refresh:}")
    private String refreshTokenKeyPrefix;

    public RefreshTokenRedisRepository(RedisTemplate<String, RefreshToken> refreshTokenRedisTemplate) {
        this.refreshTokenRedisTemplate = refreshTokenRedisTemplate;
    }

    public RefreshToken save(RefreshToken refreshToken) {
        validateToken(refreshToken);
        Duration ttl = resolveTtl(refreshToken.getExpiresAt());
        refreshTokenRedisTemplate.opsForValue().set(keyFor(refreshToken.getTokenId()), refreshToken, ttl);
        return refreshToken;
    }

    public Optional<RefreshToken> findById(String tokenId) {
        return Optional.ofNullable(refreshTokenRedisTemplate.opsForValue().get(keyFor(tokenId)));
    }

    public RefreshToken update(RefreshToken refreshToken) {
        validateToken(refreshToken);
        if (!existsById(refreshToken.getTokenId())) {
            throw new IllegalArgumentException("Refresh token does not exist: " + refreshToken.getTokenId());
        }
        Duration ttl = resolveTtl(refreshToken.getExpiresAt());
        refreshTokenRedisTemplate.opsForValue().set(keyFor(refreshToken.getTokenId()), refreshToken, ttl);
        return refreshToken;
    }

    public RefreshToken updateMetadata(String tokenId, String deviceInfo, String ipAddress) {
        RefreshToken existing = findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token does not exist: " + tokenId));

        existing.setDeviceInfo(deviceInfo);
        existing.setIpAddress(ipAddress);
        return update(existing);
    }

    public RefreshToken rotate(String oldTokenId, RefreshToken newRefreshToken) {
        if (!deleteById(oldTokenId)) {
            throw new IllegalArgumentException("Old refresh token does not exist: " + oldTokenId);
        }
        return save(newRefreshToken);
    }

    public boolean deleteById(String tokenId) {
        return Boolean.TRUE.equals(refreshTokenRedisTemplate.delete(keyFor(tokenId)));
    }

    public boolean existsById(String tokenId) {
        return Boolean.TRUE.equals(refreshTokenRedisTemplate.hasKey(keyFor(tokenId)));
    }

    public Optional<Duration> getTimeToLive(String tokenId) {
        Long ttlSeconds = refreshTokenRedisTemplate.getExpire(keyFor(tokenId), TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds < 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(ttlSeconds));
    }

    public String keyFor(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Token id must not be blank");
        }
        return refreshTokenKeyPrefix + tokenId;
    }

    private static Duration resolveTtl(Instant expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Refresh token expiry must be in the future");
        }
        return ttl;
    }

    private static void validateToken(RefreshToken refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("refreshToken must not be null");
        }
        if (refreshToken.getTokenId() == null || refreshToken.getTokenId().isBlank()) {
            throw new IllegalArgumentException("tokenId must not be blank");
        }
    }
}
