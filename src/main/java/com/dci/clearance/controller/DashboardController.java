package com.dci.clearance.controller;

import com.dci.clearance.dto.DashboardResponseDto;
import com.dci.clearance.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final TransactionLogService transactionLogService;

    @GetMapping("/data")
    @PreAuthorize("hasAnyRole('ADMIN', 'HPG', 'LTO')")
    public ResponseEntity<DashboardResponseDto> getDashboardData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {

        // Convert from 1-based (frontend) to 0-based (backend)
        DashboardResponseDto response = transactionLogService.getDashboardData(page - 1, size);
        return ResponseEntity.ok(response);
    }
}