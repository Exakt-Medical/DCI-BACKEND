package com.exakt.vvip.merchantCallback.mapper;

import com.exakt.vvip.merchantCallback.dto.BilleroConfirmResult;
import com.exakt.vvip.merchantCallback.dto.PaymentSummaryResponse;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;

import java.math.BigDecimal;

public final class MerchantCallbackMapper {

    private MerchantCallbackMapper() {
    }

    public static PaymentSummaryResponse toPaymentSummary(TransactionReport report, BilleroConfirmResult confirmResult) {
        BigDecimal amountPaid = report.getAmountPaid() == null ? BigDecimal.ZERO : report.getAmountPaid();
        boolean alreadyProcessed = confirmResult != null && confirmResult.isVoucherAlreadyProcessed();
        String voucherStatusLabel;

        if (alreadyProcessed) {
            voucherStatusLabel = "Voucher already processed";
        } else if (confirmResult != null && confirmResult.isSuccess()) {
            voucherStatusLabel = "Voucher Generated Successfully";
        } else {
            voucherStatusLabel = "Unable to confirm voucher";
        }

        String voucherDescription = report.getVoucherDescription();
        if (voucherDescription == null || voucherDescription.isBlank()) {
            voucherDescription = confirmResult != null ? confirmResult.getMessage() : null;
        }

        return PaymentSummaryResponse.builder()
                .amountPaid(amountPaid)
                .companyCode(report.getCompanyCode())
                .merchantReference(report.getMerchantReference())
                .paymentReference(report.getPaymentReference())
                .statusCode(report.getStatusCode())
                .voucherCount(report.getVoucherCount())
                .voucherFee(report.getVoucherFee())
                .voucherStatusLabel(voucherStatusLabel)
                .voucherAlreadyProcessed(alreadyProcessed)
                .voucherDescription(voucherDescription)
                .report(report.getRawResponse())
                .confirmResponse(confirmResult == null ? null : confirmResult.getRawResponse())
                .build();
    }
}