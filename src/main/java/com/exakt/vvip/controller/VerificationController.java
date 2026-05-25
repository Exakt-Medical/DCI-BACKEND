package com.exakt.vvip.controller;

import com.exakt.vvip.dto.VehicleSearchRequest;
import com.exakt.vvip.entity.Vehicle;
import com.exakt.vvip.entity.Transaction;
import com.exakt.vvip.service.VehicleService;
import com.exakt.vvip.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Verification", description = "Vehicle lookup, verification, and certificate generation")
public class VerificationController {

    private final VehicleService vehicleService;
    private final TransactionService transactionService;

    @PostMapping("/lookup")
    @Operation(summary = "Look up vehicle from VVS database")
    public ResponseEntity<?> lookupVehicle(@RequestBody VehicleSearchRequest request) {
        Optional<Vehicle> vehicle = vehicleService.findVehicle(
                request.getMvFileNo(), request.getPlateNo(),
                request.getEngineNo(), request.getChassisNo());

        if (request.getMvFileNo() == null && request.getPlateNo() == null &&
                request.getEngineNo() == null && request.getChassisNo() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Provide at least one search criteria"));
        }

        if (vehicle.isPresent()) {
            return ResponseEntity.ok(vehicle.get());
        }
        return ResponseEntity.ok(Map.of("found", false, "message", "No records found in VVS database"));
    }

    @PostMapping("/certificate")
    @Operation(summary = "Generate certificate and record transaction")
    public ResponseEntity<?> generateCertificate(@RequestBody Map<String, Object> body, Authentication auth) {
        String authNo = "AUTH-" + java.time.Year.now().getValue() + "-" +
                String.format("%05d", System.currentTimeMillis() % 100000);

        String agentName = auth.getName();
        String company = body.getOrDefault("company", "Insurance Corp").toString();
        String assuredName = body.getOrDefault("assuredName", "").toString();
        String mvFileNumber = body.getOrDefault("mvFileNumber", "").toString();
        String plateNumber = body.getOrDefault("plateNumber", "").toString();
        String policyNumber = body.getOrDefault("policyNumber", "").toString();
        String voucherCode = body.getOrDefault("voucherCode", "").toString();
        String premiumType = body.getOrDefault("premiumType", "").toString();
        BigDecimal totalAmount = new BigDecimal(body.getOrDefault("totalAmount", "0").toString());

        Transaction tx = transactionService.createTransaction(
                agentName, company, assuredName, mvFileNumber, plateNumber,
                policyNumber, voucherCode, premiumType, totalAmount, auth.getName());

        return ResponseEntity.ok(Map.of(
                "authNo", authNo,
                "transaction", tx,
                "message", "Certificate generated and transaction recorded"
        ));
    }
}