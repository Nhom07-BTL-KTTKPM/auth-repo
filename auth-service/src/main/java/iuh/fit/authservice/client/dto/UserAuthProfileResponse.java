package iuh.fit.authservice.client.dto;

import java.util.UUID;

public record UserAuthProfileResponse(
        UUID accountId,
        String email,
        String role,
        String passwordHash,
        Boolean active
) {
}
