package com.dci.clearance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDto {
    private Long totalAgents;
    private Long totalTransactions;
    private Long lastWeekAuthenticated;
    private Long todayAuthenticated;
    private Long todayPurchasedVouchers;
    private Long availableVouchers;
    private Long agentsCount;
    private Long subagentsCount;
}