package iuh.fit.authservice.account.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import iuh.fit.authservice.account.dto.CustomerInfor;
import iuh.fit.authservice.account.dto.CustomerResponse;
import iuh.fit.authservice.account.dto.EmployeeInfo;
import iuh.fit.authservice.account.dto.EmployeeResponse;
import iuh.fit.authservice.account.dto.UpdateEmployeeRequest;
import iuh.fit.authservice.account.entity.Account;
import iuh.fit.authservice.account.enums.AccountRole;
import iuh.fit.authservice.account.enums.AccountStatus;
import iuh.fit.authservice.account.repository.AccountRepository;
import iuh.fit.authservice.client.UserClient;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserClient userClient;

    public AccountService(AccountRepository accountRepository, UserClient userClient) {
        this.accountRepository = accountRepository;
        this.userClient = userClient;
    }

    //Get customer accounts
    public List<CustomerInfor> getCustomerAccounts() {
        List<Account> accounts = accountRepository.findByRole(AccountRole.CUSTOMER);
        return accounts.stream().map(account -> {
            String fullName = "N/A";
            String phoneNumber = "N/A";
            try {
                var response = userClient.getCustomerByAccountId(account.getId().toString());
                if (response != null && response.getBody() != null && response.getBody().data() != null) {
                    CustomerResponse customerResponse = response.getBody().data();
                    fullName = customerResponse.getFullName() != null ? customerResponse.getFullName() : "N/A";
                    phoneNumber = customerResponse.getPhoneNumber() != null ? customerResponse.getPhoneNumber() : "N/A";
                }
            } catch (Exception e) {
                // Log and gracefully return fallback info
            }
            return new CustomerInfor(account.getId(), account.getEmail(), account.getStatus(), fullName, phoneNumber);
        }).toList();
    }

    //Get employee accounts
    public List<EmployeeInfo> getEmployeeAccounts() {
        List<Account> accounts = accountRepository.findByRole(AccountRole.EMPLOYEE);
        return accounts.stream().map(account -> {
            String fullName = "N/A";
            String phoneNumber = "N/A";
            String employeeCode = "N/A";
            java.time.LocalDate hireDate = null;
            try {
                var response = userClient.getEmployeeByAccountId(account.getId().toString());
                if (response != null && response.getBody() != null && response.getBody().data() != null) {
                    EmployeeResponse employeeResponse = response.getBody().data();
                    fullName = employeeResponse.getFullName() != null ? employeeResponse.getFullName() : "N/A";
                    phoneNumber = employeeResponse.getPhoneNumber() != null ? employeeResponse.getPhoneNumber() : "N/A";
                    employeeCode = employeeResponse.getEmployeeCode() != null ? employeeResponse.getEmployeeCode() : "N/A";
                    hireDate = employeeResponse.getHireDate();
                }
            } catch (Exception e) {
                // Log and gracefully return fallback info
            }
            return new EmployeeInfo(account.getId(), account.getEmail(), account.getStatus(), fullName, phoneNumber, employeeCode, hireDate);
        }).toList();
    }

    //Activate or deactivate an account
    public Account changeAccountActiveStatus(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        if (account.getStatus() == AccountStatus.ACTIVE) {
            account.setStatus(AccountStatus.DISABLED);
        } else {
            account.setStatus(AccountStatus.ACTIVE);
        }
        return accountRepository.save(account);
    }

    //Update employee account information
    public Account updateEmployeeAccount(UUID accountId, UpdateEmployeeRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (account.getRole() != AccountRole.EMPLOYEE) {
            throw new RuntimeException("Account with id: " + accountId + " is not an employee account");    
        }

        account.setEmail(request.getEmail());
        accountRepository.save(account);

        UUID employeeId = userClient.getEmployeeByAccountId(accountId.toString()).getBody().data().getId();
        userClient.updateEmployee(employeeId, request);
        return account;
    }
}
