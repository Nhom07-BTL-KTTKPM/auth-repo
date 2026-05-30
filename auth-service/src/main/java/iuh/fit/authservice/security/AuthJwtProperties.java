package iuh.fit.authservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthJwtProperties {

    private String issuer = "auth-service";
    private String secret = "change-this-secret-in-env";
    private Duration accessTokenExpiration = Duration.ofMinutes(15);
    private Duration refreshTokenExpiration = Duration.ofDays(7);
}
