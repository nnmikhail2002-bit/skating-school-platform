package com.skating.platform.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerServiceResponse {
    private Long trainerId;
    private String firstName;
    private String lastName;
    private Set<SchoolServiceResponse> services;
}
