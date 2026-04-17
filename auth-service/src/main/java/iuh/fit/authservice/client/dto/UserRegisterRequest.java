package iuh.fit.authservice.client.dto;

public record UserRegisterRequest(
        String email,
        String password,
        String fullName,
        String phoneNumber
) {
}
