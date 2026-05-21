package com.exakt.vvip.controller;

import com.exakt.vvip.dto.DashboardStats;
import com.exakt.vvip.entity.Transaction;
import com.exakt.vvip.service.DashboardService;
import com.exakt.vvip.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Admin dashboard and management endpoints")
public class AdminController {

    private final DashboardService dashboardService;
    private final TransactionService transactionService;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/transactions/recent")
    @Operation(summary = "Get recent transactions")
    public ResponseEntity<List<Transaction>> getRecentTransactions() {
        return ResponseEntity.ok(transactionService.getRecentTransactions());
    }
}