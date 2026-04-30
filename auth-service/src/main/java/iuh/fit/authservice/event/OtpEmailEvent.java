package iuh.fit.authservice.event;

/**
 * Dùng chung với notification-service.
 * Gửi qua critical.exchange → routing key email.otp
 */
public record OtpEmailEvent(
        String email,
        String fullName,
        String otpCode,
        /** "FORGOT_PASSWORD" hoặc "CHANGE_PASSWORD" */
        String purpose
) {}
