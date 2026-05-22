package iuh.fit.authservice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAvatarRequest(
        @NotBlank(message = "Avatar URL is required") String avatarUrl
) {
}