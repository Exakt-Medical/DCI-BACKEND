package com.exakt.vvip.merchantCallback.controller;

import com.exakt.vvip.merchantCallback.dto.MerchantCallbackResponse;
import com.exakt.vvip.merchantCallback.service.MerchantCallbackService;
import com.exakt.vvip.merchantCallback.service.MerchantWebhookService;
import com.exakt.vvip.merchantCallback.service.MerchantWebhookStreamService;
import com.exakt.vvip.merchantCallback.util.TransactionIdValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/merchant-callback")
@RequiredArgsConstructor
public class MerchantCallbackController {

    private final MerchantCallbackService merchantCallbackService;
    private final MerchantWebhookService merchantWebhookService;
    private final MerchantWebhookStreamService streamService;

    @GetMapping(value = "/summary/{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MerchantCallbackResponse> summary(@PathVariable String transactionId) {
        return ResponseEntity.ok(merchantCallbackService.verifyAndConfirm(transactionId));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String transactionId) {
        if (!StringUtils.hasText(transactionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }
        String normalized = TransactionIdValidator.normalize(transactionId);
        return streamService.register(normalized);
    }

    @PostMapping(value = "/payment-result", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MerchantCallbackResponse> webhook(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody byte[] body) {
        return ResponseEntity.ok(merchantWebhookService.processWebhook(authorization, body));
    }
}