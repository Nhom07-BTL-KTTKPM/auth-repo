package iuh.fit.authservice.client.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceInternalAuthFeignConfig {

    @Bean
    public RequestInterceptor userServiceInternalAuthRequestInterceptor(UserServiceInternalAuthProperties properties) {
        return template -> template.header(properties.getHeaderName(), properties.getApiKey());
    }
}
