package iuh.fit.authservice.account.dto;

import java.util.UUID;

import iuh.fit.authservice.account.enums.AccountStatus;

public class CustomerInfor {
    private UUID accountId;
    private String email;
    private AccountStatus status;
    private String fullName;
    private String phoneNumber;

    public CustomerInfor(UUID accountId, String email, AccountStatus status, String fullName, String phoneNumber) {
        this.accountId = accountId;
        this.email = email;
        this.status = status;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
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
}
