package com.exakt.vvip.merchantCallback.service;

import com.exakt.vvip.merchantCallback.dto.BilleroConfirmResult;
import com.exakt.vvip.merchantCallback.dto.MerchantCallbackResponse;
import com.exakt.vvip.merchantCallback.dto.PaymentSummaryResponse;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import com.exakt.vvip.merchantCallback.mapper.MerchantCallbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantCallbackService {

    private final TransactionVerificationService transactionVerificationService;
    private final BilleroVoucherService billeroVoucherService;

    public MerchantCallbackResponse verifyAndConfirm(String transactionId) {
        TransactionReport report = transactionVerificationService.fetchReport(transactionId);
        BilleroConfirmResult confirmResult = billeroVoucherService.confirmPayment(report);
        PaymentSummaryResponse summary = MerchantCallbackMapper.toPaymentSummary(report, confirmResult);

        return MerchantCallbackResponse.builder()
                .success(true)
                .message("Payment summary generated successfully")
                .data(summary)
                .build();
    }

    public TransactionReport verifyOnly(String transactionId) {
        return transactionVerificationService.fetchReport(transactionId);
    }
}