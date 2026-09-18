package com.skating.platform.backend.dto.response;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class SchoolServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
    private Boolean active;
}
