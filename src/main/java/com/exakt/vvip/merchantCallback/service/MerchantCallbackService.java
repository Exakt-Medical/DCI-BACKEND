package com.exakt.vvip.merchantCallback.service;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.repository.OrderRepository;
import com.exakt.vvip.merchantCallback.dto.MerchantCallbackResponse;
import com.exakt.vvip.merchantCallback.dto.PaymentSummaryResponse;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import com.exakt.vvip.merchantCallback.mapper.MerchantCallbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantCallbackService {

    private final TransactionVerificationService transactionVerificationService;
    private final OrderRepository orderRepository;

    /**
     * Called by GET /payment-result (browser redirect) AND GET /summary/{transactionId}.
     * Fetches the TLPE /report, updates order status in DB to PAYMENT_CONFIRMED.
     * Billeroo confirmation is handled downstream by VoucherProcessingService.
     */
    @Transactional
    public MerchantCallbackResponse verifyAndConfirm(String transactionId) {
        TransactionReport report = transactionVerificationService.fetchReport(transactionId);

        // ── Update order in DB, get the matched order back ────────────────────
        Order order = updateOrderFromReport(report);

        // ── Enrich report with order DB data (fills nulls TLPE /report omits) ─
        TransactionReport enrichedReport = enrichReportFromOrder(report, order);

        PaymentSummaryResponse summary = MerchantCallbackMapper.toPaymentSummary(enrichedReport, null);

        return MerchantCallbackResponse.builder()
                .success(true)
                .message("Payment summary generated successfully")
                .data(summary)
                .build();
    }

    public TransactionReport verifyOnly(String transactionId) {
        return transactionVerificationService.fetchReport(transactionId);
    }

    // ── Package-visible: also called by MerchantWebhookService ───────────────

    @Transactional
    public Order updateOrderFromReport(TransactionReport report) {
        String merchantRef = report.getMerchantReference();
        String txnId = report.getTransactionId();

        log.debug("TLPE /report raw: {}", report.getRawResponse());
        log.info("TLPE report → transactionId={}, merchantRef={}, statusCode={}, success={}", txnId, merchantRef, report.getStatusCode(), report.isSuccess());

        Order order = null;
        if (StringUtils.hasText(merchantRef)) {
            order = orderRepository.findByMerchantReferenceId(merchantRef).orElse(null);
        }
        if (order == null && StringUtils.hasText(txnId)) {
            order = orderRepository.findByTlpeTransactionId(txnId).orElse(null);
            if (order != null) log.info("Order found via tlpe_transaction_id={}", txnId);
        }
        if (order == null) {
            log.warn("No order found — merchantRef={}, transactionId={}", merchantRef, txnId);
            return null;
        }

        String currentStatus = order.getStatus();

        if (!StringUtils.hasText(order.getTlpeTransactionId()) && StringUtils.hasText(txnId)) {
            order.setTlpeTransactionId(txnId);
        }
        if ("PENDING_PAYMENT".equals(currentStatus)) {
            order.setStatus("PAYMENT_VERIFYING");
            currentStatus = "PAYMENT_VERIFYING";
            log.info("Order {} → PAYMENT_VERIFYING (via /report)", order.getId());
        }
        if ("PAYMENT_VERIFYING".equals(currentStatus) && report.isSuccess()) {
            BigDecimal processingFee = report.getProcessingFee() != null ? report.getProcessingFee() : BigDecimal.ZERO;
            order.setPaymentReference(report.getPaymentReference());
            order.setProcessingFee(processingFee);
            order.setTotalCharged(order.getOriginalAmount().add(processingFee));
            order.setStatus("PAYMENT_CONFIRMED");
            log.info("Order {} → PAYMENT_CONFIRMED (via /report), processingFee={}", order.getId(), processingFee);
        }

        orderRepository.save(order);
        return order;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Fills null fields in the TLPE report using the order's stored DB values.
     * Ensures downstream services always receive voucherCount, voucherFee, companyCode etc.
     */
    private TransactionReport enrichReportFromOrder(TransactionReport report, Order order) {
        if (order == null) return report;

        return TransactionReport.builder()
                .success(report.isSuccess())
                .transactionId(report.getTransactionId())
                .statusCode(report.getStatusCode())
                .message(report.getMessage())
                .rawResponse(report.getRawResponse())
                .merchantReference(firstNonBlank(report.getMerchantReference(),  order.getMerchantReferenceId()))
                .paymentReference(firstNonBlank(report.getPaymentReference(),    order.getPaymentReference()))
                .amountPaid(firstNonNull(report.getAmountPaid(),                 order.getTotalCharged()))
                .processingFee(firstNonNull(report.getProcessingFee(),           order.getProcessingFee()))
                .companyCode(firstNonBlank(report.getCompanyCode(),              order.getCompanyCode()))
                .voucherCount(report.getVoucherCount() != null ? report.getVoucherCount() : order.getVoucherCount())
                .voucherFee(firstNonNull(report.getVoucherFee(),                 order.getVoucherFee()))
                .firstName(report.getFirstName())
                .lastName(report.getLastName())
                .email(report.getEmail())
                .contactMobile(report.getContactMobile())
                .companyName(report.getCompanyName())
                .build();
    }

    private String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }
}