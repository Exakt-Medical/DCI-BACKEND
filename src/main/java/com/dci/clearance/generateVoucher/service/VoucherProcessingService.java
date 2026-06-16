package com.dci.clearance.generateVoucher.service;

import com.dci.clearance.entity.Order;
import com.dci.clearance.generateVoucher.client.BillerooClient;
import com.dci.clearance.generateVoucher.dto.BillerooConfirmResponse;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherProcessingService {

    private final OrderRepository orderRepository;
    private final BillerooClient billerooClient;
    private final VoucherGeneratorService voucherGenerator;
    private final VoucherCountVerificationService countVerifier;
    private final CompanyRepository companyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BillerooConfirmResponse process(Order order) {
        try {
            // Step 2: Confirm with Billeroo
            BillerooConfirmResponse billerooResp = billerooClient.confirmPayment(order);

            // Step 3: Update order
            order.setBillerooConfirmed(true);
            order.setBillerooConfirmedAt(java.time.LocalDateTime.now());
            order.setStatus("BILLEROO_CONFIRMED");
            orderRepository.save(order);

            // Step 4: Generate vouchers
            int quantity = billerooResp.getData().getVoucherCount();
            generateAndComplete(order, quantity);

            return billerooResp;
        } catch (Exception e) {
            log.error("[VOUCHER PROCESS] Failed for order {}: {}", order.getId(), e.getMessage());
            markFailedInNewTransaction(order.getId());
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPurchaseRequest(Order order, int quantity) {
        try {
            // Billeroo already confirmed via webhook
            order.setBillerooConfirmed(true);
            order.setBillerooConfirmedAt(java.time.LocalDateTime.now());
            order.setStatus("BILLEROO_CONFIRMED");
            orderRepository.save(order);

            generateAndComplete(order, quantity);
        } catch (Exception e) {
            log.error("[PURCHASE REQUEST PROCESS] Failed for order {}: {}", order.getId(), e.getMessage());
            markFailedInNewTransaction(order.getId());
            throw e;
        }
    }

    private void generateAndComplete(Order order, int quantity) {
        // Generate vouchers
        voucherGenerator.generateVouchers(order, quantity);

        // Update company count
        companyRepository.incrementAvailableVouchers(order.getCompany().getId(), quantity);

        // Verify against Billeroo
        countVerifier.verifyCount(order);

        // Complete
        order.setStatus("COMPLETED");
        orderRepository.save(order);
    }

    /**
     * Runs in a separate transaction so the FAILED status is committed
     * even when the caller's transaction rolls back due to the re-thrown exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTransaction(Long orderId) {
        orderRepository.markFailed(orderId);
    }
}
