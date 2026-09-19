package com.skating.platform.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SchoolServiceRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
}
