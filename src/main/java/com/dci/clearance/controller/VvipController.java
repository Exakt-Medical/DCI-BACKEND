package com.dci.clearance.controller;

import com.dci.clearance.dto.VehicleVerificationRequest;
import com.dci.clearance.dto.VehicleVerificationResponse;
import com.dci.clearance.dto.VoucherValidateResponse;
import com.dci.clearance.dto.VvsLookupResponse;
import com.dci.clearance.security.UserDetailsImpl;
import com.dci.clearance.service.DciCertificateService;
import com.dci.clearance.service.TransactionLogService;
import com.dci.clearance.service.VerificationService;
import com.dci.clearance.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vvip")
@RequiredArgsConstructor
@Tag(name = "VVIP", description = "Vehicle Verification Insurance Program")
@SecurityRequirement(name = "Bearer Authentication")
public class VvipController {

    private final VerificationService   verificationService;
    private final DciCertificateService dciCertificateService;
    private final VoucherService voucherService;
    private final TransactionLogService transactionLogService;

    @PostMapping("/verify")
    @Operation(summary = "Submit vehicle identifiers for VVS verification")
    public ResponseEntity<VehicleVerificationResponse> verify(
            @Valid @RequestBody VehicleVerificationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        VehicleVerificationResponse result = verificationService.verify(request, resolveUserId(principal));
        String username = principal != null ? principal.getUsername() : "System";
        String status = result.getVerificationStatus();
        String desc = "Vehicle verification initiated for plate: " + (request.getPlateNumber() != null ? request.getPlateNumber() : "N/A");
        String resp = "Verification status: " + status;
        transactionLogService.logTransaction(username, null, desc, resp, "WEB", "Verified".equals(status) ? "Verified" : "Failed");
        return toResponseEntity(result);
    }

    @PostMapping("/{verificationId}/confirm")
    @Operation(summary = "Submit Final Review — calls ConfirmRequest and issues certificate")
    public ResponseEntity<VehicleVerificationResponse> confirm(
            @PathVariable Long verificationId,
            @RequestBody VehicleVerificationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        VehicleVerificationResponse result = verificationService.confirm(verificationId, request, resolveUserId(principal));
        String username = principal != null ? principal.getUsername() : "System";
        String certNo = result.getCertificateNo();
        String desc = "Certificate issued: " + (certNo != null ? certNo : "N/A") + " for verification #" + verificationId;
        String resp = "Certificate: " + (certNo != null ? certNo : "N/A");
        transactionLogService.logTransaction(username, null, desc, resp, "WEB", "Authenticated");
        return toResponseEntity(result);
    }

    @GetMapping("/certificate/{certNo}")
    public ResponseEntity<VehicleVerificationResponse> getByCertNo(@PathVariable String certNo) {
        return ResponseEntity.ok(verificationService.getByCertNo(certNo));
    }

    @PostMapping("/validate-voucher")
    @Operation(summary = "Validate a voucher code before submitting final review")
    public ResponseEntity<?> validateVoucher(@RequestBody java.util.Map<String, String> body) {
        String voucherCode = body.get("voucherCode");
        if (voucherCode == null || voucherCode.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "voucherCode is required"));
        }
        try {
            VoucherValidateResponse result = voucherService.validateVoucherByCode(voucherCode.toUpperCase());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------

    private Long resolveUserId(UserDetails principal) {
        if (principal instanceof UserDetailsImpl userDetailsImpl) {
            return userDetailsImpl.getId();
        }
        return null;
    }

    private ResponseEntity<VehicleVerificationResponse> toResponseEntity(
            VehicleVerificationResponse response) {
        return switch (response.getVerificationStatus()) {
            case "VERIFIED"  -> ResponseEntity.ok(response);               // 200 — show vehicle details
            case "COMPLETED" -> ResponseEntity.ok(response);               // 200 — certificate ready
            case "FAILED"    -> ResponseEntity.unprocessableEntity()       // 422 — no VVS record found
                    .body(response);
            case "ERROR"     -> ResponseEntity.internalServerError()       // 500 — API/internal fault
                    .body(response);
            default          -> ResponseEntity.internalServerError().body(response);
        };
    }
}