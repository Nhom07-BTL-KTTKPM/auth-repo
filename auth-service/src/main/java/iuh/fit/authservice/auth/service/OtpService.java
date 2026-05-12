package iuh.fit.authservice.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.authservice.auth.dto.VerifyEmailTokenPayload;
import iuh.fit.shared.error.BusinessException;
import iuh.fit.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final int otpLength;
    private final Duration otpTtl;
    private final String forgotKeyPrefix;
    private final String changeKeyPrefix;
    private final Duration verifyTokenTtl;
    private final String verifyKeyPrefix;

    public OtpService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${auth.otp.length:6}") int otpLength,
            @Value("${auth.otp.ttl:5m}") Duration otpTtl,
            @Value("${auth.otp.forgot-password-key-prefix:otp:forgot:}") String forgotKeyPrefix,
            @Value("${auth.otp.change-password-key-prefix:otp:change:}") String changeKeyPrefix,
            @Value("${auth.verify-email.token-ttl:24h}") Duration verifyTokenTtl,
            @Value("${auth.verify-email.key-prefix:verify:}") String verifyKeyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.otpLength = otpLength;
        this.otpTtl = otpTtl;
        this.forgotKeyPrefix = forgotKeyPrefix;
        this.changeKeyPrefix = changeKeyPrefix;
        this.verifyTokenTtl = verifyTokenTtl;
        this.verifyKeyPrefix = verifyKeyPrefix;
    }

    // ── OTP ──────────────────────────────────────────────────────────────────

    public String generateAndSaveForgotPasswordOtp(String email) {
        String otp = generateOtp();
        redisTemplate.opsForValue().set(forgotKeyPrefix + email, otp, otpTtl);
        return otp;
    }

    public String generateAndSaveChangePasswordOtp(String accountId) {
        String otp = generateOtp();
        redisTemplate.opsForValue().set(changeKeyPrefix + accountId, otp, otpTtl);
        return otp;
    }

    public void verifyForgotPasswordOtp(String email, String inputOtp) {
        String storedOtp = getStringValue(forgotKeyPrefix + email);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "OTP expired or not found");
        }
        if (!storedOtp.equals(inputOtp.trim())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid OTP");
        }
        // Xóa OTP sau khi verify thành công
        redisTemplate.delete(forgotKeyPrefix + email);
    }

    public void verifyChangePasswordOtp(String accountId, String inputOtp) {
        String storedOtp = getStringValue(changeKeyPrefix + accountId);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "OTP expired or not found");
        }
        if (!storedOtp.equals(inputOtp.trim())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid OTP");
        }
        redisTemplate.delete(changeKeyPrefix + accountId);
    }

    // ── Verify Email Token ────────────────────────────────────────────────────

    public String generateAndSaveVerifyToken(String accountId, String fullName, String phoneNumber) {
        String token = java.util.UUID.randomUUID().toString();
        VerifyEmailTokenPayload payload = new VerifyEmailTokenPayload(accountId, fullName, phoneNumber);
        redisTemplate.opsForValue().set(verifyKeyPrefix + token, payload, verifyTokenTtl);
        return token;
    }

    public VerifyEmailTokenPayload getVerifyEmailPayload(String token) {
        Object raw = redisTemplate.opsForValue().get(verifyKeyPrefix + token);
        if (raw == null) {
            return null;
        }
        if (raw instanceof VerifyEmailTokenPayload payload) {
            return payload;
        }
        if (raw instanceof String accountId) {
            return new VerifyEmailTokenPayload(accountId, null, null);
        }
        return objectMapper.convertValue(raw, VerifyEmailTokenPayload.class);
    }

    public void deleteVerifyToken(String token) {
        redisTemplate.delete(verifyKeyPrefix + token);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = RANDOM.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    private String getStringValue(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }
}
