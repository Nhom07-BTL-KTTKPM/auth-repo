package iuh.fit.authservice.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.rate-limit.login-register")
public class AuthLoginRegisterRateLimitProperties {

    private boolean enabled = true;
    private long capacity = 5;
    private long refillTokens = 5;
    private Duration refillDuration = Duration.ofMinutes(1);
    private String keyPrefix = "auth:rate-limit:";
}
