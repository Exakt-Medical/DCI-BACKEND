package com.dci.clearance.generateVoucher.controller;

import com.dci.clearance.dto.VoucherRedeemWebhookDto;
import com.dci.clearance.generateVoucher.dto.BillerooWebhookRequest;
import com.dci.clearance.generateVoucher.service.BillerooWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billeroo")
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

    @PostMapping("/voucher-redeem")
    public ResponseEntity<String> handleVoucherRedeem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody VoucherRedeemWebhookDto request) {

        webhookService.processVoucherRedeemWebhook(authorization, request);
        return ResponseEntity.ok("Voucher redeem notification processed successfully");
    }
}
