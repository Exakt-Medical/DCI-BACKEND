package com.exakt.vvip.generateVoucher.service;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.generateVoucher.client.BillerooClient;
import com.exakt.vvip.generateVoucher.dto.BillerooConfirmResponse;
import com.exakt.vvip.repository.CompanyRepository;
import com.exakt.vvip.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    @Transactional
    public void process(Order order) {
        try {
            // Step 2: Confirm with Billeroo
            BillerooConfirmResponse billerooResp = billerooClient.confirmPayment(order);

            // Step 3: Update order
            orderRepository.updateBillerooConfirmed(order.getId());
            order.setStatus("BILLEROO_CONFIRMED");

            // Step 4: Generate vouchers
            int quantity = billerooResp.getData().getVoucherCount();
            voucherGenerator.generateVouchers(order, quantity);

            // Step 5: Update company count
            companyRepository.incrementAvailableVouchers(order.getCompany().getId(), quantity);

            // Step 6: Verify against Billeroo
            countVerifier.verifyCount(order);

            // Step 7: Complete
            orderRepository.markCompleted(order.getId());

        } catch (Exception e) {
            orderRepository.markFailed(order.getId());
            log.error("[VOUCHER PROCESS] Failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }
    }
}
