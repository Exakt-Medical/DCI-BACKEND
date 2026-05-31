package com.exakt.vvip.controller;

import com.exakt.vvip.dto.OrdersRequest;
import com.exakt.vvip.dto.OrdersResponse;
import com.exakt.vvip.service.OrdersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Orders", description = "TLPE payment link generation and storage")
public class OrdersController {

    private final OrdersService ordersService;

    @PostMapping("/tlpe")
    @Operation(summary = "Create TLPE payment link and persist order")
    public ResponseEntity<?> createTlpePayment(@RequestBody OrdersRequest request,
                                               Authentication authentication) {
        try {
            OrdersResponse response = ordersService.createPaymentLink(request, authentication);
            return ResponseEntity.ok(response);
        } catch (OrdersService.PaymentException e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}