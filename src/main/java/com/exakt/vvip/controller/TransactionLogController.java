package com.exakt.vvip.controller;

import com.exakt.vvip.entity.TransactionLog;
import com.exakt.vvip.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TransactionLogController {

    private final TransactionLogService transactionLogService;

    // GET endpoint for frontend to fetch transaction logs
    @GetMapping("/transaction-logs")
    public ResponseEntity<Map<String, Object>> getTransactionLogs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("dateCreated").descending());
        Page<TransactionLog> logsPage = transactionLogService.getTransactionLogs(status, search, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("total", logsPage.getTotalElements());
        response.put("page", page);
        response.put("totalPages", logsPage.getTotalPages());
        response.put("data", logsPage.getContent());

        return ResponseEntity.ok(response);
    }
}