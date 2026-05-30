package iuh.fit.authservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String otp,
        String oldPassword,
        @NotBlank @Size(min = 8) String newPassword
) {}
