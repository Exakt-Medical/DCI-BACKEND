package com.dci.clearance.controller;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.service.ClearanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lto")
@RequiredArgsConstructor
public class LtoController {

    private final ClearanceRequestService clearanceRequestService;

    @GetMapping("/certificate/{certificateNo}")
    public ResponseEntity<?> lookupCertificate(@PathVariable String certificateNo) {
        try {
            ClearanceRequestDto dto = clearanceRequestService.getByCertificateNo(certificateNo);
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "referenceNo", dto.getReferenceNo(),
                    "plateNumber", dto.getPlateNumber(),
                    "ownerName", dto.getOwnerName(),
                    "vehicleMake", dto.getVehicleMake(),
                    "vehicleSeries", dto.getVehicleSeries(),
                    "status", dto.getStatus(),
                    "certificateNo", dto.getCertificateNo()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("found", false, "message", "Certificate not found"));
        }
    }
}
