package iuh.fit.authservice.event;

/**
 * Dùng chung với notification-service.
 * Gửi qua critical.exchange → routing key email.verify
 */
public record EmailVerifyEvent(
        String email,
        String fullName,
        String verificationToken
) {}
