package com.dci.clearance.generateVoucher.controller;

import com.dci.clearance.generateVoucher.dto.BillerooWebhookRequest;
import com.dci.clearance.generateVoucher.service.BillerooWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billeroo")
@RequiredArgsConstructor
public class BillerooWebhookController {

    private final BillerooWebhookService webhookService;

    @PostMapping("/generate-voucher")
    public ResponseEntity<String> handleGenerateVoucher(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody BillerooWebhookRequest request) {
        
        webhookService.processWebhook(authorization, request);
        return ResponseEntity.ok("Webhook processed successfully");
    }
}
