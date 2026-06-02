package com.dci.clearance.merchantCallback.controller;

import com.dci.clearance.merchantCallback.dto.MerchantCallbackResponse;
import com.dci.clearance.merchantCallback.service.MerchantCallbackService;
import com.dci.clearance.merchantCallback.service.MerchantWebhookService;
import com.dci.clearance.merchantCallback.service.MerchantWebhookStreamService;
import com.dci.clearance.merchantCallback.util.TransactionIdValidator;
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
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Browser redirect landing page — TLPE redirects the user here after payment (GET).
     * The callback_url in the payment request should point to this endpoint.
     * TLPE typically appends ?transactionId=xxx or ?transaction_id=xxx as a query param.
     * When a transactionId is present, this calls TLPE /report and updates the order in DB.
     */
    @GetMapping(value = "/payment-result", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> paymentRedirect(
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "transaction_id", required = false) String transactionIdAlt,
            @RequestParam(value = "merchantReferenceId", required = false) String merchantReferenceId,
            @RequestParam(value = "merchant_reference_id", required = false) String merchantReferenceIdAlt,
            @RequestParam(value = "status", required = false) String status) {

        String txnId = transactionId != null ? transactionId : transactionIdAlt;
        String refId  = merchantReferenceId != null ? merchantReferenceId : merchantReferenceIdAlt;

        Map<String, Object> response = new HashMap<>();

        // If TLPE sent a transactionId, hit /report and update order status in DB
        if (txnId != null && !txnId.isBlank()) {
            try {
                merchantCallbackService.verifyAndConfirm(txnId);
                response.put("message", "Payment verified and order updated successfully.");
            } catch (Exception ex) {
                response.put("message", "Payment redirect received but verification failed: " + ex.getMessage());
            }
        } else {
            response.put("message", "Payment redirect received. Awaiting webhook confirmation.");
        }

        if (txnId != null)  response.put("transactionId", txnId);
        if (refId != null)  response.put("merchantReferenceId", refId);
        if (status != null) response.put("status", status);

        return ResponseEntity.ok(response);
    }
}