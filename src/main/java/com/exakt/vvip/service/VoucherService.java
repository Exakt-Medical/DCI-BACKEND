package com.exakt.vvip.service;

import com.exakt.vvip.dto.*;
import com.exakt.vvip.entity.*;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import com.exakt.vvip.merchantCallback.service.BilleroVoucherService;
import com.exakt.vvip.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {



    private final InsuranceProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final InsuranceFeeRepository insuranceFeeRepository;
    private final OrdersRepository ordersRepository;
    private final CompanyRepository companyRepository;
    private final BilleroVoucherService billeroVoucherService;

    public List<InsuranceFeeResponse> getAllProducts() {
        List<InsuranceProduct> products = productRepository.findByIsActiveTrue();
        return products.stream().map(p -> {
            InsuranceFee fee = insuranceFeeRepository.findByInsuranceCode(p.getInsuranceCode()).orElse(null);
            return InsuranceFeeResponse.builder()
                    .insuranceCode(p.getInsuranceCode())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<InsuranceProduct> getProducts() {
        return productRepository.findByIsActiveTrue();
    }

    @Transactional
    public PurchaseResponseDTO purchase(Long productId, String username) {
        InsuranceProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

//        if (Boolean.FALSE.equals(user.getAllowedToBuyVoucher())) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account does not have permission to purchase vouchers");
//        }

        String policyNumber = "CTPL-" + java.time.Year.now().getValue() + "-" +
                String.format("%06d", (int)(Math.random() * 1000000));
        String voucherCode = "VCH-" + randomAlpha(8) + "-" +
                String.valueOf(System.currentTimeMillis()).substring(9);

        Purchase purchase = Purchase.builder()
                .policyNumber(policyNumber)
                .voucherCode(voucherCode)
                .productName(product.getProductName())
                .premium(product.getPrice())
                .purchaseDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(product.getValidityDays()))
                .status(Purchase.PurchaseStatus.ACTIVE)
                .insuranceCode(product.getInsuranceCode())
                .purchasedBy(user)
                .build();

        purchase = purchaseRepository.save(purchase);

        return PurchaseResponseDTO.builder()
                .id(purchase.getId())
                .policyNumber(purchase.getPolicyNumber())
                .voucherCode(purchase.getVoucherCode())
                .productName(purchase.getProductName())
                .premium(purchase.getPremium())
                .purchaseDate(purchase.getPurchaseDate())
                .expirationDate(purchase.getExpirationDate())
                .status(purchase.getStatus().name())
                .insuranceCode(purchase.getInsuranceCode())
                .build();
    }

    public List<PurchaseResponseDTO> getHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return purchaseRepository.findByPurchasedByIdOrderByPurchaseDateDesc(user.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PurchaseResponseDTO validateVoucher(String voucherCode) {
        Purchase purchase = purchaseRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Invalid voucher code"));

        if (purchase.getStatus() != Purchase.PurchaseStatus.ACTIVE) {
            throw new RuntimeException("Voucher is not active. Status: " + purchase.getStatus());
        }

        if (purchase.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Voucher has expired");
        }

        return toDTO(purchase);
    }

    @Transactional
    public void redeemVoucher(String voucherCode) {
        Purchase purchase = purchaseRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        purchase.setStatus(Purchase.PurchaseStatus.REDEEMED);
        purchase.setRedeemedOn(LocalDateTime.now());
        purchaseRepository.save(purchase);
    }

    public InsuranceFee getInsuranceFee(String insuranceCode) {
        return insuranceFeeRepository.findByInsuranceCode(insuranceCode)
                .orElse(null);
    }

    private PurchaseResponseDTO toDTO(Purchase p) {
        return PurchaseResponseDTO.builder()
                .id(p.getId())
                .policyNumber(p.getPolicyNumber())
                .voucherCode(p.getVoucherCode())
                .productName(p.getProductName())
                .premium(p.getPremium())
                .purchaseDate(p.getPurchaseDate())
                .expirationDate(p.getExpirationDate())
                .status(p.getStatus().name())
                .redeemedOn(p.getRedeemedOn())
                .insuranceCode(p.getInsuranceCode())
                .build();
    }

    // ── Process Order (Martin.md Step 4 handoff) ──────────────────────────────

    /**
     * Called by POST /api/vouchers/process after Martin sets the order to PAYMENT_CONFIRMED.
     * Re-entrant: if a previous attempt failed after BILLEROO_CONFIRMED, we skip straight
     * to voucher generation.  Any failure sets the order to FAILED.
     */
    @Transactional
    public PurchaseResponseDTO processOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        String status = order.getStatus();

        // Guard: only accept PAYMENT_CONFIRMED or BILLEROO_CONFIRMED (re-entry after partial failure)
        if (!"PAYMENT_CONFIRMED".equals(status) && !"BILLEROO_CONFIRMED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order " + orderId + " cannot be processed in status: " + status);
        }

        // ── 1. Billeroo confirm (skip if already done) ────────────────────────
        if ("PAYMENT_CONFIRMED".equals(status)) {
            try {
                TransactionReport report = buildReportFromOrder(order);
                billeroVoucherService.createPurchaseRequest(report);
                order.setStatus("BILLEROO_CONFIRMED");
                order.setBillerooConfirmed(true);
                order.setBillerooConfirmedAt(LocalDateTime.now());
            } catch (Exception ex) {
                // Billeroo is non-fatal — log and continue to voucher generation
                // Order still advances so vouchers are generated even if Billeroo is unreachable
                org.slf4j.LoggerFactory.getLogger(VoucherService.class)
                        .warn("Billeroo createPurchaseRequest failed (non-fatal), continuing: {}", ex.getMessage());
                order.setStatus("BILLEROO_CONFIRMED");
                order.setBillerooConfirmed(false);
                order.setBillerooConfirmedAt(null);
            }
            ordersRepository.save(order);
        }

        // ── 2. Generate voucher Purchase records ──────────────────────────────
        User buyer = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found for order: " + orderId));

        String companyName = companyRepository.findById(order.getCompanyId())
                .map(Company::getCompanyName)
                .orElse("CTPL Insurance");

        List<Purchase> purchases = new ArrayList<>();
        try {
            int count = order.getVoucherCount() != null ? order.getVoucherCount() : 1;
            for (int i = 0; i < count; i++) {
                String policyNumber = "CTPL-" + java.time.Year.now().getValue() + "-"
                        + String.format("%06d", (int) (Math.random() * 1_000_000));
                String voucherCode = "VCH-" + randomAlpha(8) + "-"
                        + String.valueOf(System.currentTimeMillis()).substring(9);

                Purchase purchase = Purchase.builder()
                        .policyNumber(policyNumber)
                        .voucherCode(voucherCode)
                        .productName(companyName)
                        .premium(order.getVoucherFee() != null ? order.getVoucherFee() : BigDecimal.ZERO)
                        .purchaseDate(LocalDateTime.now())
                        .expirationDate(LocalDateTime.now().plusDays(365))
                        .status(Purchase.PurchaseStatus.ACTIVE)
                        .insuranceCode(order.getCompanyCode())
                        .purchasedBy(buyer)
                        .build();
                purchases.add(purchaseRepository.save(purchase));
            }

            order.setStatus("VOUCHERS_GENERATED");
            ordersRepository.save(order);

        } catch (Exception ex) {
            order.setStatus("FAILED");
            ordersRepository.save(order);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Voucher generation failed: " + ex.getMessage(), ex);
        }

        // ── 3. Finalize ───────────────────────────────────────────────────────
        order.setStatus("COMPLETED");
        ordersRepository.save(order);

        // Return the last generated voucher as representative response
        Purchase last = purchases.get(purchases.size() - 1);
        return toDTO(last);
    }

    private TransactionReport buildReportFromOrder(Orders order) {
        User buyer = userRepository.findById(order.getUserId()).orElse(null);
        String companyName = companyRepository.findById(order.getCompanyId())
                .map(Company::getCompanyName).orElse("CTPL Insurance");

        return TransactionReport.builder()
                .transactionId(order.getTlpeTransactionId())
                .merchantReference(order.getMerchantReferenceId())
                .paymentReference(order.getPaymentReference())
                .companyCode(order.getCompanyCode())
                .companyName(companyName)
                .voucherCount(order.getVoucherCount())
                .voucherFee(order.getVoucherFee())
                .amountPaid(order.getTotalCharged())
                .processingFee(order.getProcessingFee())
                .firstName(buyer != null ? buyer.getFirstName() : null)
                .lastName(buyer != null ? buyer.getLastName() : null)
                .email(buyer != null ? buyer.getEmail() : null)
                .contactMobile(buyer != null ? buyer.getMobile() : null)
                .success(true)
                .build();
    }

    private String randomAlpha(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return sb.toString();
    }
}