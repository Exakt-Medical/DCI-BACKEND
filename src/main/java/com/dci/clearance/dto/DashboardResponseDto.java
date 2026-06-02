// dto/DashboardResponseDto.java
package com.dci.clearance.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponseDto {
    private DashboardStatsDto stats;
    private List<RecentTransactionDto> recentTransactions;
    private Integer totalPages;
    private Integer currentPage;
    private Long totalItems;
}