package com.dci.clearance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsDto {
    private Long totalUsers;
    private Long totalTransactions;
    private Long totalDciCertificates;
    private Long totalProcessedVehicles;
    private List<TopEmployeeDto> topDciEmployees;
    private List<TopEmployeeDto> topHpgEmployees;
}
