package com.exakt.vvip.controller;

import com.exakt.vvip.dto.InsuranceFeeResponse;
import com.exakt.vvip.dto.PurchaseRequest;
import com.exakt.vvip.dto.PurchaseResponseDTO;
import com.exakt.vvip.entity.InsuranceProduct;
import com.exakt.vvip.service.InsuranceFeeService;
import com.exakt.vvip.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Vouchers", description = "Insurance product catalog, purchase, and voucher management")
public class VoucherController {

    private final VoucherService voucherService;
    private final InsuranceFeeService insuranceFeeService;

    @GetMapping("/products")
    @Operation(summary = "List all insurance products")
    public ResponseEntity<List<InsuranceProduct>> getProducts() {
        return ResponseEntity.ok(voucherService.getProducts());
    }

    @GetMapping("/insurance-fees")
    @Operation(summary = "List all insurance fee breakdowns")
    public ResponseEntity<List<InsuranceFeeResponse>> getInsuranceFees() {
        return ResponseEntity.ok(insuranceFeeService.getAllFees());
    }

    @GetMapping("/insurance-fees/{code}")
    @Operation(summary = "Get insurance fee by code")
    public ResponseEntity<InsuranceFeeResponse> getInsuranceFee(@PathVariable String code) {
        InsuranceFeeResponse fee = insuranceFeeService.getFeeByCode(code);
        if (fee == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(fee);
    }

    @PostMapping("/purchase")
    @Operation(summary = "Purchase a new insurance policy / voucher")
    public ResponseEntity<PurchaseResponseDTO> purchase(
            @RequestBody PurchaseRequest request,
            Authentication auth) {
        return ResponseEntity.ok(voucherService.purchase(request.getProductId(), auth.getName()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get purchase history for current user")
    public ResponseEntity<List<PurchaseResponseDTO>> getHistory(Authentication auth) {
        return ResponseEntity.ok(voucherService.getHistory(auth.getName()));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a voucher code")
    public ResponseEntity<PurchaseResponseDTO> validateVoucher(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(voucherService.validateVoucher(body.get("voucherCode")));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem a voucher (mark as redeemed)")
    public ResponseEntity<Map<String, String>> redeemVoucher(@RequestBody Map<String, String> body) {
        voucherService.redeemVoucher(body.get("voucherCode"));
        return ResponseEntity.ok(Map.of("message", "Voucher redeemed successfully"));
    }
}