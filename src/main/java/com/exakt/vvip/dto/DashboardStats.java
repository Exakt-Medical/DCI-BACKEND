package com.exakt.vvip.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardStats {
    private long totalCompanies;
    private long activeCompanies;
    private long inactiveCompanies;
    private long transactionsToday;
}