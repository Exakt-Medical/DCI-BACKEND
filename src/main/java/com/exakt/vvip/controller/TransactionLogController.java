package com.exakt.vvip.controller;

import com.exakt.vvip.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionLogController {

    private final TransactionLogService transactionLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactionLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.info("Fetching transaction logs - page: {}, size: {}, status: {}, search: {}",
                page, size, status, search);

        try {
            Map<String, Object> response = transactionLogService.getTransactionLogs(
                    status, search, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching transaction logs: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch transaction logs");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}