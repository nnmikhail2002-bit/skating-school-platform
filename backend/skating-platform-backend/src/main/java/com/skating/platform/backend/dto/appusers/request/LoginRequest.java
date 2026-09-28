package com.skating.platform.backend.dto.appusers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    @NotBlank(message = "Password is required")
    private String password;
    @NotBlank(message = "Email is required")
    @Size(max = 255)
    @Email(message = "Invalid email")
    private String email;
}
