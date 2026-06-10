package com.dci.clearance.generateVoucher.service;

import com.dci.clearance.entity.Order;
import com.dci.clearance.dto.VoucherRedeemWebhookDto;
import com.dci.clearance.generateVoucher.dto.BillerooWebhookRequest;
import com.dci.clearance.entity.Voucher;
import com.dci.clearance.repository.OrderRepository;
import com.dci.clearance.repository.VoucherRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillerooWebhookService {

    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherProcessingService voucherProcessingService;

    @Value("${merchant-callback.billero-token}")
    private String expectedAuthToken;

    public void processWebhook(String authorizationHeader, BillerooWebhookRequest request) {
        // 1. Validate Billeroo auth token
        validateAuthKey(authorizationHeader);

        // 2. Find Order by Invoice Reference
        String invoiceRef = request.getInvoiceReference();
        Optional<Order> optionalOrder = orderRepository.findByInvoiceReference(invoiceRef);

        if (optionalOrder.isEmpty()) {
            log.error("Webhook failed: Order not found for invoice reference {}", invoiceRef);
            throw new RuntimeException("Order not found for invoice reference: " + invoiceRef);
        }

        Order order = optionalOrder.get();

        // 3. Validate amounts and company code
        if (order.getOriginalAmount().compareTo(request.getInvoiceAmount()) != 0) {
            log.error("Webhook failed: Amount mismatch. Expected {}, got {}", order.getOriginalAmount(), request.getInvoiceAmount());
            throw new RuntimeException("Amount mismatch for invoice: " + invoiceRef);
        }

        if (!order.getCompanyCode().equals(request.getCompanyCode())) {
            log.error("Webhook failed: Company code mismatch. Expected {}, got {}", order.getCompanyCode(), request.getCompanyCode());
            throw new RuntimeException("Company code mismatch for invoice: " + invoiceRef);
        }

        // 4. Skip Billeroo confirmation, trigger voucher generation directly
        if (!"COMPLETED".equals(order.getStatus()) && !Boolean.TRUE.equals(order.getBillerooConfirmed())) {
            log.info("Order {} → Auto-triggering purchase request voucher processing...", order.getId());
            voucherProcessingService.processPurchaseRequest(order, request.getVoucherCount());
            log.info("Order {} → Purchase request voucher processing completed successfully.", order.getId());
        } else {
            log.info("Order {} is already processed or completed. Ignoring webhook.", order.getId());
        }
    }

    public void processVoucherRedeemWebhook(String authorizationHeader, VoucherRedeemWebhookDto request) {
        validateAuthKey(authorizationHeader);

        if (request == null) {
            throw new RuntimeException("Missing voucher redeem payload");
        }

        if (request.getStatusCode() == null || !request.getStatusCode().startsWith("OK")) {
            throw new RuntimeException("Voucher redeem notification was not successful: " + request.getStatusCode());
        }

        String transactionReference = request.getTransactionReference();
        Voucher voucher = voucherRepository.findByVoucherCode(transactionReference)
                .orElseGet(() -> request.getVoucherReference() == null || request.getVoucherReference().isBlank()
                        ? null
                        : voucherRepository.findByVoucherReference(request.getVoucherReference()).orElse(null));

        if (voucher == null) {
            throw new RuntimeException("Voucher not found for transaction reference: " + transactionReference);
        }

        if (request.getCompanyCode() != null && !request.getCompanyCode().equals(voucher.getCompanyCode())) {
            throw new RuntimeException("Company code mismatch for voucher: " + transactionReference);
        }

        if (request.getVoucherAmount() != null
                && voucher.getOrder() != null
                && voucher.getOrder().getOriginalAmount() != null
                && voucher.getOrder().getOriginalAmount().compareTo(java.math.BigDecimal.valueOf(request.getVoucherAmount())) != 0) {
            throw new RuntimeException("Voucher amount mismatch for voucher: " + transactionReference);
        }

        if ("REDEEMED".equalsIgnoreCase(voucher.getStatus())) {
            if (request.getVoucherReference() != null && !request.getVoucherReference().isBlank()
                    && !request.getVoucherReference().equals(voucher.getVoucherReference())) {
                voucher.setVoucherReference(request.getVoucherReference());
                voucherRepository.save(voucher);
            }
            log.info("Voucher redeem notification received for already redeemed voucher transactionReference={}",
                    transactionReference);
            return;
        }

        voucher.setStatus("REDEEMED");
        voucher.setRedeemedAt(java.time.LocalDateTime.now());
        if (request.getVoucherReference() != null && !request.getVoucherReference().isBlank()) {
            voucher.setVoucherReference(request.getVoucherReference());
        }

        voucherRepository.save(voucher);

        log.info("Voucher redeem notification processed transactionReference={} voucherReference={} status={}",
                transactionReference, request.getVoucherReference(), request.getStatusCode());
    }

    private void validateAuthKey(String authHeader) {
        if (authHeader == null || !authHeader.trim().equals(expectedAuthToken.trim())) {
            log.error("Webhook auth key validation failed");
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        log.debug("Webhook Auth Key successfully validated.");
    }
}
