package com.exakt.vvip.service;

import com.exakt.vvip.dto.*;
import com.exakt.vvip.entity.*;
import com.exakt.vvip.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {


    
    private final InsuranceProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final InsuranceFeeRepository insuranceFeeRepository;

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

    private String randomAlpha(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return sb.toString();
    }
}