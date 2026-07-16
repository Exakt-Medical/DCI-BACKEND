package com.dci.clearance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopEmployeeDto {
    private String name;
    private Long count;
    private String username;
}
