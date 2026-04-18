package iuh.fit.authservice.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "services.user-service.internal-auth")
public class UserServiceInternalAuthProperties {

    private String headerName = "X-Internal-Api-Key";
    private String apiKey = "change-me-in-prod";
}
