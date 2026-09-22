package com.skating.platform.backend.dto.service.response;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
    private Boolean active;
}
