package com.exakt.vvip.generateVoucher.controller;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.generateVoucher.dto.VoucherProcessRequest;
import com.exakt.vvip.generateVoucher.service.VoucherProcessingService;
import com.exakt.vvip.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("generateVoucherController")
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final OrderRepository orderRepository;
    private final VoucherProcessingService voucherProcessingService;

    @PostMapping("/process")
    public ResponseEntity<?> processVouchers(@RequestBody VoucherProcessRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderId()));

        // Guard: only process PAYMENT_CONFIRMED orders
        if (!"PAYMENT_CONFIRMED".equals(order.getStatus())) {
            return ResponseEntity.badRequest()
                    .body("Order is not in PAYMENT_CONFIRMED status. Current: " + order.getStatus());
        }

        // Guard: prevent double processing
        if (Boolean.TRUE.equals(order.getBillerooConfirmed())) {
            return ResponseEntity.badRequest()
                    .body("Order already processed.");
        }

        voucherProcessingService.process(order);
        return ResponseEntity.ok("Processing started and completed successfully.");
    }
}
