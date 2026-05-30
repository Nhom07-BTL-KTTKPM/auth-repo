package iuh.fit.authservice.account.dto;

import java.time.LocalDate;
import java.util.UUID;

import iuh.fit.authservice.account.enums.AccountStatus;

public class EmployeeInfo {
    private UUID accountId;
    private String email;
    private AccountStatus status;
    private String fullName;
    private String phoneNumber;
    private String employeeCode;
    private LocalDate hireDate;

    public EmployeeInfo(UUID accountId, String email, AccountStatus status, String fullName, String phoneNumber, String employeeCode, LocalDate hireDate) {
        this.accountId = accountId;
        this.email = email;
        this.status = status;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.employeeCode = employeeCode;
        this.hireDate = hireDate;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }
}
