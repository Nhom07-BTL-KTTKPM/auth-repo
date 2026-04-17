package iuh.fit.authservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

    private String refreshTokenName = "refreshTokenId";
    private boolean secure = true;
    private boolean httpOnly = true;
    private String sameSite = "Strict";
    private String path = "/api/v1/auth";
    private String domain;
}
