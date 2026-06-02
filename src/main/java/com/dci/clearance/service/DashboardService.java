package com.dci.clearance.service;

import com.dci.clearance.dto.DashboardStats;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final TransactionRepository transactionRepository;

    public DashboardStats getStats() {
        long total = companyRepository.count();
        long active = 0;
        long inactive = 0;
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long today = transactionRepository.countToday(todayStart);

        return DashboardStats.builder()
                .totalCompanies(total)
                .activeCompanies(active)
                .inactiveCompanies(inactive)
                .transactionsToday(today)
                .build();
    }
}
