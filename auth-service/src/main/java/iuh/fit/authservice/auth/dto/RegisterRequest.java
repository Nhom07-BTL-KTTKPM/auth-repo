package iuh.fit.authservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(max = 20) String phoneNumber
) {
}
