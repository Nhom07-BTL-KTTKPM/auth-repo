package iuh.fit.authservice.token.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken implements Serializable {

    private String tokenId;
    private String tokenValue;
    private UUID accountId;
    private String email;
    private String role;

    @Builder.Default
    private boolean revoked = false;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant expiresAt;
    private String deviceInfo;
    private String ipAddress;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
