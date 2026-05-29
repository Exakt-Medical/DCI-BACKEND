package com.exakt.vvip.merchantCallback.controller;

import com.exakt.vvip.merchantCallback.dto.MerchantCallbackResponse;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import com.exakt.vvip.merchantCallback.service.MerchantCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant-callback")
@RequiredArgsConstructor
public class MerchantCallbackController {

    private final MerchantCallbackService merchantCallbackService;

    @GetMapping("/summary/{transactionId}")
    public ResponseEntity<MerchantCallbackResponse> summary(@PathVariable String transactionId) {
        return ResponseEntity.ok(merchantCallbackService.verifyAndConfirm(transactionId));
    }

    @GetMapping("/payment-summary/{transactionId}")
    public ResponseEntity<MerchantCallbackResponse> paymentSummary(@PathVariable String transactionId) {
        return ResponseEntity.ok(merchantCallbackService.verifyAndConfirm(transactionId));
    }

    @GetMapping("/verify/{transactionId}")
    public ResponseEntity<TransactionReport> verify(@PathVariable String transactionId) {
        return ResponseEntity.ok(merchantCallbackService.verifyOnly(transactionId));
    }
}