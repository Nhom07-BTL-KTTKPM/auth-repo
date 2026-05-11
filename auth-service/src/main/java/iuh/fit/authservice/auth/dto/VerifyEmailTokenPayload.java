package iuh.fit.authservice.auth.dto;

public record VerifyEmailTokenPayload(
        String accountId,
        String fullName,
        String phoneNumber
) {
}
