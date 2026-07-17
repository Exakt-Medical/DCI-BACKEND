package com.dci.clearance.controller;

import com.dci.clearance.entity.User;
import com.dci.clearance.service.AuthService;
import com.dci.clearance.service.CertificateRequestService;
import com.dci.clearance.service.EmailService;
import com.dci.clearance.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email Delivery", description = "Send certificates and transaction slips via email")
public class EmailController {

    private final AuthService authService;
    private final EmailService emailService;
    private final CertificateRequestService certificateRequestService;

    @PostMapping("/send-transaction-slip")
    @Operation(summary = "Send transaction code slip to my email", description = "Sends voucher code and transaction details to the authenticated user's email")
    public ResponseEntity<?> sendTransactionSlip(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = authService.getCurrentUser(auth.getName());
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No email address on file"));
        }

        String voucherCode = (String) body.getOrDefault("voucherCode", "");
        String certificateNo = (String) body.getOrDefault("certificateNo", null);

        String firstName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
        emailService.sendCertificateEmail(user.getEmail(), firstName, voucherCode, voucherCode, voucherCode);

        return ResponseEntity.ok(Map.of(
                "message", "Transaction slip sent to " + user.getEmail(),
                "email", user.getEmail()
        ));
    }

    @PostMapping("/send-certificate")
    @Operation(summary = "Send certificate to my email", description = "Sends DCI clearance certificate details to the authenticated user's email")
    public ResponseEntity<?> sendCertificate(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = authService.getCurrentUser(auth.getName());
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No email address on file"));
        }

        String certificateNo = (String) body.getOrDefault("certificateNo", "");
        String plateNo = (String) body.getOrDefault("plateNo", "");
        String voucherCode = (String) body.getOrDefault("voucherCode", null);
        String pdfBase64 = (String) body.getOrDefault("pdfBase64", null);

        String firstName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
        emailService.sendCertificateEmail(user.getEmail(), firstName, certificateNo, plateNo, voucherCode, pdfBase64);

        return ResponseEntity.ok(Map.of(
                "message", "Certificate sent to " + user.getEmail(),
                "email", user.getEmail()
        ));
    }
}
