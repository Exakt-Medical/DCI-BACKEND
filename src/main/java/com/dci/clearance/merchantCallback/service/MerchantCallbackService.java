package com.dci.clearance.merchantCallback.service;

import com.dci.clearance.entity.Order;
import com.dci.clearance.generateVoucher.service.VoucherProcessingService;
import com.dci.clearance.generateVoucher.dto.BillerooConfirmResponse;
import com.dci.clearance.repository.OrderRepository;
import com.dci.clearance.merchantCallback.dto.BilleroConfirmResult;
import com.dci.clearance.merchantCallback.dto.MerchantCallbackResponse;
import com.dci.clearance.merchantCallback.dto.OrderUpdateResult;
import com.dci.clearance.merchantCallback.dto.PaymentSummaryResponse;
import com.dci.clearance.merchantCallback.dto.TransactionReport;
import com.dci.clearance.merchantCallback.mapper.MerchantCallbackMapper;
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
    private final VoucherProcessingService voucherProcessingService;

    /**
     * Called by GET /payment-result (browser redirect) AND GET /summary/{transactionId}.
     * Fetches the TLPE /report, updates order status in DB to PAYMENT_CONFIRMED.
     * Billeroo confirmation is handled downstream by VoucherProcessingService.
     */
    @Transactional
    public MerchantCallbackResponse verifyAndConfirm(String transactionId) {
        TransactionReport report = transactionVerificationService.fetchReport(transactionId);

        // ── Update order in DB, get the matched order back ────────────────────
        OrderUpdateResult result = updateOrderFromReport(report);
        Order order = result != null ? result.getOrder() : null;

        // ── Enrich report with order DB data (fills nulls TLPE /report omits) ─
        TransactionReport enrichedReport = enrichReportFromOrder(report, order);

        // ── If payment failed, return a failure response without proceeding ─────
        if (!report.isSuccess() || (order != null && "FAILED".equals(order.getStatus()))) {
            String failureMessage = StringUtils.hasText(report.getMessage())
                    ? report.getMessage()
                    : "Payment failed with status: " + report.getStatusCode();
            PaymentSummaryResponse failSummary = MerchantCallbackMapper.toPaymentSummary(enrichedReport, null);
            return MerchantCallbackResponse.builder()
                    .success(false)
                    .message(failureMessage)
                    .data(failSummary)
                    .build();
        }

        PaymentSummaryResponse summary = MerchantCallbackMapper.toPaymentSummary(enrichedReport, result != null ? result.getConfirmResult() : null);

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
    public OrderUpdateResult updateOrderFromReport(TransactionReport report) {
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
        } else if ("PAYMENT_VERIFYING".equals(currentStatus) && !report.isSuccess()) {
            // Payment failed (e.g. ER.00.00) — mark FAILED, skip voucher processing
            order.setStatus("FAILED");
            log.warn("Order {} → FAILED (via /report), statusCode={}, message={}",
                    order.getId(), report.getStatusCode(), report.getMessage());
        } else if ("FAILED".equals(currentStatus)) {
            log.info("Order {} already FAILED — skipping update.", order.getId());
        }

        orderRepository.save(order);

        BilleroConfirmResult confirmResult = null;

        // Auto-trigger voucher generation only for successfully confirmed orders
        if ("PAYMENT_CONFIRMED".equals(order.getStatus())) {
            if (Boolean.TRUE.equals(order.getBillerooConfirmed())) {
                confirmResult = BilleroConfirmResult.builder()
                        .success(true)
                        .voucherAlreadyProcessed(true)
                        .build();
            } else {
                try {
                    log.info("Order {} → Auto-triggering voucher processing...", order.getId());
                    BillerooConfirmResponse resp = voucherProcessingService.process(order);
                    log.info("Order {} → Voucher processing completed successfully.", order.getId());
                    
                    confirmResult = BilleroConfirmResult.builder()
                            .success(true)
                            .statusCode(resp != null ? resp.getStatus() : null)
                            .message(resp != null ? resp.getMessage() : null)
                            .build();
                } catch (Exception e) {
                    log.error("Order {} → Voucher processing FAILED: {}. Order stays PAYMENT_CONFIRMED for retry.",
                            order.getId(), e.getMessage());
                }
            }
        }

        return new OrderUpdateResult(order, confirmResult);
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