package com.exakt.vvip.generateVoucher.service;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.entity.VoucherCountSnapshot;
import com.exakt.vvip.generateVoucher.client.BillerooClient;
import com.exakt.vvip.generateVoucher.dto.BillerooCountResponse;
import com.exakt.vvip.repository.CompanyRepository;
import com.exakt.vvip.repository.VoucherCountSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherCountVerificationService {

    private final BillerooClient billerooClient;
    private final CompanyRepository companyRepository;
    private final VoucherCountSnapshotRepository snapshotRepository;

    @Transactional
    public void verifyCount(Order order) {
        BillerooCountResponse billeroo = billerooClient.getVoucherCount(order.getCompanyCode());
        Integer localAvailableOpt = companyRepository.getAvailableVouchers(order.getCompany().getId());
        int localAvailable = localAvailableOpt != null ? localAvailableOpt : 0;

        int billerooAvailable = billeroo.getData().getAvailable();
        boolean isSynced = billerooAvailable == localAvailable;
        int discrepancy = billerooAvailable - localAvailable;

        snapshotRepository.save(VoucherCountSnapshot.builder()
            .company(order.getCompany())
            .companyCode(order.getCompanyCode())
            .order(order)
            .billerooAvailable(billerooAvailable)
            .billerooRedeemed(billeroo.getData().getRedeemed())
            .billerooCancelled(billeroo.getData().getCancelled())
            .billerooTotal(billeroo.getData().getTotal())
            .localAvailable(localAvailable)
            .isSynced(isSynced)
            .discrepancy(discrepancy)
            .triggerType("POST_GENERATION")
            .build());

        if (!isSynced) {
            log.error("[VOUCHER SYNC] Mismatch — Billeroo: {}, Local: {}", billerooAvailable, localAvailable);
            // In a real scenario, this would likely trigger an alert to operations
        }
    }
}
