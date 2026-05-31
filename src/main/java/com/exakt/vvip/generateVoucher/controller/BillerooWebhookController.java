package com.exakt.vvip.generateVoucher.controller;

import com.exakt.vvip.generateVoucher.dto.BillerooWebhookRequest;
import com.exakt.vvip.generateVoucher.service.BillerooWebhookService;
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
