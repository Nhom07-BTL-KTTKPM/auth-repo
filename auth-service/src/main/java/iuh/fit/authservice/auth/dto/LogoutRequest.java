package iuh.fit.authservice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String refreshTokenId
) {
}
