package iuh.fit.authservice.account.dto;

import java.time.LocalDate;
import java.util.UUID;

public class EmployeeResponse {
    private UUID id;
    private String fullName;
    private String phoneNumber;
    private String employeeCode;
    private LocalDate hireDate;

    public UUID getId() {
        return id;
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
