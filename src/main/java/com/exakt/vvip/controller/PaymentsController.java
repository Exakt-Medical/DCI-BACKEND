package com.exakt.vvip.controller;

import com.exakt.vvip.dto.PaymentsRequest;
import com.exakt.vvip.dto.PaymentsResponse;
import com.exakt.vvip.service.PaymentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Payments", description = "TLPE payment link generation and storage")
public class PaymentsController {   

    private final PaymentsService paymentsService;

    @PostMapping("/tlpe")
    @Operation(summary = "Create TLPE payment link")
    public ResponseEntity<?> createTlpePayment(@RequestBody PaymentsRequest request) {
        try {
            PaymentsResponse response = paymentsService.createPaymentLink(request);
            return ResponseEntity.ok(response);
        } catch (PaymentsService.PaymentException e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}