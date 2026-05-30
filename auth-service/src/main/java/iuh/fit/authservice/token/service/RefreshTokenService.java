package iuh.fit.authservice.token.service;

import iuh.fit.authservice.token.model.RefreshToken;
import iuh.fit.authservice.token.repository.RefreshTokenRedisRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public RefreshTokenService(RefreshTokenRedisRepository refreshTokenRedisRepository) {
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
    }

    public RefreshToken create(RefreshToken refreshToken) {
        return refreshTokenRedisRepository.save(refreshToken);
    }

    public Optional<RefreshToken> getByTokenId(String tokenId) {
        return refreshTokenRedisRepository.findById(tokenId);
    }

    public RefreshToken update(RefreshToken refreshToken) {
        return refreshTokenRedisRepository.update(refreshToken);
    }

    public RefreshToken updateMetadata(String tokenId, String deviceInfo, String ipAddress) {
        return refreshTokenRedisRepository.updateMetadata(tokenId, deviceInfo, ipAddress);
    }

    public boolean delete(String tokenId) {
        return refreshTokenRedisRepository.deleteById(tokenId);
    }

    public boolean logout(String tokenId) {
        return refreshTokenRedisRepository.deleteById(tokenId);
    }

    public RefreshToken rotate(String oldTokenId, RefreshToken newRefreshToken) {
        if (!validate(oldTokenId)) {
            throw new IllegalStateException("Cannot rotate an invalid refresh token: " + oldTokenId);
        }
        return refreshTokenRedisRepository.rotate(oldTokenId, newRefreshToken);
    }

    public boolean revoke(String tokenId) {
        Optional<RefreshToken> existing = refreshTokenRedisRepository.findById(tokenId);
        if (existing.isEmpty()) {
            return false;
        }
        RefreshToken refreshToken = existing.get();
        refreshToken.setRevoked(true);
        refreshTokenRedisRepository.update(refreshToken);
        return refreshTokenRedisRepository.deleteById(tokenId);
    }

    public boolean validate(String tokenId) {
        return refreshTokenRedisRepository.findById(tokenId)
                .filter(token -> !token.isRevoked())
                .filter(token -> !token.isExpired())
                .isPresent();
    }

    public Optional<Duration> ttl(String tokenId) {
        return refreshTokenRedisRepository.getTimeToLive(tokenId);
    }

    public String buildKey(String tokenId) {
        return refreshTokenRedisRepository.keyFor(tokenId);
    }
}
