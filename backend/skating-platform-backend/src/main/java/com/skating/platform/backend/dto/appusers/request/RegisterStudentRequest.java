package com.skating.platform.backend.dto.appusers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterStudentRequest {


    @NotBlank(message = "First name is required")
    @Size(
            min = 2,
            max = 20,
            message = "First name must be between 2 and 20 characters"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(
            min = 2,
            max = 50,
            message = "Last name must be between 2 and 50 characters"
    )
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Invalid phone number"
    )
    private String phone;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Size(max = 255)
    @Email(message = "Invalid email")
    private String email;

}

