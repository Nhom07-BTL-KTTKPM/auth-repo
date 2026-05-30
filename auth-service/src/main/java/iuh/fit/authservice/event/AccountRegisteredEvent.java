package iuh.fit.authservice.event;

public record AccountRegisteredEvent(
        String accountId,
        String email,
        String fullName,
        String phoneNumber,
        String role
) {
}
