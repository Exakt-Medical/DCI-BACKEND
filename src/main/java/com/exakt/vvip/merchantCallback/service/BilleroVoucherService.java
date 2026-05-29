package com.exakt.vvip.merchantCallback.service;

import com.exakt.vvip.merchantCallback.client.BilleroClient;
import com.exakt.vvip.merchantCallback.dto.BilleroConfirmRequest;
import com.exakt.vvip.merchantCallback.dto.BilleroConfirmResult;
import com.exakt.vvip.merchantCallback.dto.BilleroPurchaseRequest;
import com.exakt.vvip.merchantCallback.dto.BilleroPurchaseResult;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BilleroVoucherService {

    private final BilleroClient billeroClient;

    public BilleroConfirmResult confirmPayment(TransactionReport report) {
        BilleroConfirmRequest request = BilleroConfirmRequest.builder()
                .amountPaid(defaultAmount(report.getAmountPaid()))
                .voucherCount(report.getVoucherCount())
                .voucherFee(report.getVoucherFee())
                .companyCode(report.getCompanyCode())
                .merchantReference(report.getMerchantReference())
                .paymentReference(report.getPaymentReference())
                .statusCode(report.getStatusCode())
                .build();

        return billeroClient.confirmPayment(request);
    }

    public BilleroPurchaseResult createPurchaseRequest(TransactionReport report) {
        String companyName = report.getCompanyName() != null ? report.getCompanyName() : "Exakt Medical";
        
        BilleroPurchaseRequest request = BilleroPurchaseRequest.builder()
                .companyCode(report.getCompanyCode())
                .companyName(companyName)
                .contact(report.getContactMobile())
                .email(report.getEmail())
                .firstName(report.getFirstName())
                .lastName(report.getLastName())
                .reference(report.getMerchantReference())
                .totalAmount(defaultAmount(report.getAmountPaid()))
                .voucherCount(report.getVoucherCount())
                .voucherFee(report.getVoucherFee())
                .build();

        return billeroClient.createPurchaseRequest(request);
    }

    private BigDecimal defaultAmount(BigDecimal amountPaid) {
        return amountPaid == null ? BigDecimal.ZERO : amountPaid;
    }
}