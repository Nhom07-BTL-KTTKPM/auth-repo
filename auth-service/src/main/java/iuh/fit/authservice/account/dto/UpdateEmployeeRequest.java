package iuh.fit.authservice.account.dto;

public class UpdateEmployeeRequest {
    private String email;
    private String fullName;
    private String phoneNumber;

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
