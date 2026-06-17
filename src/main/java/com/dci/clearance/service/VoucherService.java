package com.dci.clearance.service;

import com.dci.clearance.dto.InsuranceFeeResponse;
import com.dci.clearance.dto.PurchaseResponseDTO;
import com.dci.clearance.entity.InsuranceFee;
import com.dci.clearance.entity.InsuranceProduct;
import com.dci.clearance.entity.Purchase;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.InsuranceFeeRepository;
import com.dci.clearance.repository.InsuranceProductRepository;
import com.dci.clearance.repository.PurchaseRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.dci.clearance.dto.VoucherValidateResponse;
import com.dci.clearance.entity.Voucher;
import com.dci.clearance.repository.VoucherRepository;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final InsuranceProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final InsuranceFeeRepository insuranceFeeRepository;
    private final VoucherRepository voucherRepository;
    private final BillerooRedeemClient billerooRedeemClient;

    public List<InsuranceFeeResponse> getAllProducts() {
        List<InsuranceProduct> products = productRepository.findByIsActiveTrue();
        return products.stream().map(p -> InsuranceFeeResponse.builder()
                .insuranceCode(p.getInsuranceCode())
                .build()).collect(Collectors.toList());
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
        return insuranceFeeRepository.findByInsuranceCode(insuranceCode).orElse(null);
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

    @Transactional(readOnly = true)
    public VoucherValidateResponse validateVoucherByCode(String voucherCode) {

        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher code not found."));

        if (!"AVAILABLE".equalsIgnoreCase(voucher.getStatus())) {
            throw new RuntimeException(
                    "Voucher is not available. Current status: " + voucher.getStatus());
        }

        if (voucher.getExpiresAt() != null
                && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Voucher has already expired.");
        }

        long remaining = voucherRepository.countByCurrentUserIdAndStatus(
                voucher.getCurrentUser().getId(), "AVAILABLE");

        return VoucherValidateResponse.builder()
                .voucherCode(voucher.getVoucherCode())
                .status(voucher.getStatus())
                .expiresAt(voucher.getExpiresAt() != null
                        ? voucher.getExpiresAt().toString() : null)
                .ownerUsername(voucher.getCurrentUser().getUsername())
                .remainingVouchers(remaining)
                .build();
    }

    @Transactional
    public void redeemVoucherByCode(String voucherCode, String certNo) {

        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherCode));

        if (!"AVAILABLE".equalsIgnoreCase(voucher.getStatus())) {
            throw new RuntimeException(
                    "Cannot redeem — voucher status is: " + voucher.getStatus());
        }

        // Call Billeroo first — transactionReference = certNo, companyCode from voucher row
        String voucherReference = billerooRedeemClient.redeem(certNo, voucher.getCompanyCode());

        // Mark redeemed in our DB regardless of Billeroo result
        voucher.setStatus("REDEEMED");
        voucher.setRedeemedAt(LocalDateTime.now());

        if (voucherReference != null) {
            voucher.setVoucherReference(voucherReference);
        } else {
            log.warn("Billeroo redeem returned no voucherReference for certNo={} voucherCode={}",
                    certNo, voucherCode);
        }

        voucherRepository.save(voucher);

        long remaining = voucherRepository.countByCurrentUserIdAndStatus(
                voucher.getCurrentUser().getId(), "AVAILABLE");

        log.info("Voucher {} redeemed — certNo={} billerooRef={} remaining={}",
                voucherCode, certNo, voucherReference, remaining);
    }

    /**
     * Looks up a voucher's database ID by its code.
     */
    public Long findIdByCode(String voucherCode) {
        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherCode));
        return voucher.getId();
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