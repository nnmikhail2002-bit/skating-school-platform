package com.skating.platform.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private Integer experienceYears;
    private Boolean active;
}
