package iuh.fit.authservice.account.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.authservice.account.dto.CustomerInfor;
import iuh.fit.authservice.account.dto.EmployeeInfo;
import iuh.fit.authservice.account.dto.UpdateEmployeeRequest;
import iuh.fit.authservice.account.entity.Account;
import iuh.fit.authservice.account.service.AccountService;
import iuh.fit.shared.api.ApiResponse;
import iuh.fit.shared.trace.TraceIdContext;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/v1/auth/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<CustomerInfor>>> getCustomerAccounts() {
        List<CustomerInfor> customerAccounts = accountService.getCustomerAccounts();
        return ResponseEntity.ok(ApiResponse.success(customerAccounts, "Lấy danh sách tài khoản khách hàng thành công", resolveTraceId()));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<EmployeeInfo>>> getEmployeeAccounts() {
        List<EmployeeInfo> employeeAccounts = accountService.getEmployeeAccounts();
        return ResponseEntity.ok(ApiResponse.success(employeeAccounts, "Lấy danh sách tài khoản nhân viên thành công", resolveTraceId()));
    }

    @PutMapping("/active/{id}")
    public ResponseEntity<ApiResponse<Account>> changeAccountActiveStatus(@PathVariable UUID id) {
        Account updatedAccount = accountService.changeAccountActiveStatus(id);
        return ResponseEntity.ok(ApiResponse.success(updatedAccount, "Cập nhật trạng thái tài khoản thành công", resolveTraceId()));
    }

    @PutMapping("/employee/{accountId}")
    public ResponseEntity<ApiResponse<Account>> updateEmployeeAccount(
            @PathVariable UUID accountId,
            @RequestBody UpdateEmployeeRequest request) {
        Account updatedAccount = accountService.updateEmployeeAccount(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedAccount, "Cập nhật thông tin tài khoản nhân viên thành công", resolveTraceId()));
    }

    private String resolveTraceId() {
        return TraceIdContext.get();
    }
}
