package com.skating.platform.backend.dto.student.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStudentRequest {

    @Size(
            min = 2,
            max = 20,
            message = "First name must be between 2 and 20 characters"
    )
    @NotBlank(message = "First name is required")
    private String firstName;

    @Size(
            min = 2,
            max = 20,
            message = "First name must be between 2 and 20 characters"
    )
    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Invalid phone number"
    )
    private String phone;

    @Size(max = 255)
    @Email(message = "Invalid email")
    private String email;
}
