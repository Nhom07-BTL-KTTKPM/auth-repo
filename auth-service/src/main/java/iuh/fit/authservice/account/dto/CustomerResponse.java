package iuh.fit.authservice.account.dto;

import java.util.UUID;

public class CustomerResponse {
    private UUID id;
    private String fullName;
    private String phoneNumber;

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
