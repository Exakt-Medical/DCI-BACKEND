package com.exakt.vvip.controller;

import com.exakt.vvip.dto.VehicleVerificationRequest;
import com.exakt.vvip.dto.VehicleVerificationResponse;
import com.exakt.vvip.dto.VvsLookupResponse;
import com.exakt.vvip.security.UserDetailsImpl;
import com.exakt.vvip.service.DciCertificateService;
import com.exakt.vvip.service.VerificationService;
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

    @PostMapping("/verify")
    @Operation(summary = "Submit vehicle identifiers for VVS verification")
    public ResponseEntity<VehicleVerificationResponse> verify(
            @Valid @RequestBody VehicleVerificationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        VehicleVerificationResponse result = verificationService.verify(request, resolveUserId(principal));
        return toResponseEntity(result);
    }

    @PostMapping("/{verificationId}/confirm")
    @Operation(summary = "Submit Final Review — calls ConfirmRequest and issues certificate")
    public ResponseEntity<VehicleVerificationResponse> confirm(
            @PathVariable Long verificationId,
            @RequestBody VehicleVerificationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        VehicleVerificationResponse result = verificationService.confirm(verificationId, request, resolveUserId(principal));
        return toResponseEntity(result);
    }

    @GetMapping("/certificate/{certNo}")
    public ResponseEntity<VehicleVerificationResponse> getByCertNo(@PathVariable String certNo) {
        return ResponseEntity.ok(verificationService.getByCertNo(certNo));
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