package com.exakt.vvip.controller;

import com.exakt.vvip.dto.VoucherRedeemWebhookDto;
import com.exakt.vvip.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TransactionLogService transactionLogService;

    // EXACT path Billeroo expects - NO /api prefix
    @PostMapping("/webhook-to-external")
    public ResponseEntity<Map<String, Object>> handleVoucherRedeemWebhook(
            @RequestBody VoucherRedeemWebhookDto webhookData) {

        log.info("Webhook received from Billeroo: {}", webhookData);

        try {
            transactionLogService.processVoucherRedeemWebhook(webhookData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook received and processed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing webhook", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error processing webhook: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}