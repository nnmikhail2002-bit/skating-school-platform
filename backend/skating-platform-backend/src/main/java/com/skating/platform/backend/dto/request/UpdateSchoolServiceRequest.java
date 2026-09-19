package com.skating.platform.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchoolServiceRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
}
