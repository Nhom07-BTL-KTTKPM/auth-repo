package iuh.fit.authservice.client;

import iuh.fit.authservice.client.config.UserServiceInternalAuthFeignConfig;
import iuh.fit.authservice.client.dto.UserAuthProfileResponse;
import iuh.fit.authservice.client.dto.UserRegisterRequest;
import iuh.fit.shared.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = UserServiceInternalAuthFeignConfig.class
)
public interface UserServiceClient {

    @PostMapping("/api/v1/user/internal/register")
    ApiResponse<UserAuthProfileResponse> register(@RequestBody UserRegisterRequest request);

    @GetMapping("/api/v1/user/internal/by-email")
    ApiResponse<UserAuthProfileResponse> getByEmail(@RequestParam("email") String email);
}
