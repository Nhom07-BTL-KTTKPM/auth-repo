package iuh.fit.authservice.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import iuh.fit.authservice.account.dto.CustomerResponse;
import iuh.fit.authservice.account.dto.EmployeeResponse;
import iuh.fit.authservice.account.dto.UpdateEmployeeRequest;
import iuh.fit.shared.api.ApiResponse;
import jakarta.validation.Valid;

@FeignClient(name = "user-service") 
public interface UserClient {

    @PutMapping("/api/v1/user/employees/{id}")
    public ResponseEntity<ApiResponse<Void>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request);

    @GetMapping("/api/v1/user/employees/account/{accountId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByAccountId(
            @PathVariable String accountId);

    @GetMapping("/api/v1/user/customers/account/{accountId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByAccountId(
            @PathVariable String accountId);
}
